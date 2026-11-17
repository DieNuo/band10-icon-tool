package com.example.kernelsustyleuikit.ui.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * 把用户挑的图片写回 .face 容器。已选项目才替换，未选项目保留原图。
 */
object FaceGenerator {

    private const val MAX_FILE_SIZE = 64 * 1024 * 1024
    private const val HEAD_SIZE = 4096

    /** 官方卡片圆角比例：11px / 196px 宽（从模板原版卡片量得）。 */
    private const val CARD_CORNER_RATIO = 11f / 196f

    fun generate(
        context: Context,
        template: FaceTemplate,
        picks: Map<Int, Uri>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Int {
        if (picks.isEmpty()) throw IllegalStateException("先选至少一张图片")

        val output = template.data.copyOf()
        val total = picks.size
        var done = 0
        for (entry in template.entries) {
            val uri = picks[entry.idx] ?: continue
            val encoded = encodeEntry(context, entry, uri)
            if (encoded.size != entry.size) {
                throw IllegalStateException(
                    "${entry.label} 长度不匹配 ${encoded.size}/${entry.size}"
                )
            }
            System.arraycopy(encoded, 0, output, entry.dataOff, encoded.size)
            done++
            onProgress(done, total)
        }
        return done
    }

    fun writeTo(context: Context, target: Uri, template: FaceTemplate, picks: Map<Int, Uri>): Int {
        val output = template.data.copyOf()
        var done = 0
        for (entry in template.entries) {
            val uri = picks[entry.idx] ?: continue
            val encoded = encodeEntry(context, entry, uri)
            if (encoded.size != entry.size) {
                throw IllegalStateException("${entry.label} 长度不匹配 ${encoded.size}/${entry.size}")
            }
            System.arraycopy(encoded, 0, output, entry.dataOff, encoded.size)
            done++
        }
        val stream = context.contentResolver.openOutputStream(target)
            ?: throw IOException("无法写入目标文件")
        stream.use { it.write(output) }
        return done
    }

    private fun encodeEntry(context: Context, entry: FaceTemplate.Entry, uri: Uri): ByteArray {
        val head = readHead(context, uri, HEAD_SIZE)
        val isImage = isKnownImage(head)
        if (!isImage && TgaDecoder.looksLike(head)) {
            val raw = readAll(context, uri, MAX_FILE_SIZE)
            val passthrough = IconCodec.encodeTga(raw, entry.width, entry.height)
            if (passthrough != null && passthrough.size == entry.size) {
                return passthrough
            }
        }
        val bitmap = loadBitmap(context, uri, 1600)
        var cropped = coverCrop(bitmap, entry.width, entry.height)
        when (entry.group) {
            // 卡包封面按官方模板规格切透明圆角，与手环原生卡片外观一致
            "card" -> cropped = roundCorners(cropped, (entry.width * CARD_CORNER_RATIO).toInt().coerceIn(4, 64))
            // 控制中心图标是整圆轮廓（原厂图标半径≈宽/2），按圆形裁剪
            "cc" -> cropped = roundCorners(cropped, entry.width / 2)
        }
        val pixels = IntArray(entry.width * entry.height)
        cropped.getPixels(pixels, 0, entry.width, 0, 0, entry.width, entry.height)
        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()
        return IconCodec.encode(pixels, entry.width, entry.height, entry.colorFormat)
    }

    fun loadThumbnail(context: Context, uri: Uri, maxSize: Int = 480): Bitmap? = try {
        loadBitmap(context, uri, maxSize)
    } catch (e: Exception) {
        null
    }

    fun loadBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val head = readHead(context, uri, HEAD_SIZE)
        if (!isKnownImage(head) && TgaDecoder.looksLike(head)) {
            return scaleDown(TgaDecoder.decode(readAll(context, uri, MAX_FILE_SIZE)), maxSize)
        }

        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("图片无法解码")

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSize) sample = sample shl 1

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("图片无法解码")

        val rotation = readRotation(context, uri)
        if (rotation == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun readRotation(context: Context, uri: Uri): Int {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun readHead(context: Context, uri: Uri, size: Int): ByteArray {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取图片")
        stream.use {
            val buffer = ByteArray(size)
            var read = 0
            while (read < size) {
                val n = it.read(buffer, read, size - read)
                if (n < 0) break
                read += n
            }
            return if (read == size) buffer else buffer.copyOf(read)
        }
    }

    private fun readAll(context: Context, uri: Uri, limit: Int): ByteArray {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取图片")
        stream.use {
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(65536)
            while (true) {
                val n = it.read(buffer)
                if (n <= 0) break
                out.write(buffer, 0, n)
                if (out.size() > limit) throw IOException("图片过大")
            }
            return out.toByteArray()
        }
    }

    private fun isKnownImage(head: ByteArray): Boolean {
        if (head.size >= 3 && (head[0].toInt() and 0xFF) == 0xFF &&
            (head[1].toInt() and 0xFF) == 0xD8 && (head[2].toInt() and 0xFF) == 0xFF
        ) return true
        if (head.size >= 8 && (head[0].toInt() and 0xFF) == 0x89 &&
            head[1] == 'P'.code.toByte() && head[2] == 'N'.code.toByte() && head[3] == 'G'.code.toByte()
        ) return true
        if (head.size >= 6 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte()
        ) return true
        if (head.size >= 12 && head[0] == 'R'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() && head[3] == 'F'.code.toByte() &&
            head[8] == 'W'.code.toByte() && head[9] == 'E'.code.toByte() &&
            head[10] == 'B'.code.toByte() && head[11] == 'P'.code.toByte()
        ) return true
        return head.size >= 2 && head[0] == 'B'.code.toByte() && head[1] == 'M'.code.toByte()
    }

    private fun coverCrop(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val scale = maxOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val scaledWidth = maxOf(1, Math.round(bitmap.width * scale))
        val scaledHeight = maxOf(1, Math.round(bitmap.height * scale))
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        return Bitmap.createBitmap(
            scaled,
            maxOf(0, (scaledWidth - width) / 2),
            maxOf(0, (scaledHeight - height) / 2),
            minOf(width, scaledWidth),
            minOf(height, scaledHeight),
        )
    }

    /**
     * 给图标/封面切透明圆角；radius 达到宽度一半时即为圆形（控制中心图标样式）。
     * 卡片圆角比例按官方模板量得：196x120 的卡片圆角约 11px，即宽度的 11/196。
     */
    private fun roundCorners(bitmap: Bitmap, radiusPx: Int): Bitmap {
        val radius = radiusPx.coerceIn(4, 256)
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(
                bitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            radius.toFloat(),
            radius.toFloat(),
            paint,
        )
        if (out !== bitmap) bitmap.recycle()
        return out
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSize) return bitmap
        val scale = maxSize.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            maxOf(1, Math.round(bitmap.width * scale)),
            maxOf(1, Math.round(bitmap.height * scale)),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
