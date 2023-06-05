package matt.rtask.iarpa.gends.tabular

import matt.briar.meta.MediaAnnotation
import matt.briar.meta.extract.BinnedOrientation
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.briar.meta.extract.PitchBin
import matt.briar.meta.extract.YawBin
import matt.rtask.tabular.tabfields.TabularClass
import matt.time.dur.sec

object TabularVideo : TabularClass<MediaAnnotation>() {

    val camera by field {
        it.sensorInfo.id
    }
    val cameraModel by field {
        it.sensorInfo.model
    }
    val sex by field {
        it.subject.subjectPersistentInfo.sex
    }
    val raceLabel by field {
        it.subject.subjectPersistentInfo.race.label
    }
    val raceAdditionalInfo by field {
        it.subject.subjectPersistentInfo.race.additionalInfo
    }
    val venueName by field {
        it.environment.location.name
    }
    val activity by field {
        it.subject.subjectActivity.actvitiy
    }
    val distance by field {
        it.sensorToSubjectInfo.sensorToSubjectDistance_meters
    }
    val clothingSet by field {
        it.subject.subjectImageSpecificInfo.attire.clothingSet
    }
    val subject by field {
        it.subject.id
    }
    val weight by field {
        it.subject.subjectPersistentInfo.weight_lbs
    }
    val height by field {
        it.subject.subjectPersistentInfo.height_inches
    }
    val spectrum by field {
        it.sensorInfo.captureSpectrum
    }
    val modality by field {
        it.modality
    }
    val bitsPerPixel by field {
        it.mediaInfo.bitsPerPixel
    }
    val resolution by field {
        it.mediaInfo.resolution
    }
    val colorSpace by field {
        it.mediaInfo.colorSpace
    }
    val totalFrames by field {
        it.mediaInfo.videoNumFrames
    }
    val fps by field {
        it.mediaInfo.videoFrameRate_fps
    }
    val duration by field {
        it.mediaInfo.videoDuration_secs.sec
    }
    val trackDescription by field {
        it.detailedAnnotation.completeAnnotation.description
    }
    val cn2 by field {
        it.environment.atmosphericCondition.cn2
    }
    val temperature by field {
        it.environment.atmosphericCondition.temperature
    }
    val windChill by field {
        it.environment.atmosphericCondition.windChill
    }
    val heatIndex by field {
        it.environment.atmosphericCondition.heatIndex
    }
    val dewPoint by field {
        it.environment.atmosphericCondition.dewPoint
    }
    val relHumidity by field {
        it.environment.atmosphericCondition.relHumidity
    }
    val windSpeedInstant by field {
        it.environment.atmosphericCondition.windSpeedInstant
    }
    val windDirInstant by field {
        it.environment.atmosphericCondition.windDirInstant
    }
    val barometricPress by field {
        it.environment.atmosphericCondition.barometricPress
    }
    val precipitation by field {
        it.environment.atmosphericCondition.precipitation
    }
    val solarLoading by field {
        it.environment.atmosphericCondition.solarLoading
    }
}

object TabularFrame : TabularClass<ExtractedFrameMetaData>() {
    val yaw by field {
        it.faceOrientation.yaw
    }
    val pitch by field {
        it.faceOrientation.pitch
    }
    val orientation by field {
        val y = it.faceOrientation.yaw
        val p = it.faceOrientation.pitch

        OrientationBinner.bin(yaw = y, pitch = p)
    }

    val faceBox by field {
        it.face
    }
    val bodyBox by field {
        it.body
    }

}


object OrientationBinner {
    const val YAW_RADIUS = 2.0
    const val PITCH_RADIUS = 2.0
    private const val YAW_MIDDLE_ANGLE = 0.0
    private const val YAW_SIDE_ANGLE = 45.0
    private const val YAW_VERY_SIDE_ANGLE = 90.0
    private const val PITCH_MIDDLE_ANGLE = 0.0
    private const val PITCH_UP_OR_DOWN_ANGLE = 30.0
    private val yawMiddle = (-YAW_RADIUS - YAW_MIDDLE_ANGLE)..(YAW_RADIUS - YAW_MIDDLE_ANGLE)
    private val yawLeft = (-YAW_RADIUS - YAW_SIDE_ANGLE)..(YAW_RADIUS - YAW_SIDE_ANGLE)
    private val yawRight = (-YAW_RADIUS + YAW_SIDE_ANGLE)..(YAW_RADIUS + YAW_SIDE_ANGLE)
    private val yawVeryLeft = (-YAW_RADIUS - YAW_VERY_SIDE_ANGLE)..(YAW_RADIUS - YAW_VERY_SIDE_ANGLE)
    private val yawVeryRight = (-YAW_RADIUS + YAW_VERY_SIDE_ANGLE)..(YAW_RADIUS + YAW_VERY_SIDE_ANGLE)
    private val pitchMiddle = (-PITCH_RADIUS - PITCH_MIDDLE_ANGLE)..(PITCH_RADIUS - PITCH_MIDDLE_ANGLE)
    private val pitchUp = (-PITCH_RADIUS + PITCH_UP_OR_DOWN_ANGLE)..(PITCH_RADIUS + PITCH_UP_OR_DOWN_ANGLE)
    private val pitchDown = (-PITCH_RADIUS - PITCH_UP_OR_DOWN_ANGLE)..(PITCH_RADIUS - PITCH_UP_OR_DOWN_ANGLE)

    fun bin(yaw: Double, pitch: Double): BinnedOrientation? {
        val pitchPart = when (pitch) {
            in pitchMiddle -> PitchBin(0)
            in pitchUp     -> PitchBin(PITCH_UP_OR_DOWN_ANGLE.toInt())
            in pitchDown   -> PitchBin(-PITCH_UP_OR_DOWN_ANGLE.toInt())
            else           -> return null
        }
        val yawPart = when (yaw) {
            in yawMiddle    -> YawBin(0)
            in yawLeft      -> YawBin(-YAW_SIDE_ANGLE.toInt())
            in yawVeryLeft  -> YawBin(-YAW_VERY_SIDE_ANGLE.toInt())
            in yawRight     -> YawBin(YAW_SIDE_ANGLE.toInt())
            in yawVeryRight -> YawBin(YAW_VERY_SIDE_ANGLE.toInt())
            else            -> return null
        }
        return BinnedOrientation(yawPart, pitchPart)
    }
}

