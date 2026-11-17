package com.example.kernelsustyleuikit.ui.tool

import org.json.JSONObject

/**
 * `.face` 容器解析：定位 FPK1 段，逐项读出偏移/长度/颜色格式/尺寸，
 * 再用 mapping.json 补上中文名称与分组。
 */
class FaceTemplate(val data: ByteArray, mappingJson: String) {

    class Entry(
        val idx: Int,
        val dataOff: Int,
        val size: Int,
        val colorFormat: Int,
        val width: Int,
        val height: Int,
        val label: String,
        val group: String,
    )

    val entries: List<Entry>

    init {
        val fpk = findFpk(data)
        val count = u32(data, fpk + 4)
        require(count in 0..4096) { "模板条目数量异常：$count" }

        val meta = HashMap<Int, JSONObject>()
        val array = JSONObject(mappingJson).getJSONArray("entries")
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            meta[item.getInt("idx")] = item
        }

        val list = ArrayList<Entry>(count)
        var pos = fpk + 8
        for (i in 0 until count) {
            if (pos + 16 > data.size) break
            val size = u32(data, pos + 4)
            val dataOff = pos + 8
            if (size <= 0 || dataOff + size > data.size) break
            val item = meta[i]
            list += Entry(
                idx = i,
                dataOff = dataOff,
                size = size,
                colorFormat = data[pos + 9].toInt() and 0xFF,
                width = u16(data, pos + 12),
                height = u16(data, pos + 14),
                label = item?.optString("label").orEmpty().ifEmpty { "item $i" },
                group = item?.optString("group").orEmpty().ifEmpty { "app" },
            )
            pos = dataOff + size
        }
        entries = list
    }

    private fun findFpk(data: ByteArray): Int {
        val limit = minOf(1024, data.size - 4)
        for (i in 256..limit) {
            if (data[i] == 70.toByte() && data[i + 1] == 80.toByte() &&
                data[i + 2] == 75.toByte() && data[i + 3] == 49.toByte()
            ) {
                return i
            }
        }
        throw IllegalArgumentException("模板里没有找到 FPK1")
    }

    private fun u16(data: ByteArray, at: Int): Int =
        (data[at].toInt() and 0xFF) or ((data[at + 1].toInt() and 0xFF) shl 8)

    private fun u32(data: ByteArray, at: Int): Int =
        (data[at].toInt() and 0xFF) or
            ((data[at + 1].toInt() and 0xFF) shl 8) or
            ((data[at + 2].toInt() and 0xFF) shl 16) or
            ((data[at + 3].toInt() and 0xFF) shl 24)
}
