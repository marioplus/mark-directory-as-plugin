package com.github.marioplus.markdirectoryasplugin.util

import com.github.marioplus.markdirectoryasplugin.enums.MarkAsType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import java.nio.file.Paths

class ModuleSourceFolderUtils {

    companion object {

        /**
         * 添加源码目录
         */
        fun addSourceFolderForPath(
            project: Project,
            module: Module,
            markAsType: MarkAsType,
            folderPaths: List<String>,
        ) {
            val vFiles = folderPaths
                .mapNotNull { VfsUtil.findFile(Paths.get(it), false) }
            addSourceFolderForVFile(project, module, markAsType, vFiles)
        }

        /**
         * 添加源码目录
         */
        fun addSourceFolderForVFile(
            project: Project,
            module: Module,
            markAsType: MarkAsType,
            vFiles: List<VirtualFile>,
        ) {
            // 提交状态
            val sourceRootPaths = module.rootManager.sourceRoots
                .map(VirtualFile::getPath)
                .toHashSet()

            vFiles
                // 过滤不存在的目录
                .filter { it.exists() }
                .filterNot {
                    // 过滤已经添加的
                    sourceRootPaths.contains(it.path)
                }
                .filter {
                    // 当前 module 下的目录才添加
                    it.path.startsWith(ModuleUtil.getModuleDirPath(module))
                }
                .stream()

            if (vFiles.isEmpty()) {
                return
            }

            modifyRoots(module, markAsType, vFiles)
        }

        private fun modifyRoots(module: Module, markAsType: MarkAsType, vFiles: List<VirtualFile>) {
            if (vFiles.isEmpty()) {
                return
            }
            val modifiableModel = ModuleRootManager.getInstance(module).modifiableModel
            val entry = findContentEntry(modifiableModel, vFiles[0]) ?: return

            for (vFile in vFiles) {
                val sourceFolders = entry.sourceFolders
                for (sourceFolder in sourceFolders) {
                    if (Comparing.equal(sourceFolder.file, vFile)) {
                        entry.removeSourceFolder(sourceFolder)
                        break
                    }
                }
                modifyRoots(entry, markAsType, vFile)
                NotificationUtils.markAsNotify("需要将 ${vFile.path} 标记为 ${markAsType.displayName}")
            }
            commitModel(module, modifiableModel)
        }

        private fun modifyRoots(entry: ContentEntry, markAsType: MarkAsType, vFile: VirtualFile) {
            when (markAsType) {
                MarkAsType.NORMAL -> return
                MarkAsType.RESOURCE -> entry.addSourceFolder(vFile, JavaResourceRootType.RESOURCE)
                MarkAsType.TEST_RESOURCE -> entry.addSourceFolder(vFile, JavaResourceRootType.TEST_RESOURCE)
                MarkAsType.SOURCE -> entry.addSourceFolder(vFile, JavaSourceRootType.SOURCE)
                MarkAsType.TEST_SOURCE -> entry.addSourceFolder(vFile, JavaSourceRootType.TEST_SOURCE)
                MarkAsType.GENERATED_SOURCE -> {
                    val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true)
                    entry.addSourceFolder(vFile, JavaSourceRootType.SOURCE, properties)
                }
            }
        }

        private fun commitModel(module: Module, model: ModifiableRootModel) {
            ApplicationManager.getApplication().runWriteAction {
                model.commit()
                module.project.save()
            }
        }

        private fun findContentEntry(model: ModuleRootModel, vFile: VirtualFile): ContentEntry? {
            return model.contentEntries
                .filterNot { it == null || it.file == null }
                .find { VfsUtilCore.isAncestor(it.file!!, vFile, false) }
        }

        fun isJavaModule(module: Module): Boolean {
            val moduleType = ModuleType.get(module)
            return moduleType.id == ModuleTypeId.JAVA_MODULE
        }

    }
}