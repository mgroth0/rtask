package matt.rtask.iarpa.gends.filter.cleanorientations

import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.collect.list.slide.slide
import matt.math.reduce.mean
import kotlin.math.abs
import kotlin.math.floor

const val RATIO_ANNOTATED_THRESHOLD = 0.5
const val DIST_FROM_MEAN_ANGLE_THRESHOLD = 5

fun List<ExtractedFrameMetaData>.markOrientationWithNoConfidence(
    fps: Int
) {


    val winSize = if (fps % 2 == 0) {
        fps + 1
    } else fps

    val centerI = floor(winSize / 2.0).toInt()


    val countThreshold = winSize * RATIO_ANNOTATED_THRESHOLD

    slide(winSize).forEach {
        val centerFrame = it.centerElement
        val win = it.window
        val orientationAnnotationCount = win.count { it.faceOrientation != null }
        val centerFrameOrientation = centerFrame.faceOrientation
        if (centerFrameOrientation != null) {
            if (orientationAnnotationCount < countThreshold) {
                centerFrameOrientation.confident = false
            } else {
                val meanYaw = win.mapNotNull { it.faceOrientation?.yaw }.mean()
                val yawDev = abs(centerFrameOrientation.yaw - meanYaw)
                if (yawDev >= DIST_FROM_MEAN_ANGLE_THRESHOLD) {
                    centerFrameOrientation.confident = false
                } else {
                    val meanPitch = win.mapNotNull { it.faceOrientation?.pitch }.mean()
                    val pitchDev = abs(centerFrameOrientation.pitch - meanPitch)
                    if (pitchDev >= DIST_FROM_MEAN_ANGLE_THRESHOLD) {
                        centerFrameOrientation.confident = false
                    }
                }
            }
        }
    }

    subList(0, centerI).forEach {
        it.faceOrientation?.confident = false
    }

    asReversed().subList(0, centerI).forEach {
        it.faceOrientation?.confident = false
    }

}