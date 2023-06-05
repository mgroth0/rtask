package matt.rtask.iarpa.gends

import matt.async.executors.withFailableDaemonPool
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.meta.FrameAnnotation
import matt.briar.meta.MediaAnnotation
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.briar.meta.extract.ExtractedVideoMetaData
import matt.briar.meta.extract.doextract.extractedFrameMetadata
import matt.briar.meta.extract.filter.framesToExtract
import matt.briar.meta.extract.filter.include
import matt.collect.itr.filterNotNull
import matt.file.context.ComputeContext
import matt.file.ext.FileExtension
import matt.json.toJsonString
import matt.log.CountPrinter
import matt.rtask.iarpa.briar.BriarTrainingFolder
import matt.rtask.iarpa.briar.BriarVideo
import matt.rtask.iarpa.fstruct.extractFolder
import matt.rtask.iarpa.gends.readme.briarExtractReadme
import matt.rtask.rinput.ExtractBriarMetadataInputs

fun generateDatasetJsons(rArg: ExtractBriarMetadataInputs) = with(rArg.computeContext) {
    val extractedVidsCounter = CountPrinter(printEvery = 100) { "finished extracting vid $it..." }
    val fileContext = rArg.computeContext.files
    val readmeFile = fileContext.briarExtractFolder["README.txt"]
    val videoMetadataFiles = withFailableDaemonPool {
        BriarTrainingFolder(rArg.computeContext).videos.parMap { vid ->
            checkIfInterrupted()
            val metadata = vid.metadataFile.read()
            extract(vid, metadata).also {
                extractedVidsCounter.click()
            }
        }
    }
    fileContext.briarExtractMetadataFile.write(ExtractedMetaData(videoMetadataFiles.filterNotNull()).toJsonString())
    readmeFile.text = briarExtractReadme()
}

context(ComputeContext)
fun extract(
    vid: BriarVideo,
    metadata: MediaAnnotation,
): ExtractedVideoMetaData? {
    val extractedVidMetadataFile = vid.extractFolder.resRepExt(FileExtension.JSON)
    val framesToExtract = if (metadata.include())
        metadata.framesToExtract().map(FrameAnnotation::extractedFrameMetadata) else listOf()
    if (framesToExtract.isEmpty()) return null
    /*if (framesToExtract.size > DEFAULT_MAX_STREAMS) {
        error("I cannot get ffmpeg's max_streams option to work yet so stay below 1000 streams. I don't have time to do make too many images right now anyway.")
    }*/
    val extractedMetadata = ExtractedVideoMetaData(
        framesMetaDataFile = extractedVidMetadataFile.abspath,
        metadata = metadata.copy(
            detailedAnnotation = metadata.detailedAnnotation.copy(
                completeAnnotation = metadata.detailedAnnotation.completeAnnotation.copy(
                    track = metadata.detailedAnnotation.completeAnnotation.track.copy(frameAnnotations = listOf())
                )
            )
        )
    )
    val framesMetaData = ExtractedFramesMetaData(framesToExtract)
    extractedVidMetadataFile.text = framesMetaData.toJsonString()
    return extractedMetadata
}



