package com.example.kernelsustyleuikit.ui.tool

/**
 * 手环图标编码器。
 *
 * 输出格式沿用固件里的“1036 字节头 + 像素”布局：
 * 前 12 字节为 TGA 头（含 25 字节扩展区标记），紧接着是 256 * 4 字节调色板，
 * 之后才是 8 位索引数据或 32 位真彩数据。
 */
object IconCodec {

    const val FORMAT_INDEXED = 10
    const val FORMAT_TRUE_COLOR_ALPHA = 16

    private const val HEADER_SIZE = 1036
    private const val PALETTE_SLOTS = 256

    fun encode(pixels: IntArray, width: Int, height: Int, colorFormat: Int): ByteArray =
        when (colorFormat) {
            FORMAT_INDEXED -> encodeIndexed(pixels, width, height)
            FORMAT_TRUE_COLOR_ALPHA -> encodeTrueColorAlpha(pixels, width, height)
            else -> throw IllegalArgumentException("不支持的图标格式: $colorFormat")
        }

    /** 把用户提供的 8 位索引 TGA 规范化成模板使用的布局；不匹配时返回 null。 */
    fun encodeTga(source: ByteArray, width: Int, height: Int): ByteArray? {
        if (source.size < 18) return null
        val idLength = source[0].toInt() and 0xFF
        val colorMapType = source[1].toInt() and 0xFF
        val imageType = source[2].toInt() and 0xFF
        val colorMapFirst = u16(source, 3)
        val colorMapLength = u16(source, 5)
        val colorMapDepth = source[7].toInt() and 0xFF
        val imageWidth = u16(source, 12)
        val imageHeight = u16(source, 14)
        val depth = source[16].toInt() and 0xFF
        val descriptor = source[17].toInt() and 0xFF

        if (colorMapType != 1 || (imageType != 1 && imageType != 9)) return null
        if (depth != 8 || colorMapDepth != 32 || colorMapLength < 1) return null
        if (imageWidth != width || imageHeight != height) return null

        var pos = idLength + 18
        val palette = IntArray(colorMapFirst + colorMapLength)
        for (i in 0 until colorMapLength) {
            if (pos + 4 > source.size) return null
            palette[colorMapFirst + i] =
                ((source[pos + 3].toInt() and 0xFF) shl 24) or
                    ((source[pos + 2].toInt() and 0xFF) shl 16) or
                    ((source[pos + 1].toInt() and 0xFF) shl 8) or
                    (source[pos].toInt() and 0xFF)
            pos += 4
        }

        val count = width * height
        val indices = ByteArray(count)
        if (imageType == 1) {
            if (pos + count > source.size) return null
            System.arraycopy(source, pos, indices, 0, count)
        } else {
            var out = 0
            while (out < count && pos < source.size) {
                val packet = source[pos].toInt()
                pos++
                val run = (packet and 0x7F) + 1
                if (packet and 0x80 == 0) {
                    var k = 0
                    while (k < run && out < count) {
                        if (pos >= source.size) return null
                        indices[out++] = source[pos++]
                        k++
                    }
                } else {
                    if (pos >= source.size) return null
                    val value = source[pos]
                    pos++
                    var k = 0
                    while (k < run && out < count) {
                        indices[out++] = value
                        k++
                    }
                }
            }
        }

        if (descriptor and 0x20 == 0) {
            for (row in 0 until height / 2) {
                for (col in 0 until width) {
                    val top = row * width + col
                    val bottom = (height - 1 - row) * width + col
                    val tmp = indices[top]
                    indices[top] = indices[bottom]
                    indices[bottom] = tmp
                }
            }
        }
        if (descriptor and 0x10 != 0) {
            for (row in 0 until height) {
                val base = row * width
                for (col in 0 until width / 2) {
                    val left = base + col
                    val right = base + width - 1 - col
                    val tmp = indices[left]
                    indices[left] = indices[right]
                    indices[right] = tmp
                }
            }
        }

        var transparent = -1
        for (i in palette.indices) {
            if ((palette[i] ushr 24) and 0xFF == 0) {
                transparent = i
                break
            }
        }
        if (transparent < 0) return null
        if (transparent != 0) {
            val swap = palette[0]
            palette[0] = palette[transparent]
            palette[transparent] = swap
            for (i in 0 until count) {
                val value = indices[i].toInt() and 0xFF
                indices[i] = when (value) {
                    0 -> transparent.toByte()
                    transparent -> 0
                    else -> value.toByte()
                }
            }
        }

        val out = ByteArray(count + HEADER_SIZE)
        out[0] = 25
        out[1] = 10
        put16(out, 4, width)
        put16(out, 6, height)
        put16(out, 8, width)
        for (slot in 0 until PALETTE_SLOTS) {
            val color = if (slot < palette.size) palette[slot] else 0
            val base = 12 + slot * 4
            out[base] = (color and 0xFF).toByte()
            out[base + 1] = ((color shr 8) and 0xFF).toByte()
            out[base + 2] = ((color shr 16) and 0xFF).toByte()
            out[base + 3] = ((color ushr 24) and 0xFF).toByte()
        }
        System.arraycopy(indices, 0, out, HEADER_SIZE, count)
        return out
    }

    private fun encodeTrueColorAlpha(pixels: IntArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(width * height * 4 + 12)
        header(FORMAT_TRUE_COLOR_ALPHA, width, height, width shl 2).copyInto(out, 0)
        var pos = 12
        for (color in pixels) {
            out[pos] = (color and 0xFF).toByte()
            out[pos + 1] = ((color shr 8) and 0xFF).toByte()
            out[pos + 2] = ((color shr 16) and 0xFF).toByte()
            out[pos + 3] = ((color ushr 24) and 0xFF).toByte()
            pos += 4
        }
        return out
    }

    private fun encodeIndexed(pixels: IntArray, width: Int, height: Int): ByteArray {
        val histogram = HashMap<Int, Int>()
        for (color in pixels) {
            if ((color ushr 24) and 0xFF >= 8) {
                histogram[color] = (histogram[color] ?: 0) + 1
            }
        }
        val samples = ArrayList<IntArray>(histogram.size)
        for ((color, weight) in histogram) samples.add(intArrayOf(color, weight))

        val palette = medianCut(samples, 255)
        val out = ByteArray(width * height + HEADER_SIZE)
        header(FORMAT_INDEXED, width, height, width).copyInto(out, 0)
        for (i in palette.indices) {
            val color = palette[i]
            val base = 12 + (i + 1) * 4
            out[base] = (color and 0xFF).toByte()
            out[base + 1] = ((color shr 8) and 0xFF).toByte()
            out[base + 2] = ((color shr 16) and 0xFF).toByte()
            out[base + 3] = ((color ushr 24) and 0xFF).toByte()
        }

        val cache = HashMap<Int, Int>()
        for (i in pixels.indices) {
            val color = pixels[i]
            if ((color ushr 24) and 0xFF < 8) {
                out[HEADER_SIZE + i] = 0
            } else {
                val index = cache.getOrPut(color) { nearest(palette, color) + 1 }
                out[HEADER_SIZE + i] = index.toByte()
            }
        }
        return out
    }

    private fun header(type: Int, width: Int, height: Int, bitsPerPixel: Int): ByteArray {
        val out = ByteArray(12)
        out[0] = 25
        out[1] = type.toByte()
        put16(out, 4, width)
        put16(out, 6, height)
        put16(out, 8, bitsPerPixel)
        put16(out, 10, 0)
        return out
    }

    private fun put16(target: ByteArray, at: Int, value: Int) {
        target[at] = (value and 0xFF).toByte()
        target[at + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun u16(source: ByteArray, at: Int): Int =
        (source[at].toInt() and 0xFF) or ((source[at + 1].toInt() and 0xFF) shl 8)

    /** 按 alpha 加权的最近邻匹配，alpha 权重为其它通道的 4 倍。 */
    private fun nearest(palette: IntArray, color: Int): Int {
        val alpha = (color ushr 24) and 0xFF
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        var best = 0
        var bestDistance = Long.MAX_VALUE
        for (i in palette.indices) {
            val candidate = palette[i]
            val da = (alpha - ((candidate ushr 24) and 0xFF)).toLong()
            val dr = (red - ((candidate shr 16) and 0xFF)).toLong()
            val dg = (green - ((candidate shr 8) and 0xFF)).toLong()
            val db = (blue - (candidate and 0xFF)).toLong()
            val distance = da * da * 4 + dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                best = i
                bestDistance = distance
            }
        }
        return best
    }

    /** 中位切分量化：按“跨度最大且像素最多”的盒子优先切分，最多产出 limit 个颜色。 */
    private fun medianCut(samples: List<IntArray>, limit: Int): IntArray {
        if (samples.isEmpty()) return IntArray(0)
        if (samples.size <= limit) {
            return IntArray(samples.size) { samples[it][0] }
        }

        val buckets = ArrayList<MutableList<IntArray>>()
        buckets.add(ArrayList(samples))

        while (buckets.size < limit) {
            var target = -1
            var bestScore = -1L
            var bestChannel = 0
            for (i in buckets.indices) {
                val bucket = buckets[i]
                if (bucket.size < 2) continue
                val range = range(bucket)
                var weight = 0L
                for (entry in bucket) weight += entry[1]
                val dr = range[1] - range[0]
                val dg = range[3] - range[2]
                val db = range[5] - range[4]
                val da = (range[7] - range[6]) shl 1
                val span = maxOf(maxOf(dr, dg), maxOf(db, da))
                val score = span.toLong() * weight
                if (score > bestScore) {
                    bestScore = score
                    target = i
                    bestChannel = when (span) {
                        dr -> 16
                        dg -> 8
                        db -> 0
                        else -> 24
                    }
                }
            }
            if (target < 0) break

            val bucket = buckets.removeAt(target)
            bucket.sortBy { (it[0] shr bestChannel) and 0xFF }

            var total = 0L
            for (entry in bucket) total += entry[1].toLong()
            var acc = 0L
            var split = 1
            for (i in 0 until bucket.size - 1) {
                acc += bucket[i][1]
                split = i + 1
                if (acc * 2 >= total) break
            }
            buckets.add(ArrayList(bucket.subList(0, split)))
            buckets.add(ArrayList(bucket.subList(split, bucket.size)))
        }

        return IntArray(buckets.size) { index ->
            var alpha = 0L
            var red = 0L
            var green = 0L
            var blue = 0L
            var weight = 0L
            for (entry in buckets[index]) {
                val color = entry[0]
                val w = entry[1].toLong()
                alpha += ((color ushr 24) and 0xFF) * w
                red += ((color shr 16) and 0xFF) * w
                green += ((color shr 8) and 0xFF) * w
                blue += (color and 0xFF) * w
                weight += w
            }
            if (weight == 0L) weight = 1
            (((alpha / weight) and 0xFF).toInt() shl 24) or
                (((red / weight) and 0xFF).toInt() shl 16) or
                (((green / weight) and 0xFF).toInt() shl 8) or
                ((blue / weight) and 0xFF).toInt()
        }
    }

    private fun range(samples: List<IntArray>): IntArray {
        var minR = 255; var maxR = 0
        var minG = 255; var maxG = 0
        var minB = 255; var maxB = 0
        var minA = 255; var maxA = 0
        for (entry in samples) {
            val color = entry[0]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val a = (color ushr 24) and 0xFF
            if (r < minR) minR = r
            if (r > maxR) maxR = r
            if (g < minG) minG = g
            if (g > maxG) maxG = g
            if (b < minB) minB = b
            if (b > maxB) maxB = b
            if (a < minA) minA = a
            if (a > maxA) maxA = a
        }
        return intArrayOf(minR, maxR, minG, maxG, minB, maxB, minA, maxA)
    }
}
