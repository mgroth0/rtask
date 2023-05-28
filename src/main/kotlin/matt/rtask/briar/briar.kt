package matt.rtask.briar

import matt.briar.meta.MediaAnnotation
import matt.briar.meta.SubjectID
import kotlinx.serialization.decodeFromString
import matt.file.MFile
import matt.file.commons.rcommons.OM_LOCAL_DATA_FOLDER
import nl.adaptivity.xmlutil.serialization.XML


object BriarTrainingFolder {
    val folder = OM_LOCAL_DATA_FOLDER["BRS1"]
    val subjectFolders by lazy {
        folder.listFilesAsList()!!.map {
            BriarSubjectFolder(it)
        }
    }
}

class BriarSubjectFolder(private val folder: MFile) {
    val controlled = BriarControlledData(folder["controlled"])
    val field = BriarFieldData(folder["field"])
    val subjectID get() = SubjectID(folder.name)
}

class BriarControlledData(private val folder: MFile)

enum class BriarDistances {
    `100m`, `200m`, `400m`, `500m`, close_range, uav
}

class BriarFieldData(private val folder: MFile) {
    val distanceFolders = BriarDistances.values().map {
        folder[it.name]
    }.filter {
        it.exists()
    }.map {
        BriarDistanceData(it)
    }
}

enum class BoundingCondition {
    wb, face
}

class BriarDistanceData(private val folder: MFile) {
    val boundingConditions = BoundingCondition.values().map {
        folder[it.name]
    }.filter {
        it.exists()
    }.map {
        BriarWholeBodyOrFaceData(it)
    }
}

class BriarWholeBodyOrFaceData(private val folder: MFile) {
    val videos by lazy {
        val children = folder.listFilesAsList() ?: error("could not get children of $folder")
        children.filter { it.mExtension.afterDot == "mp4" }.map {
            BriarVideo(it)
        }
    }
}

private const val DETECTIONS_FILE_SUFFIX = "_WB_face_detections.xml"

class BriarVideo(val vidFile: MFile) {
    val metadataFile by lazy {
        val tracksFile = vidFile.resolveSibling(vidFile.name.substringBefore(".") + "_WB_face_tracks.xml")
        val detectionsFile = vidFile.resolveSibling(vidFile.name.substringBefore(".") + DETECTIONS_FILE_SUFFIX)
        BriarMetadataFile(listOf(tracksFile, detectionsFile).single { it.exists() })
    }
}

class BriarMetadataFile(val file: MFile) {
    companion object {
        val BRIAR_XML = XML {

        }
    }

    fun read() = BRIAR_XML.decodeFromString<MediaAnnotation>(file.text)

    val isDetections = file.endsWith(DETECTIONS_FILE_SUFFIX)
}

