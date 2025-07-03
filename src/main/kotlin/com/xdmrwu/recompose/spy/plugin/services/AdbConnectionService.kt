package com.xdmrwu.recompose.spy.plugin.services

import com.android.ddmlib.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.xdmrwu.recompose.spy.plugin.services.DeviceManager.IDeviceInfoListener

@Service(Service.Level.PROJECT)
class AdbConnectionService : Disposable {

    private val logger = Logger.getInstance(AdbConnectionService::class.java)
    private var adbBridge: AndroidDebugBridge? = null

    private val deviceManager: DeviceManager = DeviceManager(logger)

    private val deviceChangeListener = object : AndroidDebugBridge.IDeviceChangeListener {
        override fun deviceConnected(device: IDevice) {
            deviceManager.handleDeviceConnected(device)
        }

        override fun deviceDisconnected(device: IDevice) {
            deviceManager.handleDeviceDisconnected(device)
        }

        override fun deviceChanged(device: IDevice, changeMask: Int) {
            // 设备状态变化，例如从 offline 到 online
            if (changeMask and IDevice.CHANGE_STATE != 0) {
                if (device.isOnline) {
                    // 可以重新尝试连接或转发
                    deviceManager.handleDeviceConnected(device)
                } else {
                    deviceManager.handleDeviceDisconnected(device)
                }
            }
        }
    }.apply {
        // 添加设备状态监听器
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    fun addDeviceInfoListener(listener: IDeviceInfoListener) {
        deviceManager.addListener(listener)
    }

    // 初始化 ADB 连接
    fun connectToAdb() {
        if (adbBridge != null && adbBridge!!.isConnected) {
            logger.info("ADB Bridge already connected.")
            return
        }

        // 初始化前先清理旧的连接
        deviceManager.dispose()

        adbBridge = AndroidDebugBridge.getBridge()

        if (adbBridge == null) {
            logger.error("Failed to create ADB Bridge.")
            return
        }

        // 处理所有已经连接的设备
        adbBridge?.devices?.forEach { deviceManager.handleDeviceConnected(it) }

        logger.info("AdbConnectionService connected to ADB.")
    }

    // 实现 Disposable 接口，在插件被卸载或项目关闭时进行清理
    override fun dispose() {
        logger.info("AdbConnectionService disposing...")
        // 移除设备监听器
        AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener)

        deviceManager.dispose()
        adbBridge = null
    }
}