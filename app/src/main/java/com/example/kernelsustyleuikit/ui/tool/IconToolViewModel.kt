package com.example.kernelsustyleuikit.ui.tool

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IconToolViewModel(application: Application) : AndroidViewModel(application) {

    /** tpl: 0=主模板(应用图标+卡包封面)  1=控制中心模板 */
    data class Slot(
        val entry: FaceTemplate.Entry,
        val tpl: Int,
        val uri: Uri? = null,
        val thumb: Bitmap? = null,
    ) {
        /** 全局唯一键：两个模板里的 idx 会重复，用 tpl 前缀区分。 */
        val key: Int get() = tpl * 1000 + entry.idx
    }

    data class UiState(
        val loading: Boolean = true,
        val error: String? = null,
        val group: Int = 0,
        val slots: List<Slot> = emptyList(),
        val busy: Boolean = false,
        val status: String? = null,
    ) {
        private fun groupKey(group: Int): String = when (group) {
            0 -> "app"
            1 -> "card"
            else -> "cc"
        }

        val visible: List<Slot>
            get() = if (group == 2) {
                slots.filter { it.tpl == 1 }
            } else {
                slots.filter { it.tpl == 0 && it.entry.group == groupKey(group) }
            }

        fun total(group: Int): Int = if (group == 2) {
            slots.count { it.tpl == 1 }
        } else {
            slots.count { it.tpl == 0 && it.entry.group == groupKey(group) }
        }

        fun picked(group: Int): Int = if (group == 2) {
            slots.count { it.tpl == 1 && it.uri != null }
        } else {
            slots.count { it.tpl == 0 && it.entry.group == groupKey(group) && it.uri != null }
        }

        val pickedTotal: Int get() = slots.count { it.uri != null }
        val grandTotal: Int get() = slots.size
        val pickedMain: Int get() = slots.count { it.tpl == 0 && it.uri != null }
        val pickedCc: Int get() = slots.count { it.tpl == 1 && it.uri != null }
    }

    private var template: FaceTemplate? = null
    private var ccTemplate: FaceTemplate? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                runCatching {
                    val assets = getApplication<Application>().assets
                    val mainData = assets.open("tool.face").use { it.readBytes() }
                    val mainMapping = assets.open("mapping.json").use { it.readBytes().toString(Charsets.UTF_8) }
                    val main = FaceTemplate(mainData, mainMapping)
                    template = main

                    val ccData = assets.open("cc.face").use { it.readBytes() }
                    val ccMapping = assets.open("cc_mapping.json").use { it.readBytes().toString(Charsets.UTF_8) }
                    val cc = FaceTemplate(ccData, ccMapping)
                    ccTemplate = cc

                    UiState(
                        loading = false,
                        slots = main.entries.map { Slot(it, 0) } + cc.entries.map { Slot(it, 1) },
                    )
                }.getOrElse { error ->
                    UiState(loading = false, error = error.message ?: "模板加载失败")
                }
            }
            _uiState.value = state
        }
    }

    fun setGroup(group: Int) {
        if (_uiState.value.group == group) return
        _uiState.update { it.copy(group = group) }
    }

    fun hasPicks(tpl: Int): Boolean = _uiState.value.slots.any { it.tpl == tpl && it.uri != null }

    fun notifyEmpty() {
        _uiState.update { it.copy(status = "先选至少一张图片") }
    }

    fun onPicked(key: Int, uri: Uri) {
        val context = getApplication<Application>()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        _uiState.update { state ->
            state.copy(
                slots = state.slots.map {
                    if (it.key == key) it.copy(uri = uri, thumb = null) else it
                },
                status = "正在载入预览…",
            )
        }
        viewModelScope.launch {
            val thumb = withContext(Dispatchers.IO) {
                FaceGenerator.loadThumbnail(context, uri)
            }
            _uiState.update { state ->
                state.copy(
                    slots = state.slots.map {
                        if (it.key == key) it.copy(thumb = thumb) else it
                    },
                    status = null,
                )
            }
        }
    }

    fun clear(key: Int) {
        _uiState.update { state ->
            state.copy(
                slots = state.slots.map {
                    if (it.key == key) it.copy(uri = null, thumb = null) else it
                },
                status = null,
            )
        }
    }

    fun clearAll() {
        _uiState.update { state ->
            state.copy(slots = state.slots.map { it.copy(uri = null, thumb = null) }, status = null)
        }
    }

    /** 把某一模板里已选的图片写成一个 .face 文件。 */
    fun save(target: Uri, tpl: Int) {
        val current = if (tpl == 1) ccTemplate else template
        if (current == null) {
            _uiState.update { it.copy(status = "模板未就绪") }
            return
        }
        val picks = _uiState.value.slots
            .filter { it.tpl == tpl }
            .mapNotNull { slot -> slot.uri?.let { slot.entry.idx to it } }
            .toMap()
        if (picks.isEmpty()) {
            _uiState.update { it.copy(status = "先选至少一张图片") }
            return
        }
        _uiState.update { it.copy(busy = true, status = "正在生成，请稍候…") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    FaceGenerator.writeTo(getApplication(), target, current, picks)
                }
            }
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(busy = false, status = "已生成 $count 项，.face 保存完成")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(busy = false, status = "生成失败：${error.message ?: "未知错误"}")
                }
            }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(status = null) }
    }
}
