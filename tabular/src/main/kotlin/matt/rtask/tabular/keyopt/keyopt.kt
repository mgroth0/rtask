package matt.rtask.tabular.keyopt

import matt.lang.anno.SupportedByChatGPT

val ZERO_UNTIL_16 = 0 until 16

@SupportedByChatGPT
fun interleaveUnsignedShortBitsTypeSafely(a: Short, b: Short): Int {
    val aUnsigned = a.toInt() and 0xFFFF
    val bUnsigned = b.toInt() and 0xFFFF

    var result = 0
    for (i in ZERO_UNTIL_16) {
        val aBit = (aUnsigned shr i) and 1
        val bBit = (bUnsigned shr i) and 1

        result = result or (aBit shl (2 * i))
        result = result or (bBit shl (2 * i + 1))
    }

    return result
}


fun interleaveUnsignedShortBitsPerformatively(a: Int, b: Int): Int {
    val aUnsigned = a and 0xFFFF
    val bUnsigned = b and 0xFFFF

    var result = 0
    for (i in ZERO_UNTIL_16) {
        val aBit = (aUnsigned shr i) and 1
        val bBit = (bUnsigned shr i) and 1

        result = result or (aBit shl (2 * i))
        result = result or (bBit shl (2 * i + 1))
    }

    return result
}


fun concatUnsignedShortsPerformatively(a: Int, b: Int) = a shl 16 and b