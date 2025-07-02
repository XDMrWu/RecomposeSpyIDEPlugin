package com.xdmrwu.recompose.spy.plugin.action

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.SwingUtilities

/**
 * @Author: wulinpeng
 * @Date: 2025/7/2 22:44
 * @Description:
 */
class JumpIRActionGroup: DefaultActionGroup() {

    override fun update(e: AnActionEvent) {
        val psiFile = getPsiFile(e)
        e.presentation.isEnabledAndVisible =
            (psiFile != null && psiFile is KtFile && !psiFile.isScript())
    }

    private fun getPsiFile(e: AnActionEvent): PsiFile? {
        var result: PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if (result == null) {
            val virtualFile = getVirtualFile(e) ?: return null
            result = PsiManager.getInstance(e.project!!).findFile(virtualFile)
        }
        return result
    }

    private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
        return e.getData(CommonDataKeys.VIRTUAL_FILE) ?: e.getData(CommonDataKeys.EDITOR)?.virtualFile
    }

    override fun getChildren(p0: AnActionEvent?): Array<out AnAction?> {
        return arrayOf(JumpRawIRAction(), JumpComposeStyleIRAction())
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}