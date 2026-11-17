package com.example.kernelsustyleuikit.ui.tool

import android.graphics.Bitmap

/**
 * 精简版 TGA 解码器：只覆盖手环图标常见的 8/15/16/24/32 位变体，
 * 用于给用户挑进来的 TGA 生成预览。
 */
object TgaDecoder {

    fun looksLike(source: ByteArray?): Boolean {
        if (source == null || source.size < 18) return false
        val colorMapType = source[1].toInt() and 0xFF
        val imageType = source[2].toInt() and 0xFF
        val width = u16(source, 12)
        val height = u16(source, 14)
        val depth = source[16].toInt() and 0xFF
        if (width <= 0 || height <= 0 || width > 2048 || height > 2048) return false
        if (imageType != 1 && imageType != 2 && imageType != 3 &&
            imageType != 9 && imageType != 10 && imageType != 11
        ) {
            return false
        }
        if (colorMapType != 0 && colorMapType != 1) return false
        return depth == 8 || depth == 15 || depth == 16 || depth == 24 || depth == 32
    }

    fun decode(source: ByteArray): Bitmap {
        val idLength = source[0].toInt() and 0xFF
        val colorMapType = source[1].toInt() and 0xFF
        val imageType = source[2].toInt() and 0xFF
        val colorMapFirst = u16(source, 3)
        val colorMapLength = u16(source, 5)
        val colorMapDepth = source[7].toInt() and 0xFF
        val width = u16(source, 12)
        val height = u16(source, 14)
        val depth = source[16].toInt() and 0xFF
        val descriptor = source[17].toInt() and 0xFF

        var pos = idLength + 18
        var palette: IntArray? = null
        if (colorMapType == 1 && colorMapLength > 0) {
            val bytesPerEntry = maxOf(1, colorMapDepth / 8)
            palette = IntArray(colorMapFirst + colorMapLength)
            var index = 0
            while (index < colorMapLength && pos + bytesPerEntry <= source.size) {
                palette[colorMapFirst + index] = readPixel(source, pos, colorMapDepth, null)
                pos += bytesPerEntry
                index++
            }
        }

        val count = width * height
        val pixels = IntArray(count)
        val bytesPerPixel = maxOf(1, (depth + 7) / 8)
        val rle = imageType >= 9
        val indexed = imageType == 1 || imageType == 9

        if (!rle) {
            var i = 0
            while (i < count) {
                val next = pos + bytesPerPixel
                if (next > source.size) break
                pixels[i] = readPixel(source, pos, depth, if (indexed) palette else null)
                pos = next
                i++
            }
        } else {
            var i = 0
            while (i < count && pos < source.size) {
                val packet = source[pos].toInt()
                pos++
                val run = (packet and 0x7F) + 1
                if (packet and 0x80 != 0) {
                    if (pos + bytesPerPixel > source.size) break
                    val value = readPixel(source, pos, depth, if (indexed) palette else null)
                    pos += bytesPerPixel
                    var k = 0
                    while (k < run && i < count) {
                        pixels[i++] = value
                        k++
                    }
                } else {
                    var k = 0
                    while (k < run && i < count) {
                        if (pos + bytesPerPixel > source.size) break
                        pixels[i++] = readPixel(source, pos, depth, if (indexed) palette else null)
                        pos += bytesPerPixel
                        k++
                    }
                }
            }
        }

        if (descriptor and 0x20 == 0) {
            for (row in 0 until height / 2) {
                val top = row * width
                val bottom = (height - 1 - row) * width
                for (col in 0 until width) {
                    val tmp = pixels[top + col]
                    pixels[top + col] = pixels[bottom + col]
                    pixels[bottom + col] = tmp
                }
            }
        }
        if (descriptor and 0x10 != 0) {
            for (row in 0 until height) {
                val base = row * width
                for (col in 0 until width / 2) {
                    val tmp = pixels[base + col]
                    pixels[base + col] = pixels[base + width - 1 - col]
                    pixels[base + width - 1 - col] = tmp
                }
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun readPixel(source: ByteArray, at: Int, depth: Int, palette: IntArray?): Int {
        if (depth == 8) {
            val index = source[at].toInt() and 0xFF
            return if (palette == null || index >= palette.size) {
                (index shl 16) or (0xFF shl 24) or (index shl 8) or index
            } else {
                palette[index]
            }
        }
        var blue = source[at].toInt() and 0xFF
        var green = if (depth == 15 || depth == 16) 0 else source[at + 1].toInt() and 0xFF
        var red = if (depth == 15 || depth == 16) 0 else source[at + 2].toInt() and 0xFF
        var alpha = 255
        if (depth == 15 || depth == 16) {
            val value = blue or ((source[at + 1].toInt() and 0xFF) shl 8)
            red = ((((value shr 10) and 31) * 255) + 15) / 31
            green = ((((value shr 5) and 31) * 255) + 15) / 31
            blue = (((value and 31) * 255) + 15) / 31
            if (depth == 16 && (value and 0x8000) == 0) alpha = 0
        } else if (depth == 32) {
            alpha = source[at + 3].toInt() and 0xFF
        }
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun u16(source: ByteArray, at: Int): Int =
        (source[at].toInt() and 0xFF) or ((source[at + 1].toInt() and 0xFF) shl 8)
}
