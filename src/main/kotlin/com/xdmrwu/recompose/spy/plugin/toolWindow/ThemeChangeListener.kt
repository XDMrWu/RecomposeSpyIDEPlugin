package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsListener

/**
 * @Author: wulinpeng
 * @Date: 2025/10/20 20:02
 * @Description:
 */
var onThemeChange: (() -> Unit)? = null

class ThemeChangeListener: UISettingsListener {
    override fun uiSettingsChanged(p0: UISettings) {
        onThemeChange?.invoke()
    }
}