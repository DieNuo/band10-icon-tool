package com.example.kernelsustyleuikit.ui.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IconCodecTest {

    private class Entry(val idx: Int, val size: Int, val format: Int, val w: Int, val h: Int)

    private fun parse(bytes: ByteArray): List<Entry> {
        fun u16(at: Int) = (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)
        fun u32(at: Int) = (bytes[at].toInt() and 0xFF) or
            ((bytes[at + 1].toInt() and 0xFF) shl 8) or
            ((bytes[at + 2].toInt() and 0xFF) shl 16) or
            ((bytes[at + 3].toInt() and 0xFF) shl 24)

        var fpk = -1
        for (i in 256..1023) {
            if (bytes[i] == 'F'.code.toByte() && bytes[i + 1] == 'P'.code.toByte() &&
                bytes[i + 2] == 'K'.code.toByte() && bytes[i + 3] == '1'.code.toByte()
            ) {
                fpk = i
                break
            }
        }
        assertTrue("FPK1 not found", fpk > 0)
        val count = u32(fpk + 4)
        var pos = fpk + 8
        val list = ArrayList<Entry>()
        for (i in 0 until count) {
            val size = u32(pos + 4)
            list += Entry(i, size, bytes[pos + 9].toInt() and 0xFF, u16(pos + 12), u16(pos + 14))
            pos = pos + 8 + size
        }
        return list
    }

    private fun samplePixels(width: Int, height: Int): IntArray {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = if ((x + y) % 7 == 0) 0 else 255
                pixels[y * width + x] = (alpha shl 24) or
                    (((x * 255 / maxOf(1, width - 1)) and 0xFF) shl 16) or
                    (((y * 255 / maxOf(1, height - 1)) and 0xFF) shl 8) or
                    ((x * y and 0xFF))
            }
        }
        return pixels
    }

    @Test
    fun encodedSizeMatchesTemplateSlot() {
        val file = File("src/main/assets/tool.face")
        assertTrue("找不到模板文件 ${file.absolutePath}", file.exists())
        val entries = parse(file.readBytes())
        assertTrue("模板条目为空", entries.isNotEmpty())
        for (entry in entries) {
            val pixels = samplePixels(entry.w, entry.h)
            val encoded = IconCodec.encode(pixels, entry.w, entry.h, entry.format)
            assertEquals(
                "条目 ${entry.idx} 长度不符 (${entry.w}x${entry.h}, cf=${entry.format})",
                entry.size,
                encoded.size,
            )
        }
    }

    @Test
    fun indexedEncodingUsesReservedTransparentIndex() {
        val width = 32
        val height = 32
        val pixels = samplePixels(width, height)
        val encoded = IconCodec.encode(pixels, width, height, IconCodec.FORMAT_INDEXED)
        assertEquals(width * height + 1036, encoded.size)
        assertEquals(25, encoded[0].toInt())
        assertEquals(10, encoded[1].toInt())
        val firstPixel = encoded[1036].toInt() and 0xFF
        // (0,0) 采样为全透明，必须映射到 0 号索引
        assertEquals(0, firstPixel)
    }

    @Test
    fun trueColorEncodingKeepsAlpha() {
        val width = 8
        val height = 8
        val pixels = samplePixels(width, height)
        val encoded = IconCodec.encode(pixels, width, height, IconCodec.FORMAT_TRUE_COLOR_ALPHA)
        assertEquals(width * height * 4 + 12, encoded.size)
        for (i in pixels.indices) {
            val base = 12 + i * 4
            val alpha = encoded[base + 3].toInt() and 0xFF
            val expected = (pixels[i] ushr 24) and 0xFF
            if (expected == 0) assertEquals(0, alpha) else assertEquals(255, alpha)
        }
    }
}
