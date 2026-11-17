package com.example.kernelsustyleuikit.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kernelsustyleuikit.R
import com.example.kernelsustyleuikit.ui.theme.LocalEnableBlur
import com.example.kernelsustyleuikit.ui.tool.IconToolViewModel
import com.example.kernelsustyleuikit.ui.util.BlurredBar
import com.example.kernelsustyleuikit.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: IconToolViewModel.UiState,
    actions: IconToolActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            ActionBar(state = state, actions = actions, bottomInnerPadding = bottomInnerPadding)
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            when {
                state.loading -> MessageCard("正在读取 .face 模板…")
                state.error != null -> MessageCard("模板加载失败：${state.error}", isError = true)
                else -> SlotGrid(state = state, actions = actions, innerPadding = innerPadding, scrollBehavior = scrollBehavior)
            }
        }
    }
}

@Composable
private fun SlotGrid(
    state: IconToolViewModel.UiState,
    actions: IconToolActions,
    innerPadding: PaddingValues,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        overscrollEffect = null,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SummaryCard(state)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            GroupTabs(state = state, actions = actions)
        }
        items(state.visible, key = { it.entry.idx }) { slot ->
            SlotCell(slot = slot, actions = actions)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SummaryCard(state: IconToolViewModel.UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "已选 ${state.pickedTotal} / ${state.grandTotal} 项",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "点按方块挑图，已选方块点右上角 × 可移除；未选的格子会保留模板原图。卡包封面自动切圆角、控制中心自动切圆形，与手环原生外观一致。",
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
            val status = state.status
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = status, fontSize = 13.sp, color = colorScheme.primary)
            }
        }
    }
}

@Composable
private fun GroupTabs(state: IconToolViewModel.UiState, actions: IconToolActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabCard(
            text = "应用图标 ${state.picked(0)}/${state.total(0)}",
            selected = state.group == 0,
            modifier = Modifier.weight(1f),
            onClick = { actions.onGroup(0) },
        )
        TabCard(
            text = "卡包封面 ${state.picked(1)}/${state.total(1)}",
            selected = state.group == 1,
            modifier = Modifier.weight(1f),
            onClick = { actions.onGroup(1) },
        )
        TabCard(
            text = "控制中心 ${state.picked(2)}/${state.total(2)}",
            selected = state.group == 2,
            modifier = Modifier.weight(1f),
            onClick = { actions.onGroup(2) },
        )
    }
}

@Composable
private fun TabCard(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(
            color = if (selected) colorScheme.primary else colorScheme.surfaceVariant
        ),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SlotCell(slot: IconToolViewModel.Slot, actions: IconToolActions) {
    val picked = slot.uri != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = if (picked) colorScheme.secondaryContainer else colorScheme.surface
        ),
        onClick = { actions.onPick(slot.key) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val thumb = slot.thumb
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = slot.entry.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "+",
                        fontSize = 24.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
                if (picked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .clickable { actions.onClear(slot.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "×", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = slot.entry.label,
                fontSize = 12.sp,
                color = if (picked) colorScheme.primary else colorScheme.onSurface,
            )
            Text(
                text = "${slot.entry.width}×${slot.entry.height}",
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun ActionBar(
    state: IconToolViewModel.UiState,
    actions: IconToolActions,
    bottomInnerPadding: Dp,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 抬升到浮动底部导航栏之上，避免与「主页/设置」药丸重叠
            .padding(bottom = bottomInnerPadding)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = "清空",
                onClick = actions.onClearAll,
                modifier = Modifier.weight(1f),
                enabled = state.pickedTotal > 0 && !state.busy,
            )
            Button(
                onClick = actions.onSave,
                modifier = Modifier.weight(2f),
                enabled = state.pickedTotal > 0 && !state.busy,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = if (state.busy) "生成中…" else "生成 .face", fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp,
            color = if (isError) colorScheme.error else colorScheme.onSurface,
        )
    }
}
