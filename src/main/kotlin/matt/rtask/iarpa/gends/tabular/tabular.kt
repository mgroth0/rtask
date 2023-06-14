package matt.rtask.iarpa.gends.tabular

import matt.briar.meta.MediaAnnotation
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.briar.orientation.OrientationBinner
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
        it.faceOrientation!!.yaw
    }
    val pitch by field {
        it.faceOrientation!!.pitch
    }
    val orientation by field {
        val y = it.faceOrientation!!.yaw
        val p = it.faceOrientation!!.pitch

        OrientationBinner.bin(yaw = y, pitch = p)
    }

    val faceBox by field {
        it.face
    }
    val bodyBox by field {
        it.body
    }

}

