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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kernelsustyleuikit.R
import com.example.kernelsustyleuikit.ui.tool.IconToolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerMaterial(
    state: IconToolViewModel.UiState,
    actions: IconToolActions,
    bottomInnerPadding: Dp,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            MaterialActionBar(state = state, actions = actions, bottomInnerPadding = bottomInnerPadding)
        },
    ) { innerPadding ->
        when {
            state.loading -> CenterMessage("正在读取 .face 模板…")
            state.error != null -> CenterMessage("模板加载失败：${state.error}", isError = true)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { MaterialSummary(state) }
                item(span = { GridItemSpan(maxLineSpan) }) { MaterialTabs(state, actions) }
                items(state.visible, key = { it.entry.idx }) { slot ->
                    MaterialSlotCell(slot, actions)
                }
            }
        }
    }
}

@Composable
private fun MaterialSummary(state: IconToolViewModel.UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "已选 ${state.pickedTotal} / ${state.grandTotal} 项",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "点按方块挑图，已选方块点右上角 × 可移除；未选的格子会保留模板原图。卡包封面自动切圆角、控制中心自动切圆形，与手环原生外观一致。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val status = state.status
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MaterialTabs(state: IconToolViewModel.UiState, actions: IconToolActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.group == 0,
            onClick = { actions.onGroup(0) },
            label = { Text("应用图标 ${state.picked(0)}/${state.total(0)}") },
        )
        FilterChip(
            selected = state.group == 1,
            onClick = { actions.onGroup(1) },
            label = { Text("卡包封面 ${state.picked(1)}/${state.total(1)}") },
        )
        FilterChip(
            selected = state.group == 2,
            onClick = { actions.onGroup(2) },
            label = { Text("控制中心 ${state.picked(2)}/${state.total(2)}") },
        )
    }
}

@Composable
private fun MaterialSlotCell(slot: IconToolViewModel.Slot, actions: IconToolActions) {
    val picked = slot.uri != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { actions.onPick(slot.key) },
        colors = CardDefaults.cardColors(
            containerColor = if (picked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface),
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
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Text(text = "×", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = slot.entry.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${slot.entry.width}×${slot.entry.height}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MaterialActionBar(
    state: IconToolViewModel.UiState,
    actions: IconToolActions,
    bottomInnerPadding: Dp,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 抬升到底部导航栏之上，避免重叠
                .padding(bottom = bottomInnerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = actions.onClearAll,
                modifier = Modifier.weight(1f),
                enabled = state.pickedTotal > 0 && !state.busy,
            ) {
                Text("清空")
            }
            Button(
                onClick = actions.onSave,
                modifier = Modifier.weight(2f),
                enabled = state.pickedTotal > 0 && !state.busy,
            ) {
                Text(if (state.busy) "生成中…" else "生成 .face")
            }
        }
    }
}

@Composable
private fun CenterMessage(message: String, isError: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}
