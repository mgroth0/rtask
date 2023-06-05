package matt.rtask.iarpa.gends.tabular.tabulartwo

import matt.briar.meta.MediaAnnotation
import matt.rtask.tabular.tabfields.TabularClass2
import matt.time.dur.sec

class TabularVideo2(it: MediaAnnotation) : TabularClass2 {

    val camera = it.sensorInfo.id
    val cameraType = it.sensorInfo.type
    val cameraModel = it.sensorInfo.model
    val focalLength = it.sensorInfo.focalLength
    val sex = it.subject.subjectPersistentInfo.sex
    val raceLabel = it.subject.subjectPersistentInfo.race.label
    val raceAdditionalInfo = it.subject.subjectPersistentInfo.race.additionalInfo ?: ""
    val venueName = it.environment.location.name
    val activity = it.subject.subjectActivity.actvitiy
    val distance = it.sensorToSubjectInfo.sensorToSubjectDistance_meters ?: 0
    val clothingSet = it.subject.subjectImageSpecificInfo.attire.clothingSet
    val subject = it.subject.id
    val weight = it.subject.subjectPersistentInfo.weight_lbs
    val height = it.subject.subjectPersistentInfo.height_inches
    val spectrum = it.sensorInfo.captureSpectrum
    val modality = it.modality
    val bitsPerPixel = it.mediaInfo.bitsPerPixel
    val resolution = it.mediaInfo.resolution
    val colorSpace = it.mediaInfo.colorSpace
    val totalFrames = it.mediaInfo.videoNumFrames
    val fps = it.mediaInfo.videoFrameRate_fps
    val duration = it.mediaInfo.videoDuration_secs.sec
    val trackDescription = it.detailedAnnotation.completeAnnotation.description
    val cn2 = it.environment.atmosphericCondition.cn2
    val temperature = it.environment.atmosphericCondition.temperature
    val windChill = it.environment.atmosphericCondition.windChill
    val heatIndex = it.environment.atmosphericCondition.heatIndex
    val dewPoint = it.environment.atmosphericCondition.dewPoint
    val relHumidity = it.environment.atmosphericCondition.relHumidity
    val windSpeedInstant = it.environment.atmosphericCondition.windSpeedInstant
    val windDirInstant = it.environment.atmosphericCondition.windDirInstant
    val barometricPress = it.environment.atmosphericCondition.barometricPress
    val precipitation = it.environment.atmosphericCondition.precipitation
    val solarLoading = it.environment.atmosphericCondition.solarLoading

}
