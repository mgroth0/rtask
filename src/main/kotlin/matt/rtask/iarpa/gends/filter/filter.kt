package matt.rtask.iarpa.gends.filter

import kotlinx.serialization.Serializable
import matt.async.executors.withFailableDaemonPool
import matt.async.par.parFor
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.BriarExtraction
import matt.briar.GALLERY_DIST
import matt.briar.QUERY_DIST
import matt.briar.TrialConfiguration
import matt.briar.meta.ClothingSet.set1
import matt.briar.meta.ClothingSet.set2
import matt.briar.meta.FrameAnnotation
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
import matt.lang.go
import matt.log.CountPrinter
import matt.model.data.message.SFile
import matt.prim.str.joinWithNewLines
import matt.rtask.iarpa.briar.BriarTrainingFolder
import matt.rtask.iarpa.briar.BriarVideo
import matt.rtask.iarpa.fstruct.extractMetadataFile
import matt.rtask.iarpa.fstruct.trackCacheFile
import matt.rtask.iarpa.gends.filter.cleanorientations.markOrientationWithNoConfidence
import matt.rtask.iarpa.gends.filter.simmat.loadSimMat
import matt.rtask.iarpa.gends.filter.stimselect.StimuliSelectionContextImpl
import matt.rtask.iarpa.gends.readme.briarExtractReadme
import matt.rtask.rinput.ExtractBriarMetadataInputs
import kotlin.random.Random

fun extractAndFilterMetadata(rArg: ExtractBriarMetadataInputs) {
    val extractionProcess = ExtractionProcess(rArg.computeContext, rArg.extraction)

    val readmeFile = rArg.extraction.briarExtractFolder["README.txt"]
    val vidSeq = BriarTrainingFolder(rArg.computeContext).videosSeq.map { VideoExtraction(it, extractionProcess) }
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

        val chosenFrames = if (stimSelectionContext == null) {
            allFrameCandidates
        } else {
            val chosenFrames = allFrameCandidates.keys.associateWith { mutableListOf<ExtractedFrameMetaData>() }
            val builtTrials = stimSelectionContext.trialConfigs.mapNotNull { trial ->
                val qVids = allFrameCandidates
                    .entries
                    .filter { it.key.eitherMetadata.subject.id == trial.query }
                    .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == QUERY_DIST }
                    .filter { it.key.eitherMetadata.subject.subjectImageSpecificInfo.attire.clothingSet == set1 }

                val qFrames =
                    qVids.sortedBy { it.key.video.relativeVidFile.path }
                        .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                val foundQFrame = if (qFrames.isEmpty()) {
                    missing += "query frame for ${trial.query}"
                    null
                } else {
                    val qFrame = qFrames.random(Random(234))
                    chosenFrames[qFrame.video]!!.add(qFrame.frame)
                    qFrame
                }


                val qGalleryVids = allFrameCandidates
                    .entries
                    .filter { it.key.eitherMetadata.subject.id == trial.query }
                    .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == GALLERY_DIST }
                    .filter { it.key.eitherMetadata.subject.subjectImageSpecificInfo.attire.clothingSet == set2 }

                val queryGalleryFrames = qGalleryVids.sortedBy { it.key.video.relativeVidFile.path }
                    .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                val foundQGalleryFrame = if (queryGalleryFrames.isEmpty()) {
                    missing += "query gallery frame for ${trial.query}"
                    null
                } else {
                    val qGalleryFrame = queryGalleryFrames.random(Random(3463))
                    chosenFrames[qGalleryFrame.video]!!.add(qGalleryFrame.frame)
                    qGalleryFrame
                }


                val foundDFrames = trial.distractors.mapIndexed { index, d ->
                    val dVids = allFrameCandidates
                        .entries
                        .filter { it.key.eitherMetadata.subject.id == d }
                        .filter { it.key.eitherMetadata.sensorToSubjectInfo.sensorToSubjectDistance_meters == GALLERY_DIST }
                        .filter { it.key.eitherMetadata.subject.subjectImageSpecificInfo.attire.clothingSet == set2 }

                    val dFrames = dVids.sortedBy { it.key.video.relativeVidFile.path }
                        .flatMap { e -> e.value?.map { FoundFrame(e.key, it) } ?: listOf() }
                        .filter { it.frame !in chosenFrames[it.video]!! }
                    if (dFrames.isEmpty()) {
                        missing += "distractor frame for $d"
                        null
                    } else {
                        val dFrame = dFrames.random(Random(976975 + index))
                        chosenFrames[dFrame.video]!!.add(dFrame.frame)
                        dFrame
                    }
                }

                if (foundQFrame != null) {
                    if (foundQGalleryFrame != null) {
                        val notNullDFrames = foundDFrames.filterNotNull()
                        if (notNullDFrames.size == 4) {
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
    val extractedMetasCounter = CountPrinter(printEvery = 100) { "finished metadata vid $it..." }
    val extractedVidsCounter = CountPrinter(printEvery = 100) { "finished extracting vid $it..." }
}

class VideoExtraction(
    val video: BriarVideo,
    val extractionProcess: ExtractionProcess
) {
    val quickMetadata by lazy {
        extractionProcess.extractedMetadata?.get(video.relativeVidFile.path)
    }

    val fullMetadata by lazy {
        video.metadataFile.read()
    }

    val trackMetadata by lazy {
        with(extractionProcess.extraction) {
            video.trackCacheFile.loadOrSaveCbor<List<ExtractedFrameMetaData>> {
                val originalFrames = fullMetadata.detailedAnnotation.completeAnnotation.track.frameAnnotations
                val extractedFrames = originalFrames.map(FrameAnnotation::extractedFrameMetadata)
                extractedFrames.markOrientationWithNoConfidence(
                    fps = eitherMetadata.mediaInfo.videoFrameRate_fps
                )
                extractedFrames
            }
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