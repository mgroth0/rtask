package matt.rtask.gends

import matt.briar.meta.FrameAnnotation
import matt.briar.meta.MediaAnnotation
import matt.briar.meta.extract.AllVidMetadata
import matt.briar.meta.extract.BoundingBox
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.briar.meta.extract.ExtractedVidMetaData
import matt.briar.meta.extract.Orientation
import matt.file.commons.rcommons.BRIAR_EXTRACT_DATA_FOLDER
import matt.file.commons.rcommons.BRIAR_EXTRACT_FOLDER
import matt.file.commons.rcommons.BRIAR_EXTRACT_METADATA_FOLDER
import matt.file.commons.rcommons.OpenMindFiles.OM2_TEMP
import matt.file.construct.mFile
import matt.json.toJsonString
import matt.lang.RUNTIME
import matt.model.email.MY_MIT_EMAIL
import matt.rtask.briar.BriarTrainingFolder
import matt.rtask.briar.BriarVideo
import matt.service.frames.MFrameGrabber
import matt.time.dur.sleep
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds


val BriarVideo.extractFolder
    get() =
        vidFile
            .relativeTo(BriarTrainingFolder.folder)
            .let { BRIAR_EXTRACT_DATA_FOLDER[it] }
            .let { it.resolveSibling(it.name.substringBefore(".")) }

fun generateDatasetJsons(frameExtractor: MFrameGrabber? = null) {

    val readmeFile = BRIAR_EXTRACT_FOLDER["README.txt"]
    val mainMetadataFile = BRIAR_EXTRACT_METADATA_FOLDER
//    val allMetadataFile = briarExtractFolder["all_metadata.json"]

    val videoMetadataFiles = forEachVideo { vid, metadata ->

        val extractedVidMetadataFile = mFile(
            vid.extractFolder.abspath + ".json"
        )


        val framesToExtract = metadata.framesToExtract().map { it.extractedFrameMetadata() }

        frameExtractor?.extractVideoFrames(
            video = vid.vidFile,
            outputFolder = vid.extractFolder,
            framesToExtract = framesToExtract.map { it.index }
        )

        val extractedMetadata = ExtractedVidMetaData(
            subject = metadata.subject.id,
            frames = framesToExtract
        )
        extractedVidMetadataFile.text = extractedMetadata.toJsonString()

        AllVidMetadata(
            file = extractedVidMetadataFile.abspath,
            metadata = extractedMetadata
        )


    }



    println("finished all ${videoMetadataFiles.size} videos")

    println("writing metadata file 1")
    mainMetadataFile.write(ExtractedMetaData(videoMetadataFiles.map { it.file }).toJsonString())
    println("writing metadata file 2")
//    println("compiling metadata")
//    val compiled = AllExtractedMetaData(videoMetadataFiles)
//    val outputStream = allMetadataFile.outputStream()
//    println("got stream. encoding to stream...")
//    Json.encodeToStream(compiled,outputStream)
//    println("closing stream")
//    outputStream.close()
//    println("closed stream")

    println("copying metadata file")
    OM2_TEMP.mkdirs()
    mainMetadataFile.copyTo(OM2_TEMP[mainMetadataFile.name], overwrite = true)

    println("writing readme")
    readmeFile.text = """
        
        BRIAR data extracted by $MY_MIT_EMAIL
        
        ${mainMetadataFile.name} contains a simple list of all metadata files, one per video. Each metadata file contains further information on that video.
        
        
    """.trimIndent()
    println("finished writing readme")
}


fun <R> forEachVideo(op: (BriarVideo, MediaAnnotation) -> R): List<R> {


    val numVideos = AtomicInteger()
    val checkedCount = AtomicInteger()
    var gotException = false

    val pool = Executors.newFixedThreadPool(
        RUNTIME.availableProcessors()
    ) {
        Thread(it).also {
            it.isDaemon = true
        }
    }

    thread(isDaemon = true) {
        while (true) {
            println("Memory: Max=${RUNTIME.maxMemory()}\tFree=${RUNTIME.freeMemory()}\tTotal=${RUNTIME.totalMemory()}")
            sleep(5.seconds)
        }
    }

    val mainThread = Thread.currentThread()


    val results = mutableListOf<R>()
    val futures = BriarTrainingFolder.subjectFolders.map { s ->
        pool.submit {
            try {
                s.field.distanceFolders.forEach { d ->
                    d.boundingConditions.forEach { b ->
                        b.videos.forEach { vid ->
                            if (gotException) return@submit
                            val n = numVideos.incrementAndGet()
                            println("processing vid $n...")
                            val metadata = vid.metadataFile.read()

                            results += op(vid, metadata)


                            val c = checkedCount.incrementAndGet()
                            println("finished $c videos")
                        }
                    }
                }
            } catch (e: Exception) {
                println("got ${e::class.simpleName} while reading xml file for subject {${s.subjectID} (message=${e.message})")
                gotException = true
                mainThread.interrupt()
                throw e
            }
        }
    }
    futures.forEach {
        it.get()
    }
    pool.shutdown()
    if (gotException) error("gotException")
    return results
}


fun MediaAnnotation.framesToExtract(): List<FrameAnnotation> {
    val frameAnnotations = detailedAnnotation.completeAnnotation.track.frameAnnotations
    return frameAnnotations.filter {
        it.numModalitiesDetected == 2
    }
}

fun FrameAnnotation.extractedFrameMetadata() = ExtractedFrameMetaData(
    index = frameIndex,
    body = bodyAnnotation.let {
        BoundingBox(
            x = it.boundingBox.boundingBoxCoordinates.leftTopCoordinates.x,
            y = it.boundingBox.boundingBoxCoordinates.leftTopCoordinates.y,
            width = it.boundingBox.boundingBoxCoordinates.width,
            height = it.boundingBox.boundingBoxCoordinates.height
        )
    },
    face = faceAnnotation.let {
        BoundingBox(
            x = it.boundingBox.boundingBoxCoordinates.leftTopCoordinates.x,
            y = it.boundingBox.boundingBoxCoordinates.leftTopCoordinates.y,
            width = it.boundingBox.boundingBoxCoordinates.width,
            height = it.boundingBox.boundingBoxCoordinates.height
        )
    },
    faceOrientation = faceAnnotation.let {
        Orientation(
            yaw = it.poseAngle!!.pose.yawAngle.angleValue,
            pitch = it.poseAngle!!.pose.pitchAngle.angleValue
        )
    }
)