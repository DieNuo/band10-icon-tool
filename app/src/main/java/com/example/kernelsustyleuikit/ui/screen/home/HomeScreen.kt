package com.example.kernelsustyleuikit.ui.screen.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kernelsustyleuikit.ui.LocalUiMode
import com.example.kernelsustyleuikit.ui.UiMode
import com.example.kernelsustyleuikit.ui.navigation3.Navigator
import com.example.kernelsustyleuikit.ui.tool.IconToolViewModel

/** 首页交互回调，Miuix 与 Material 两种外观共用。参数为 Slot.key。 */
data class IconToolActions(
    val onPick: (Int) -> Unit,
    val onClear: (Int) -> Unit,
    val onClearAll: () -> Unit,
    val onSave: () -> Unit,
    val onGroup: (Int) -> Unit,
)

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<IconToolViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingIndex by remember { mutableIntStateOf(-1) }
    // 保存流程状态："main"=正在保存主模板 "cc"=正在保存控制中心模板 ""=空闲
    var pendingSave by remember { mutableStateOf("") }
    // 回调里需要再次发起第二个文件的保存对话框，用可空引用持有 launcher
    var saveLauncherRef by remember {
        mutableStateOf<androidx.activity.compose.ManagedActivityResultLauncher<String, android.net.Uri?>?>(null)
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && pendingIndex >= 0) {
            viewModel.onPicked(pendingIndex, uri)
        }
        pendingIndex = -1
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        when (pendingSave) {
            "main" -> {
                if (uri != null) viewModel.save(uri, 0)
                // 主模板保存后，若控制中心也选了图，接着保存第二个文件
                if (viewModel.hasPicks(1)) {
                    pendingSave = "cc"
                    runCatching { saveLauncherRef?.launch("控制中心_环10改图标_custom.face") }
                } else {
                    pendingSave = ""
                }
            }
            "cc" -> {
                if (uri != null) viewModel.save(uri, 1)
                pendingSave = ""
            }
        }
    }
    saveLauncherRef = saveLauncher

    val actions = IconToolActions(
        onPick = { key ->
            pendingIndex = key
            runCatching { pickLauncher.launch(arrayOf("image/*", "application/octet-stream")) }
        },
        onClear = viewModel::clear,
        onClearAll = viewModel::clearAll,
        onSave = {
            when {
                viewModel.hasPicks(0) -> {
                    pendingSave = "main"
                    runCatching { saveLauncher.launch("环10改图标_custom.face") }
                }
                viewModel.hasPicks(1) -> {
                    pendingSave = "cc"
                    runCatching { saveLauncher.launch("控制中心_环10改图标_custom.face") }
                }
                else -> viewModel.notifyEmpty()
            }
        },
        onGroup = viewModel::setGroup,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> HomePagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
