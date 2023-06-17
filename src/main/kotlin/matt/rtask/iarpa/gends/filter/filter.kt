package matt.rtask.iarpa.gends.filter

import kotlinx.serialization.Serializable
import matt.async.executors.withFailableDaemonPool
import matt.async.par.parFor
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.BriarExtraction
import matt.briar.GALLERY_DIST
import matt.briar.QUERY_DIST
import matt.briar.TARGET_ORIENTATION
import matt.briar.TrialConfiguration
import matt.briar.meta.ClothingSet
import matt.briar.meta.ClothingSet.set1
import matt.briar.meta.ClothingSet.set2
import matt.briar.meta.FrameAnnotation
import matt.briar.meta.SubjectID
import matt.briar.meta.extract.BinnedOrientation
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.briar.meta.extract.ExtractedVideoMetaData
import matt.briar.meta.extract.doextract.extractedFrameMetadata
import matt.cbor.loadOrSaveCbor
import matt.file.context.ComputeContext
import matt.file.context.LocalComputeContext
import matt.json.prim.loadJson
import matt.json.toJsonString
import matt.lang.function.Op
import matt.lang.go
import matt.log.CountPrinter
import matt.model.data.message.SFile
import matt.prim.str.joinWithNewLines
import matt.rtask.iarpa.briar.BriarTrainingFolder
import matt.rtask.iarpa.briar.BriarVideo
import matt.rtask.iarpa.fstruct.extractMetadataFile
import matt.rtask.iarpa.fstruct.trackCacheFile
import matt.rtask.iarpa.gends.filter.cleanorientations.markOrientationWithNoConfidence
import matt.rtask.iarpa.gends.filter.man.doManualChecks
import matt.rtask.iarpa.gends.filter.simmat.loadSimMat
import matt.rtask.iarpa.gends.filter.stimselect.StimuliSelectionContextImpl
import matt.rtask.iarpa.gends.readme.briarExtractReadme
import matt.rtask.rinput.ExtractBriarMetadataInputs
import kotlin.random.Random

const val MANUAL_CHECKS = true
const val PRINT_EVERY = 1000

fun extractAndFilterMetadata(rArg: ExtractBriarMetadataInputs) {
    val extractionProcess = ExtractionProcess(rArg.computeContext, rArg.extraction)

    val readmeFile = rArg.extraction.briarExtractFolder["README.txt"]
    val vidSeq = BriarTrainingFolder(rArg.computeContext).videos.map {
        VideoExtraction(
            it,
            extractionProcess,
            TARGET_ORIENTATION
        )
    }
    val videoMetadataFiles = withFailableDaemonPool {


        val (vidExtractions, tracklessVidMetaDatas) = vidSeq.parMap {
            checkIfInterrupted()
            it to it.eitherMetadata
        }.unzip()


        vidExtractions.parFor {
            checkIfInterrupted()
            it.frameCandidates
        }

        val allFrameCandidates = vidExtractions.associateWith { it.frameCandidates }


        val stimSelectionContext = extractionProcess.realSimMat?.let { realSimMat ->
            StimuliSelectionContextImpl(
                simMat = realSimMat,
                videos = tracklessVidMetaDatas
            )
        }

        val missing = mutableListOf<String>()

        class ClothingAssignments() {
            private val queryClothingSets = mutableMapOf<SubjectID, ClothingSet>()
            private var temporaryQueryClothingSet: Pair<SubjectID, ClothingSet>? = null
            fun canUseSeqAsQuery(subjectID: SubjectID, set: ClothingSet): Boolean {
                return if (subjectID !in queryClothingSets) true
                else {
                    val qSet = queryClothingSets[subjectID]
                    set == qSet
                }
            }

            fun canUseSeqAsGallery(subjectID: SubjectID, set: ClothingSet): Boolean {
                temporaryQueryClothingSet?.go {
                    if (it.first == subjectID) {
                        return it.second != set
                    }
                }
                return if (subjectID !in queryClothingSets) true
                else {
                    val qSet = queryClothingSets[subjectID]
                    set != qSet
                }
            }

            fun assignQuerySet(subjectID: SubjectID, set: ClothingSet) {
                queryClothingSets[subjectID] = set
            }

            fun assignTemporaryQuerySet(subjectID: SubjectID, set: ClothingSet) {
                temporaryQueryClothingSet = subjectID to set
            }

            fun unAssignTemporaryQuerySet() {
                temporaryQueryClothingSet = null
            }

            fun assignGallerySet(subjectID: SubjectID, set: ClothingSet) {
                queryClothingSets[subjectID] = when (set) {
                    set1 -> set2
                    set2 -> set1
                }
            }

        }

        val clothingAssignments = ClothingAssignments()

        val chosenFrames = if (stimSelectionContext == null) {
            allFrameCandidates
        } else {
            val chosenFrames = allFrameCandidates.keys.associateWith { mutableListOf<ExtractedFrameMetaData>() }
            val builtTrials = stimSelectionContext.trialConfigs.mapNotNull { trial ->
                val addChosenFrameOps = mutableListOf<Op>()
                val qVids = allFrameCandidates
                    .entries
                    .filter { it.key.eitherMetadata.subjectID == trial.query }
                    .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == QUERY_DIST }
                    .filter {
                        clothingAssignments.canUseSeqAsQuery(
                            it.key.eitherMetadata.subjectID,
                            it.key.eitherMetadata.clothingSet
                        )
                    }

                val qFrames =
                    qVids.sortedBy { it.key.video.relativeVidFile.path }
                        .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                val foundQFrame = if (qFrames.isEmpty()) {
                    missing += "query frame for ${trial.query}"
                    null
                } else {
                    val qFrame = qFrames.random(Random(234))
                    addChosenFrameOps += {
                        chosenFrames[qFrame.video]!!.add(qFrame.frame)
                    }



                    clothingAssignments.assignTemporaryQuerySet(
                        qFrame.video.eitherMetadata.subjectID,
                        qFrame.video.eitherMetadata.clothingSet
                    )


                    qFrame
                }


                val qGalleryVids = allFrameCandidates
                    .entries
                    .filter { it.key.eitherMetadata.subjectID == trial.query }
                    .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == GALLERY_DIST }
                    .filter {
                        clothingAssignments.canUseSeqAsGallery(
                            it.key.eitherMetadata.subjectID,
                            it.key.eitherMetadata.clothingSet
                        )
                    }

                val queryGalleryFrames = qGalleryVids.sortedBy { it.key.video.relativeVidFile.path }
                    .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                val foundQGalleryFrame = if (queryGalleryFrames.isEmpty()) {
                    missing += "query gallery frame for ${trial.query}"
                    null
                } else {
                    val qGalleryFrame = queryGalleryFrames.random(Random(3463))
                    addChosenFrameOps += {
                        chosenFrames[qGalleryFrame.video]!!.add(qGalleryFrame.frame)
                    }

                    qGalleryFrame
                }

                clothingAssignments.unAssignTemporaryQuerySet()

                val foundDFrames = trial.distractors.mapIndexed { index, d ->
                    val dVids = allFrameCandidates
                        .entries
                        .filter { it.key.eitherMetadata.subjectID == d }
                        .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == GALLERY_DIST }
                        .filter {
                            clothingAssignments.canUseSeqAsGallery(
                                it.key.eitherMetadata.subjectID,
                                it.key.eitherMetadata.clothingSet
                            )
                        }

                    val dFrames = dVids.sortedBy { it.key.video.relativeVidFile.path }
                        .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                        .filter { it.frame !in chosenFrames[it.video]!! }
                    if (dFrames.isEmpty()) {
                        missing += "distractor frame for $d"
                        null
                    } else {
                        val dFrame = dFrames.random(Random(976975 + index))
                        addChosenFrameOps += {
                            chosenFrames[dFrame.video]!!.add(dFrame.frame)
                        }
                        dFrame
                    }
                }

                if (foundQFrame != null) {
                    if (foundQGalleryFrame != null) {
                        val notNullDFrames = foundDFrames.filterNotNull()
                        if (notNullDFrames.size == 4) {
                            addChosenFrameOps.forEach { it() }
                            clothingAssignments.assignQuerySet(
                                foundQFrame.video.eitherMetadata.subjectID,
                                foundQFrame.video.eitherMetadata.clothingSet
                            )
                            clothingAssignments.assignGallerySet(
                                foundQGalleryFrame.video.eitherMetadata.subjectID,
                                foundQGalleryFrame.video.eitherMetadata.clothingSet
                            )
                            notNullDFrames.forEach {
                                clothingAssignments.assignGallerySet(
                                    it.video.eitherMetadata.subjectID,
                                    it.video.eitherMetadata.clothingSet
                                )
                            }
                            BuiltTrial(
                                trial,
                                queryFrame = foundQFrame,
                                queryGalleryFrame = foundQGalleryFrame,
                                notNullDFrames
                            )
                        } else null
                    } else null

                } else {
                    null
                }

            }

            println("Got ${builtTrials.size} fully built trials out of a possible ${stimSelectionContext.trialConfigs.size}")

            if (missing.isNotEmpty()) {
                println("missing frames:\n\n${missing.joinWithNewLines { "\t$it" }}\n\n")
            }

            extractionProcess.extractMetadataFile.resolveSibling("trials.json")
                .write(builtTrials.map { it.toSTrial() }.toJsonString())
            chosenFrames
        }


        vidExtractions.parMap { vidExtraction ->
            checkIfInterrupted()
            vidExtraction.extract(chosenFrames[vidExtraction])
        }
    }
    extractionProcess.extractMetadataFile.write(ExtractedMetaData(videoMetadataFiles).toJsonString())



    readmeFile.text = briarExtractReadme()
}

class FoundFrame(val video: VideoExtraction, val frame: ExtractedFrameMetaData) {
    fun path() = SFile(video.video.relativeVidFile.path.substringBefore(".") + "/" + frame.index + ".png")
}

class BuiltTrial(
    val configuration: TrialConfiguration,
    val queryFrame: FoundFrame,
    val queryGalleryFrame: FoundFrame,
    val distractors: List<FoundFrame>
) {
    init {
        require(distractors.size == 4)
    }

    fun toSTrial() = STrial(
        configuration = configuration,
        query = queryFrame.path(),
        queryGallery = queryGalleryFrame.path(),
        distractors = distractors.map { it.path() }
    )

}

@Serializable
class STrial(
    val configuration: TrialConfiguration,
    val query: SFile,
    val queryGallery: SFile,
    val distractors: List<SFile>
)


class ExtractionProcess(
    val computeContext: ComputeContext,
    val extraction: BriarExtraction
) {

    val realSimMat by lazy {
        (computeContext as? LocalComputeContext)?.run { loadSimMat() }
    }
    val extractMetadataFile = extraction.briarExtractMetadataFile
    val extractedMetadata by lazy {
        extractMetadataFile.takeIf { it.exists() }?.loadJson<ExtractedMetaData>()?.videos?.associate {
            it.vidFile to it.metadata
        }
    }
    val extractedMetasCounter = CountPrinter(printEvery = PRINT_EVERY) { "finished metadata vid $it..." }
    val extractedFramesCounter = CountPrinter(printEvery = PRINT_EVERY) { "finished frames vid $it..." }
    val extractedVidsCounter = CountPrinter(printEvery = PRINT_EVERY) { "finished extracting vid $it..." }
}

class VideoExtraction(
    val video: BriarVideo,
    val extractionProcess: ExtractionProcess,
    val targetAngle: BinnedOrientation
) {
    private val quickMetadata by lazy {
        extractionProcess.extractedMetadata?.get(video.relativeVidFile.path)
    }

    private val fullMetadata by lazy {
        video.metadataFile.read()
    }

    val trackMetadata by lazy {
        with(extractionProcess.extraction) {
            val extractedFrames = video.trackCacheFile.loadOrSaveCbor<List<ExtractedFrameMetaData>> {
                val originalFrames = fullMetadata.detailedAnnotation.completeAnnotation.track.frameAnnotations
                originalFrames.map(FrameAnnotation::extractedFrameMetadata)
            }
            val itr = extractedFrames.iterator()
            var n = itr.next()
            val numFrames = eitherMetadata.mediaInfo.videoNumFrames
            val filledFrames = List(numFrames) { i ->
                if (n.index == i) n.also {
                    if (i < numFrames - 1) {
                        if (itr.hasNext()) {
                            n = itr.next()
                        }
                    }
                }
                else ExtractedFrameMetaData(index = i, crop = null, face = null, body = null, faceOrientation = null)
            }
            filledFrames.markOrientationWithNoConfidence(
                fps = eitherMetadata.mediaInfo.videoFrameRate_fps,
                targetAngle = targetAngle
            )
            extractionProcess.extractedFramesCounter.click()
            filledFrames
        }
    }

    val eitherMetadata by lazy {
        (quickMetadata ?: fullMetadata).also {
            extractionProcess.extractedMetasCounter.click()
        }
    }

    private val shouldInclude by lazy {
        extractionProcess.extraction.shouldInclude(eitherMetadata, similarityMatrix = extractionProcess.realSimMat)
    }

    val frameCandidates by lazy {
        if (shouldInclude) {
            extractionProcess.extraction.framesToExtract(
                eitherMetadata,
                trackMetadata,
            )
        } else null
    }

    fun extract(
        framesToExtract: List<ExtractedFrameMetaData>?
    ): ExtractedVideoMetaData = with(extractionProcess.extraction) {

        if (MANUAL_CHECKS && !framesToExtract.isNullOrEmpty()) doManualChecks(framesToExtract)

        val extractedMetadata = ExtractedVideoMetaData(
            vidFile = video.relativeVidFile.path,
            framesMetaDataFile = video.extractMetadataFile.takeIf { !framesToExtract.isNullOrEmpty() }?.abspath,
            metadata = eitherMetadata.withoutTrack()
        )
        framesToExtract?.takeIf { it.isNotEmpty() }?.go {
            val framesMetaData = ExtractedFramesMetaData(framesToExtract)
            video.extractMetadataFile.text = framesMetaData.toJsonString()
        }
        extractionProcess.extractedVidsCounter.click()

        extractedMetadata
    }


}