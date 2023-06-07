package matt.rtask.iarpa.gends.crop.png

import matt.model.data.byte.ByteSize
import matt.model.data.byte.bytes
import kotlin.math.roundToInt

class PngSizePredictor(private val bytesPerPixel: Int = 3) {
    companion object {
        private val HEADER_GUESS = 33.bytes
        private const val COMPRESSION_FACTOR_GUESS = 0.75
    }

    fun predictSize(width: Int, height: Int): ByteSize {
        return (width * height * bytesPerPixel * COMPRESSION_FACTOR_GUESS).roundToInt().bytes + HEADER_GUESS
    }
}