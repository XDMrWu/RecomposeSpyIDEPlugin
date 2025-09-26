package com.xdmrwu.recompose.spy.plugin.ui.ext

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 12:34
 * @Description:
 */

fun ComposePanel(content: @Composable () -> Unit): ComposePanel {
    return ComposePanel().apply {
        setContent {
            content()
        }
    }
}