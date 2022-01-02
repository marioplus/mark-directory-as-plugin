package com.github.marioplus.markdirectoryasplugin

import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.impl.DirectoryIndex
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import java.nio.file.Paths

class ShowPopupAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        ModuleManager.getInstance(project)
            .modules
            .map { it.rootManager.sourceRoots }
            .filter { it.isNotEmpty() }
            .map {
                listOf(
                    it[0].parent.path + "/xmeta_gen",
                    it[0].parent.path + "/xmeta_gen1",
                    it[0].parent.path + "/xmeta_gen2"
                )
            }
            .flatMap { it.asIterable() }
            .mapNotNull { VfsUtil.findFile(Paths.get(it), false) }
            .forEach {
                modifyRoots(
                    event,
                    com.github.marioplus.markdirectoryasplugin.getModule(event, listOf(it))!!,
                    arrayOf(it)
                )
            }
    }
}

fun getModule(e: AnActionEvent, files: List<VirtualFile>?): Module? {
    if (files == null) return null
    var module = e.getData(LangDataKeys.MODULE)
    if (module == null) {
        module = findParentModule(e.project, files)
    }
    return module
}

private fun findParentModule(project: Project?, files: List<VirtualFile>): Module? {
    if (project == null) return null
    var result: Module? = null
    val index = DirectoryIndex.getInstance(project)
    for (file in files) {
        val module = index.getInfoForFile(file).module ?: return null
        if (result == null) {
            result = module
        } else if (result != module) {
            return null
        }
    }
    return result
}
fun modifyRoots(e: AnActionEvent, module: Module, files: Array<VirtualFile?>) {
    val model = ModuleRootManager.getInstance(module).modifiableModel
    for (file in files) {
        val entry = MarkRootActionBase.findContentEntry(model, file!!)
        if (entry != null) {
            val sourceFolders = entry.sourceFolders
            for (sourceFolder in sourceFolders) {
                if (Comparing.equal(sourceFolder.file, file)) {
                    entry.removeSourceFolder(sourceFolder)
                    break
                }
            }
            modifyRoots(file, entry)
        }
    }
    commitModel(module, model)
}

fun commitModel(module: Module, model: ModifiableRootModel) {
    ApplicationManager.getApplication().runWriteAction {
        model.commit()
        module.project.save()
    }
}
fun findContentEntry(model: ModuleRootModel, vFile: VirtualFile): ContentEntry? {
    val contentEntries = model.contentEntries
    for (contentEntry in contentEntries) {
        val contentEntryFile = contentEntry.file
        if (contentEntryFile != null && VfsUtilCore.isAncestor(contentEntryFile, vFile, false)) {
            return contentEntry
        }
    }
    return null
}

fun modifyRoots(vFile: VirtualFile, entry: ContentEntry) {
    val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true)
    entry.addSourceFolder(vFile, JavaSourceRootType.SOURCE, properties)
}
//private fun modifyRoots(module: Module, path: String) {
//    // 有源码目录才标记
//    val sourceRoots = module.rootManager.sourceRoots
//    if (sourceRoots.isEmpty()) {
//        println("module: ${module.name} not has source dir")
//        return
//    }
//
//    val firstSourceRoot = sourceRoots.first()
//    // 是否已经标记
//    val markedFile = sourceRoots.find { sourceRoot -> sourceRoot.path.contains(path) }
//    val needMarkPath = firstSourceRoot.parent.path + "/" + path
//    if (markedFile != null) {
//        println("$needMarkPath 已经标记过了")
//        return
//    }
//
//    val needAddFolder = firstSourceRoot.parent.path + "/" + path
//    val properties = JpsJavaExtensionService.getInstance()
//        .createSourceRootProperties("", true)
//    module.rootManager
//        .modifiableModel
//        .contentEntries
//        .forEach { contentEntry ->
////                contentEntry.addSourceFolder(needAddFolder, JavaSourceRootType.SOURCE, properties)
//            contentEntry.addSourceFolder(needAddFolder, true)
//        }
//}

//private fun commitModel(module: Module) {
//    ApplicationManager.getApplication().runWriteAction(Runnable {
//        ModuleRootManager.getInstance(module).modifiableModel.commit()
//        module.project.saveStore()
//        module.project.save()
//        println("保存完毕")
//    })
//}