package matt.rtask.iarpa.gends.filter.cleanorientations

import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.collect.list.slide.SlidingWindow
import matt.collect.list.slide.slide
import matt.math.reduce.mean
import matt.math.reduce.standardDeviation
import matt.model.data.orientation.BinnedOrientation
import kotlin.math.abs
import kotlin.math.floor

const val RATIO_ANNOTATED_THRESHOLD = 0.25
const val DIST_FROM_MEAN_ANGLE_THRESHOLD = 10
const val DIST_FROM_ADJACENT_ANGLE_THRESHOLD = 10
const val STANDARD_DEV_YAW_THRESHOLD = 5.0
const val STANDARD_DEV_PITCH_THRESHOLD = 10.0
const val MAX_ANGLE_DEV_THRESHOLD = 30
const val MEAN_THRESHOLD = 10

fun fpsToWinSize(fps: Int): Int {

    val winSize = if (fps % 2 == 0) {
        fps + 1
    } else fps
    return winSize
}

fun List<ExtractedFrameMetaData>.markOrientationWithNoConfidence(
    fps: Int,
    targetAngle: BinnedOrientation
) {


    val winSize = fpsToWinSize(fps)

    val centerI = floor(winSize / 2.0).toInt()


    val countThreshold = winSize * RATIO_ANNOTATED_THRESHOLD

    slide(winSize).forEach {
        val centerFrame = it.centerElement
        centerFrame.faceOrientation?.confident =
            it.confidentOfOrientation(
                centerI = centerI,
                countThreshold = countThreshold,
                targetAngle = targetAngle
            )
    }

    subList(0, centerI).forEach {
        it.faceOrientation?.confident = false
    }

    asReversed().subList(0, centerI).forEach {
        it.faceOrientation?.confident = false
    }

}


@Suppress("RedundantIf")
fun SlidingWindow<ExtractedFrameMetaData>.confidentOfOrientation(
    centerI: Int,
    countThreshold: Double,
    targetAngle: BinnedOrientation
): Boolean {
    val centerFrame = centerElement
    val centerFrameOrientation = centerFrame.faceOrientation!!
    val centerYaw = centerFrameOrientation.yaw
    val centerPitch = centerFrameOrientation.pitch
    val nextFrame = window[centerI + 1]
    val previousFrame = window[centerI - 1]
    val nextOrientation = nextFrame.faceOrientation ?: return false
    val previousOrientation = previousFrame.faceOrientation ?: return false
    if (abs(nextOrientation.yaw - centerYaw) >= DIST_FROM_ADJACENT_ANGLE_THRESHOLD) return false
    if (abs(previousOrientation.yaw - centerYaw) >= DIST_FROM_ADJACENT_ANGLE_THRESHOLD) return false
    if (abs(nextOrientation.pitch - centerPitch) >= DIST_FROM_ADJACENT_ANGLE_THRESHOLD) return false
    if (abs(previousOrientation.pitch - centerPitch) >= DIST_FROM_ADJACENT_ANGLE_THRESHOLD) return false
    val orientationAnnotationCount = window.count { it.faceOrientation != null }
    if (orientationAnnotationCount < countThreshold) return false
    if (!confidentOfAxial(
            centerYaw,
            targetAngle = targetAngle.yawBin.angle.toDouble(),
            standardDevThreshold = STANDARD_DEV_YAW_THRESHOLD,
            window.mapNotNull { it.faceOrientation?.yaw })
    ) return false
    if (!confidentOfAxial(
            centerPitch,
            targetAngle = targetAngle.pitchBin.angle.toDouble(),
            standardDevThreshold = STANDARD_DEV_PITCH_THRESHOLD,
            window.mapNotNull { it.faceOrientation?.pitch })
    ) return false
    return true
}

@Suppress("RedundantIf")
private fun confidentOfAxial(
    center: Double,
    targetAngle: Double,
    standardDevThreshold: Double,
    angles: List<Double>
): Boolean {
    val max = angles.max()
    val min = angles.min()
    if (abs(max - targetAngle) > MAX_ANGLE_DEV_THRESHOLD) return false
    if (abs(min - targetAngle) > MAX_ANGLE_DEV_THRESHOLD) return false
    val mean = angles.mean()
    if (abs(mean - targetAngle) > MEAN_THRESHOLD) return false
    val dev = abs(center - mean)
    if (dev >= DIST_FROM_MEAN_ANGLE_THRESHOLD) return false
    val standardDev = angles.standardDeviation()
    if (standardDev >= standardDevThreshold) return false
    return true
}