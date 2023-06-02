package matt.rtask.iarpa.gends

import matt.async.executors.withFailableDaemonPool
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.meta.MediaAnnotation
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.briar.meta.extract.ExtractedVideoMetaData
import matt.briar.meta.extract.doextract.extractedFrameMetadata
import matt.briar.meta.extract.doextract.framesToExtract
import matt.briar.meta.extract.doextract.toExtractedSubject
import matt.file.commons.rcommons.BRIAR_EXTRACT_FOLDER
import matt.file.commons.rcommons.BRIAR_EXTRACT_METADATA_FILE
import matt.file.construct.mFile
import matt.json.toJsonString
import matt.log.CountPrinter
import matt.rtask.iarpa.briar.BriarTrainingFolder
import matt.rtask.iarpa.briar.BriarVideo
import matt.rtask.iarpa.fstruct.extractFolder
import matt.rtask.iarpa.gends.readme.briarExtractReadme
import matt.rtask.profile.openMindProfile
import matt.service.frames.MFrameGrabber


fun generateDatasetJsons() = openMindProfile {
    val extractedVidsCounter = CountPrinter { "finished processing vid $it..." }
    val readmeFile = BRIAR_EXTRACT_FOLDER["README.txt"]
    val videoMetadataFiles = withFailableDaemonPool {
        BriarTrainingFolder.videos.parMap { vid ->
            checkIfInterrupted()
            val metadata = vid.metadataFile.read()
            extract(vid, metadata).also {
                extractedVidsCounter.click()
            }
        }
    }
    BRIAR_EXTRACT_METADATA_FILE.write(ExtractedMetaData(videoMetadataFiles).toJsonString())
    readmeFile.text = briarExtractReadme()
}


fun extract(
    vid: BriarVideo,
    metadata: MediaAnnotation,
    frameExtractor: MFrameGrabber? = null
): ExtractedVideoMetaData {
    val extractedVidMetadataFile = mFile(
        vid.extractFolder.abspath + ".json"
    )
    val framesToExtract = metadata.framesToExtract().map { it.extractedFrameMetadata() }
    frameExtractor?.extractVideoFrames(
        video = vid.vidFile,
        outputFolder = vid.extractFolder,
        framesToExtract = framesToExtract.map { it.index }
    )
    val extractedMetadata = ExtractedVideoMetaData(
        framesMetaDataFile = extractedVidMetadataFile.abspath,
        subject = metadata.subject.toExtractedSubject(),
    )
    val framesMetaData = ExtractedFramesMetaData(
        frames = framesToExtract
    )
    extractedVidMetadataFile.text = framesMetaData.toJsonString()
    return extractedMetadata
}



