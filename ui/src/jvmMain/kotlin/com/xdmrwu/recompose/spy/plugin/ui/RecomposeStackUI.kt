@file:OptIn(ExperimentalFoundationApi::class)

package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xdmrwu.recompose.spy.plugin.ui.state.Recomposition
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState
import kotlin.math.max

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 18:54
 * @Description:
 */
@Composable
fun RecomposeStackUI(
    state: UiState,
    openFile: (Recomposition) -> Unit
) {
    val colors = state.colors
    val selectedDevice = state.selectedDevice
    Box(
        Modifier.fillMaxSize()
            .padding(10.dp)
    ) {
        if (selectedDevice?.recompositionList?.isNotEmpty() != true) {
            Text(
                "No Data",
                color = colors.textColor,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                state.selectedDevice?.recompositionList?.forEach { recomposition ->
                    RecomposeStackItem(recomposition, recomposition, state, openFile)
                    Divider(
                        Modifier.fillMaxWidth()
                            .height(10.dp)
                            .background(colors.backgroundColor),
                        color = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun RecomposeStackItem(
    recomposition: Recomposition,
    root: Recomposition,
    state: UiState,
    openFile: (Recomposition) -> Unit = {},
    indent: Int = 0
) {
    val colors = state.colors
    var expanded by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxWidth()
            .wrapContentHeight()
            .background(colors.backgroundSecondaryColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(
                    onDoubleClick = {
                        openFile(recomposition)
                    },
                    onClick = {
                        state.selectedDevice?.currentRecomposition = recomposition
                    }
                )
//                .padding(horizontal = 10.dp, vertical = 15.dp)
        ) {
            Divider(Modifier.width((indent * 20).dp), color = Color.Transparent)
            Divider(
                Modifier.width(5.dp)
                    .height(40.dp)
                    .padding(vertical = 4.dp)
                    .alpha(max(1f - indent * 0.1f, 0f))
                    .background(colors.textSecondaryColor)
            )
            Divider(Modifier.width(5.dp), color = Color.Transparent)
            if (recomposition.children.isNotEmpty()) {
                ArrowIcon(
                    expanded,
                    colors.textSecondaryColor,
                    Modifier.size(20.dp)
                        .padding(5.dp)
                        .clickable {
                            expanded = !expanded
                        }
                )
            }
            Text(
                recomposition.name,
                color = colors.textColor,
                maxLines = 1,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            Text(
                ":${recomposition.startLine}",
                color = colors.textSecondaryColor,
                maxLines = 1,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            Divider(Modifier.weight(1f), color = Color.Transparent)
            Text(
                "${recomposition.cost} ms",
                color = colors.textSecondaryColor,
                maxLines = 1,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
        }
        if (recomposition.children.isNotEmpty() && expanded) {
            recomposition.children.forEach {
                RecomposeStackItem(it, root, state, openFile, indent + 1)
            }
        }
    }
}

@Composable
private fun ArrowIcon(expanded: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        var path = if (expanded) {
            // 下箭头
            val width = size.width
            val height = size.height
            Path().apply {
                moveTo(0f, 0f)
                lineTo(width / 2, height * 0.7f)
                lineTo(width, 0f)
            }
        } else {
            // 右箭头
            val width = size.width
            val height = size.height
            Path().apply {
                moveTo(0f, 0f)
                lineTo(width * 0.7f, height / 2)
                lineTo(0f, height)
            }
        }

        drawPath(path = path, color = color, style = Stroke(width = 2f))
    }
}