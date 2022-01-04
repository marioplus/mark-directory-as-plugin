package com.github.marioplus.markdirectoryasplugin.action

import com.github.marioplus.markdirectoryasplugin.constant.Constant
import com.github.marioplus.markdirectoryasplugin.enums.MarkAsType
import com.github.marioplus.markdirectoryasplugin.util.ModuleSourceFolderUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory

class MarkXmetaGenAsAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(MarkAsType.displayNames())
            .setItemChosenCallback() {
                it ?: return@setItemChosenCallback
                val markAsType = MarkAsType.ofDisplayName(it) ?: return@setItemChosenCallback
                doMark(project, markAsType)
            }
            .createPopup()
            .showInFocusCenter()
    }

    /**
     * 执行标记操作
     * @param project 工程
     * @param markAsType 标记类型
     */
    private fun doMark(project: Project, markAsType: MarkAsType) {
        ModuleManager.getInstance(project).modules
            .filter { ModuleSourceFolderUtils.isJavaModule(it) }
            // module -> paths
            .associateBy(
                { it },
                { module ->
                    arrayOf(Constant.X_META_GEN)
                        // module path + path
                        .map { ModuleUtil.getModuleDirPath(module) + it }
                }
            )
            .forEach {
                ModuleSourceFolderUtils.addSourceFolderForPath(project, it.key, markAsType, it.value)
            }
    }
}