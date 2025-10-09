package com.xdmrwu.recompose.spy.plugin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.xdmrwu.recompose.spy.plugin.ui.ext.ComposePanel
import com.xdmrwu.recompose.spy.plugin.ui.state.UiState
import org.jetbrains.compose.resources.painterResource
import com.xdmrwu.recompose.spy.plugin.generated.Res
import com.xdmrwu.recompose.spy.plugin.generated.phone_dark
import com.xdmrwu.recompose.spy.plugin.generated.phone_light
import com.xdmrwu.recompose.spy.plugin.ui.state.Recomposition
import java.awt.Cursor

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 13:22
 * @Description:
 */
class WindowToolPanel {

    val state = UiState()
    var onSelectDevice: (String) -> Unit = {}
    var onClickRecord: () -> Unit = {}
    var openFile: (Recomposition) -> Unit = {}

    init {
        // fix compose cannot get resource with wrong classloader
        Thread.currentThread().contextClassLoader = javaClass.classLoader
    }

    fun getPanel() = ComposePanel {
        WindowToolPanelUI(state, onSelectDevice, onClickRecord, openFile)
    }
}

@Composable
internal fun WindowToolPanelUI(
    state: UiState,
    onSelectDevice: (String) -> Unit = {},
    onClickRecord: () -> Unit = {},
    openFile: (Recomposition) -> Unit = {}
) {
    BoxWithConstraints {
        val colors = state.colors
        val width = max(maxWidth, 1000.dp)
        val height = max(maxHeight, 600.dp)
        val density = LocalDensity.current
        Column(
            Modifier
                .width(width)
                .height(height)
                .background(colors.backgroundColor)
        ) {
            TitleBar(state, onSelectDevice, onClickRecord)
            Divider(color = colors.dividerColor, modifier = Modifier.height(2.dp).fillMaxWidth())
            var leftBoxWidth by remember { mutableStateOf(width / 4f) }
            Row(
                Modifier.weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .width(leftBoxWidth)
                        .fillMaxHeight()
                ) {
                    RecomposeStackUI(state, openFile)
                }

                Divider(
                    color = colors.dividerColor,
                    modifier = Modifier.fillMaxHeight()
                        .width(4.dp)
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                        .draggable(
                            state = rememberDraggableState {
                                leftBoxWidth += with(density) {it.toDp() }
                            },
                            orientation = Orientation.Horizontal
                        )
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnalyzeUI(state)
                }
            }
        }
    }
}