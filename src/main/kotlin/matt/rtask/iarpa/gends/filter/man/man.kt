package matt.rtask.iarpa.gends.filter.man

import matt.briar.BriarExtraction
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.collect.list.slide.SlidingWindow
import matt.collect.list.slide.slide
import matt.file.construct.mFile
import matt.log.warn.warn
import matt.math.jmath.sigFigs
import matt.math.reduce.mean
import matt.math.reduce.standardDeviation
import matt.omnigui.oglang.LookAt
import matt.omnigui.oglang.onode.OImage
import matt.omnigui.oglang.onode.OText
import matt.omnigui.oglang.onode.OVBox
import matt.omnigui.rmi.lookInFxGui
import matt.prim.base64.encodeToBase64
import matt.prim.str.mybuild.lineDelimitedString
import matt.prim.str.times
import matt.rtask.iarpa.fstruct.extractFolder
import matt.rtask.iarpa.fstruct.extractMetadataFile
import matt.rtask.iarpa.gends.filter.VideoExtraction
import matt.rtask.iarpa.gends.filter.cleanorientations.fpsToWinSize

context(BriarExtraction)
fun VideoExtraction.doManualChecks(
    framesToExtract: List<ExtractedFrameMetaData>
) {
    trackMetadata.slide(fpsToWinSize(eitherMetadata.mediaInfo.videoFrameRate_fps)).forEach { slidingWindow ->
        val centerFrame = slidingWindow.centerElement
        if (centerFrame in framesToExtract) doManualCheck(slidingWindow)
    }
}

context(BriarExtraction)
fun VideoExtraction.doManualCheck(
    slidingWindow: SlidingWindow<ExtractedFrameMetaData>
) {

    val centerFrame = slidingWindow.centerElement
    if (centerFrame.faceOrientation?.confident == true) {
        val f = mFile(video.extractMetadataFile.path.substringBefore("."))["${centerFrame.index}.png"]
        if (f.exists()) {
            lookInFxGui(
                LookAt(
                    OVBox(
                        OImage(
                            f.readBytes().encodeToBase64()
                        ),

                        OText(
                            lineDelimitedString {
                                blankLine()
                                val indent = ' ' * 5
                                val win = slidingWindow.window
                                val faceOrientations = win.mapNotNull { it.faceOrientation }
                                val yaws = faceOrientations.map { it.yaw }
                                val pitches = faceOrientations.map { it.pitch }
                                +(indent + "Mean Yaw: ${
                                    yaws.mean().sigFigs(3)
                                }")
                                +(indent + "STD Yaw: ${
                                    yaws.standardDeviation().sigFigs(3)
                                }")
                                +(indent + "Mean Pitch: ${
                                    pitches.mean().sigFigs(3)
                                }")
                                +(indent + "STD Pitch: ${
                                    pitches.standardDeviation().sigFigs(3)
                                }")
                                blankLine()
                                win.forEach {
                                    val marker = if (it == centerFrame) {
                                        '>' * 4 + ' '
                                    } else {
                                        indent
                                    }
                                    val o = it.faceOrientation
                                    val info = if (o == null) {
                                        "null"
                                    } else {
                                        "Yaw=${o.yaw.sigFigs(3)}\tPitch=${o.pitch.sigFigs(3)}"
                                    }
                                    +(marker + info)
                                }
                            }

                        )
                    )
                )
            )
        } else {
            warn("!exists: ${f.relativeTo(video.extractFolder.parentFile!!)}")
        }
    }
}