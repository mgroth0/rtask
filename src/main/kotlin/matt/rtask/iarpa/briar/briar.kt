package matt.rtask.iarpa.briar

import kotlinx.serialization.decodeFromString
import matt.briar.meta.MediaAnnotation
import matt.briar.meta.SubjectID
import matt.file.MFile
import matt.file.context.ComputeContext
import matt.json.YesIUseJson
import nl.adaptivity.xmlutil.serialization.XML


class BriarTrainingFolder(context: ComputeContext) {
    val folder = context.files.brs1Folder
    val subjectFolders by lazy {
        folder.listFilesAsList()!!.map {
            BriarSubjectFolder(this, it)
        }
    }
    val fieldDistanceFolders get() = subjectFolders.asSequence().flatMap { it.field.distanceFolders }
    val boundingConditionsFolders get() = fieldDistanceFolders.flatMap { it.boundingConditions }
    val videos get() = boundingConditionsFolders.flatMap { it.videos }
}

class BriarSubjectFolder(
    trainingFolder: BriarTrainingFolder,
    private val folder: MFile
) {
    val controlled = BriarControlledData(folder["controlled"])
    val field = BriarFieldData(trainingFolder, folder["field"])
    val subjectID get() = SubjectID(folder.name)
}

class BriarControlledData(private val folder: MFile)

enum class BriarDistances {
    `100m`, `200m`, `400m`, `500m`, close_range, uav
}

class BriarFieldData(
    trainingFolder: BriarTrainingFolder,
    private val folder: MFile
) {
    val distanceFolders = BriarDistances.values().map {
        folder[it.name]
    }.filter {
        it.exists()
    }.map {
        BriarDistanceData(trainingFolder, it)
    }
}

enum class BoundingCondition {
    wb, face
}

class BriarDistanceData(
    trainingFolder: BriarTrainingFolder,
    private val folder: MFile
) {
    val boundingConditions = BoundingCondition.values().map {
        folder[it.name]
    }.filter {
        it.exists()
    }.map {
        BriarWholeBodyOrFaceData(trainingFolder, it)
    }
}

class BriarWholeBodyOrFaceData(
    trainingFolder: BriarTrainingFolder,
    private val folder: MFile
) {
    val videos by lazy {
        val children = folder.listFilesAsList() ?: error("could not get children of $folder")
        children.filter { it.mExtension.afterDot == "mp4" }.map {
            BriarVideo(trainingFolder, it)
        }
    }
}

private const val DETECTIONS_FILE_SUFFIX = "_WB_face_detections.xml"

class BriarVideo(
    trainingFolder: BriarTrainingFolder,
    private val vidFile: MFile
) {
    val relativeVidFile by lazy {
        vidFile.relativeTo(trainingFolder.folder)
    }
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


val yes = YesIUseJson
