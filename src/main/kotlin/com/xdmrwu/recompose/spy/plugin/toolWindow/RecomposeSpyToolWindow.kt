package com.xdmrwu.recompose.spy.plugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.xdmrwu.recompose.spy.plugin.services.AdbConnectionService


class RecomposeSpyToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 获取插件服务实例
        val service = project.getService(AdbConnectionService::class.java)

        val toolWindowWrapper = ToolWindowContentWrapper(service, project)
        val content = ContentFactory.getInstance().createContent(toolWindowWrapper.getContentPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}