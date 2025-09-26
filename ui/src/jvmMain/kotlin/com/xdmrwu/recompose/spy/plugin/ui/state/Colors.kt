package com.xdmrwu.recompose.spy.plugin.ui.state

import androidx.compose.ui.graphics.Color

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 13:53
 * @Description:
 */
interface Colors {
    val textColor: Color
    val textSecondaryColor: Color
    val backgroundColor: Color
    val backgroundSecondaryColor: Color
    val buttonBackgroundColor: Color
    val dividerColor: Color
}

object LightColors : Colors {
    override val textColor = Color.Black
    override val textSecondaryColor = Color(77, 85, 99)
    override val backgroundColor = Color.White
    override val backgroundSecondaryColor = Color(247, 248, 250)
    override val buttonBackgroundColor = Color.White
    override val dividerColor =  Color(229, 231, 235)
}

object DarkColors : Colors {
    override val textColor = Color(0xFFFFFFFF)
    override val textSecondaryColor = Color(153, 159, 170)
    override val backgroundColor = Color(18, 24, 38)
    override val backgroundSecondaryColor = Color(32, 41, 55)
    override val buttonBackgroundColor = Color(56, 65, 80)
    override val dividerColor =  Color(55, 63, 79)
}