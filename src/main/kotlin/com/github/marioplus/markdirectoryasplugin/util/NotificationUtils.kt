package com.github.marioplus.markdirectoryasplugin.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

class NotificationUtils {

    companion object {

        private fun defaultProject(): Project {
            return ProjectManager.getInstance().defaultProject
        }

        private fun notify(
            project: Project = defaultProject(),
            title: String,
            content: String,
            notificationType: NotificationType = NotificationType.INFORMATION,
            listener: NotificationListener? = null,
        ) {
            val notification = Notification(
                Notifications.SYSTEM_MESSAGES_GROUP_ID,
                title,
                content,
                notificationType,
                listener)
            notify(project, notification)
        }

        private fun notify(
            project: Project = ProjectManager.getInstance().defaultProject,
            notification: Notification,
        ) {
            Notifications.Bus.notify(notification, project)
        }

        fun markAsNotify(
            content: String,
            notificationType: NotificationType = NotificationType.INFORMATION,
            project: Project = defaultProject(),
        ) {
            notify(project, "mark as", content, notificationType)
        }
    }
}