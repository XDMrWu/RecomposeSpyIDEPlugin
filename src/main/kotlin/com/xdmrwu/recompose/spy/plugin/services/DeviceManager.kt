package com.xdmrwu.recompose.spy.plugin.services

import com.android.ddmlib.IDevice
import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * @Author: wulinpeng
 * @Date: 2025/6/25 22:24
 * @Description:
 */
class DeviceManager(val logger: Logger) {

    interface IDeviceInfoListener {
        fun onDeviceConnected(device: DeviceWrapper)
        fun onDeviceDisconnected(device: DeviceWrapper)
        fun onReceivedData(device: IDevice, data: String)
    }

    // 存储每个设备的 Socket 服务器和相关线程，以便管理
    private val devices = ConcurrentHashMap<String, DeviceWrapper>()
    private val listeners = mutableListOf<IDeviceInfoListener>()

    fun addListener(listener: IDeviceInfoListener) {
        listeners.add(listener)
    }

    // 当设备连接时处理
    fun handleDeviceConnected(device: IDevice) {
        if (!device.isOnline) {
            logger.info("Device ${device.serialNumber} not online yet, skipping setup.")
            return
        }
        if (devices.contains(device.serialNumber)) {
            logger.warn("Device ${device.serialNumber} already has a server, skipping.")
            return // 已经为该设备设置了
        }

        logger.info("Device connected: ${device.serialNumber}")

        DeviceWrapper(device, logger, ::onReceivedData).let {
            devices[device.serialNumber] = it
            it.connectSocket()
            listeners.forEach { listener ->
                listener.onDeviceConnected(it) // 通知所有监听器
            }
        }
    }

    // 当设备断开连接时处理
    fun handleDeviceDisconnected(device: IDevice) {
        logger.info("Device disconnected: ${device.serialNumber}")
        // 关闭 Socket 服务器和相关线程
        devices[device.serialNumber]?.dispose()
        val wrapper = devices.remove(device.serialNumber) ?: return
        logger.info("Cleanup complete for device ${device.serialNumber}.")
        listeners.forEach { listener ->
            listener.onDeviceDisconnected(wrapper) // 通知所有监听器
        }
    }

    fun onReceivedData(deviceSerial: String, data: String) {
        devices[deviceSerial]?.let { deviceWrapper ->
            listeners.forEach { listener ->
                listener.onReceivedData(deviceWrapper.device, data) // 通知所有监听器
            }
        } ?: logger.warn("No device found for serial $deviceSerial when receiving data.")
    }

    fun dispose() {
        devices.values.forEach {
            it.dispose()
            listeners.forEach { listener ->
                listener.onDeviceDisconnected(it) // 通知所有监听器
            }
        }
        devices.clear()
    }
}

/**
 * 封装 IDevice，处理 app 侧端口的更新逻辑以及 Socket 连接管理
 */
class DeviceWrapper(val device: IDevice, val logger: Logger, val onReceivedData: (String, String) -> Unit) {
    companion object {
        private const val APP_PORT = 50000 // App 端口
    }
    private val localPort by lazy { getEmptyPort() } // 获取一个空闲端口
    private val serverSocket: ServerSocket by lazy {
        ServerSocket(localPort)
    }
    private var socketThread: Thread? = null

    @Synchronized
    fun connectSocket() {
        if (socketThread != null) {
            return
        }
        logger.info("Port reverse set up for ${device.serialNumber}: host:$localPort -> device:$APP_PORT")
        socketThread = thread(name = "SocketListener-${device.serialNumber}") {
            runCatching {
                logger.info("Listening for app connections on port $localPort for device ${device.serialNumber}...")

                while (!serverSocket.isClosed && !Thread.currentThread().isInterrupted) {
                    val clientSocket = serverSocket.accept() // 阻塞，直到有客户端连接
                    logger.info("App connected from ${clientSocket.inetAddress} for device ${device.serialNumber}")
                    runCatching {
                        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                        // 读取数据直到客户端关闭连接
                        logger.info("Reading data from app socket for device ${device.serialNumber}...")
                        val received = reader.readText() // 读取所有数据

                        logger.info("Received data from app on ${device.serialNumber}: ${received}")
                        onReceivedData.invoke(device.serialNumber, received) // 调用回调处理数据
                    }.onFailure { e ->
                        logger.warn("Error reading from app socket for device ${device.serialNumber}: ${e.message}", e)
                    }
                    clientSocket.close()
                    logger.info("App socket closed for device ${device.serialNumber}")
                }
            }.onFailure { e ->
                if (!serverSocket.isClosed) { // 只有在不是因为 serverSocket 关闭而抛出异常时才记录为错误
                    logger.error("Error in server socket listener for device ${device.serialNumber}: ${e.message}", e)
                }
            }
            if (!serverSocket.isClosed) {
                serverSocket.close()
            }
            logger.info("Server socket closed for device ${device.serialNumber}")
        }
    }

    fun updateStatus(isRecording: Boolean): Boolean {
        return runCatching {
            if (isRecording) {
                // 设置端口转发
                device.createReverse(APP_PORT, localPort)
            } else {
                device.removeReverse(APP_PORT)
            }
        }.isSuccess
    }

    fun dispose() {
        // 移除端口转发
        try {
            device.removeReverse(APP_PORT)
            logger.info("Port reverse removed for ${device.serialNumber}.")
        } catch (e: Exception) {
            logger.warn("Error removing port reverse for ${device.serialNumber}: ${e.message}", e)
        }
        // 停止所有 Socket 服务器和线程
        if (!serverSocket.isClosed) {
            serverSocket.close()
        }
        socketThread?.interrupt()
    }

    private fun getEmptyPort(): Int {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort
        serverSocket.close()
        return port
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return device.serialNumber == (other as DeviceWrapper).device.serialNumber
    }

    override fun hashCode(): Int {
        return device.serialNumber.hashCode()
    }
}