package matt.rtask.iarpa.gends.crop.crop

import matt.briar.meta.extract.Box
import matt.briar.meta.extract.ExtractedFrameMetaData
import matt.ffmpeg.filtergraph.crop.CropBox
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun ExtractedFrameMetaData.generateCrop(
    totalWidth: Int,
    totalHeight: Int
): Box {
    val faceXEnd = face.x + face.width
    val faceYEnd = face.y + face.height
    val bodyXEnd = body.x + body.width
    val bodyYEnd = body.y + body.height
    var x = min(face.x, body.x)
    var y = min(face.y, body.y)
    var w = maxOf(faceXEnd, bodyXEnd) - x
    var h = maxOf(faceYEnd, bodyYEnd) - y
    val extraW = round(w * .25).toInt()
    val extraH = round(h * .25).toInt()
    x = max(0, (x - extraW))
    /*    if (x == 0) {
            println("face.x=${face.x}")
            println("face.y=${face.y}")
            println("body.x=${body.x}")
            println("body.y=${body.y}")
            println("face.width=${face.width}")
            println("face.height=${face.height}")
            println("body.width=${body.width}")
            println("body.height=${body.height}")
            println("faceYEnd=${faceYEnd}")
            println("faceXEnd=${faceXEnd}")
            println("faceYEnd=${faceYEnd}")
            println("bodyXEnd=${bodyXEnd}")
            println("bodyYEnd=${bodyXEnd}")
            println("extraW=${extraW}")
            println("extraH=${extraH}")
            println("min(face.x, body.x)=${min(face.x, body.x)}")
        }*/
    y = max(0, (y - extraH))
    w = min(totalWidth, (w + extraW + extraW))
    h = min(totalHeight, (h + extraH + extraH))
    return Box(
        x = x,
        y = y,
        width = w,
        height = h
    )
}


fun Box.toCropBox() = CropBox(
    x = x,
    y = y,
    width = width,
    height = height
)