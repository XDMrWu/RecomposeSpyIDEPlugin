package com.xdmrwu.recompose.spy.plugin.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import javax.swing.SwingUtilities

/**
 * @Author: wulinpeng
 * @Date: 2025/7/2 22:44
 * @Description:
 */
abstract class JumpIRAction: AnAction() {

    companion object {
        const val IR_DIR = "build/outputs/ir-printer"
    }

    abstract val irType: String
    abstract val text: String

    override fun update(e: AnActionEvent) {
        e.presentation.text = text
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile: KtFile = event.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        val virtualFile = psiFile.virtualFile ?: return
        val moduleDir = getModuleDir(virtualFile, project) ?: return

        val packageName = psiFile.packageFqName.asString().replace(".", "/")
        val irFile = File(getIrFilePath(moduleDir, packageName, psiFile.name))
        if (!irFile.exists()) {
            Messages.showInfoMessage(project, "Please make sure you have enabled ir printer in the gradle plugin", "IR File Not Found")
        } else {
            val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(irFile) ?: return
            FileEditorManager.getInstance(project).openFile(virtualFile)
        }
    }

    private fun getModuleDir(virtualFile: VirtualFile, project: Project): String? {
        val module = ModuleUtil.findModuleForFile(virtualFile, project) ?: return null
        return ExternalSystemApiUtil.getExternalProjectPath(module)
    }

    private fun getIrFilePath(moduleDir: String, packageName: String, fileName: String): String {
        return "$moduleDir/$IR_DIR/$irType/$packageName/$fileName"
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

class JumpRawIRAction : JumpIRAction() {
    override val irType: String = "raw-ir"
    override val text: String = "Raw IR"
}

class JumpComposeStyleIRAction : JumpIRAction() {
    override val irType: String = "compose-style-ir"
    override val text: String = "Compose Style IR"
}