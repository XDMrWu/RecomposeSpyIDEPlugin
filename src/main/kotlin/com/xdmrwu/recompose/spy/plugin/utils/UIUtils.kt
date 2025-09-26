package com.xdmrwu.recompose.spy.plugin.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * @Author: wulinpeng
 * @Date: 2025/9/26 18:41
 * @Description:
 */
fun showNotify(project: Project, message: String, groupId: String = "RecomposeSpy", type: NotificationType = NotificationType.ERROR) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(groupId)
        .createNotification(message, type)
        .notify(project)
}