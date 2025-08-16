package com.github.nacabaro.vbhelper.nfc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class Acr122uReader(private val context: Context) {
    companion object {
        private const val TAG = "Acr122uReader"
        private const val ACR122U_VENDOR_ID = 0x072f
        private const val ACR122U_PRODUCT_ID = 0x2200
        private const val ACR122U_ALTERNATIVE_PRODUCT_ID = 0x90cc
        
        // PC/SC Commands for ACR122U
        private const val CLA = 0xFF.toByte()
        private const val INS_GET_FIRMWARE_VERSION = 0x00.toByte()
        private const val INS_GET_READER_STATUS = 0x00.toByte()
        private const val INS_LOAD_AUTHENTICATION_KEYS = 0x00.toByte()
        private const val INS_AUTHENTICATE = 0x00.toByte()
        private const val INS_READ_BINARY = 0xB0.toByte()
        private const val INS_WRITE_BINARY = 0xD6.toByte()
        private const val INS_UPDATE_BINARY = 0xD7.toByte()
        private const val INS_GET_UID = 0xCA.toByte()
        private const val INS_POWER_ON_CARD = 0x00.toByte()
        private const val INS_POWER_OFF_CARD = 0x00.toByte()
        
        // P1 and P2 parameters
        private const val P1_GET_FIRMWARE_VERSION = 0x00.toByte()
        private const val P2_GET_FIRMWARE_VERSION = 0x00.toByte()
        private const val P1_GET_UID = 0x00.toByte()
        private const val P2_GET_UID = 0x00.toByte()
    }
    
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: android.hardware.usb.UsbInterface? = null
    private var usbEndpointIn: android.hardware.usb.UsbEndpoint? = null
    private var usbEndpointOut: android.hardware.usb.UsbEndpoint? = null
    
    private val isConnected = AtomicBoolean(false)
    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (device != null && isAcr122u(device)) {
                        Log.d(TAG, "ACR122U device attached")
                        requestPermission(device)
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (device != null && isAcr122u(device)) {
                        Log.d(TAG, "ACR122U device detached")
                        disconnect()
                    }
                }
                "android.hardware.usb.action.USB_PERMISSION" -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                        Log.d(TAG, "USB permission granted")
                        connectToDevice(device)
                    } else {
                        Log.e(TAG, "USB permission denied")
                    }
                }
            }
        }
    }
    
    fun isAcr122u(device: UsbDevice): Boolean {
        return device.vendorId == ACR122U_VENDOR_ID && 
               (device.productId == ACR122U_PRODUCT_ID || device.productId == ACR122U_ALTERNATIVE_PRODUCT_ID)
    }
    
    fun findAcr122uDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        return deviceList.values.find { isAcr122u(it) }
    }
    
    fun requestPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent("android.hardware.usb.action.USB_PERMISSION"), 
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    fun connectToDevice(device: UsbDevice): Boolean {
        return try {
            usbDevice = device
            usbConnection = usbManager.openDevice(device)
            
            if (usbConnection == null) {
                Log.e(TAG, "Failed to open USB connection")
                return false
            }
            
            // Find the interface (ACR122U typically has one interface)
            usbInterface = device.getInterface(0)
            
            // Find bulk transfer endpoints
            for (i in 0 until usbInterface!!.endpointCount) {
                val endpoint = usbInterface!!.getEndpoint(i)
                when (endpoint.direction) {
                    android.hardware.usb.UsbConstants.USB_DIR_IN -> usbEndpointIn = endpoint
                    android.hardware.usb.UsbConstants.USB_DIR_OUT -> usbEndpointOut = endpoint
                }
            }
            
            if (usbEndpointIn == null || usbEndpointOut == null) {
                Log.e(TAG, "Failed to find USB endpoints")
                return false
            }
            
            // Claim the interface
            if (!usbConnection!!.claimInterface(usbInterface, true)) {
                Log.e(TAG, "Failed to claim USB interface")
                return false
            }
            
            isConnected.set(true)
            Log.d(TAG, "Successfully connected to ACR122U")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to ACR122U", e)
            false
        }
    }
    
    fun disconnect() {
        try {
            usbConnection?.releaseInterface(usbInterface)
            usbConnection?.close()
            usbConnection = null
            usbInterface = null
            usbEndpointIn = null
            usbEndpointOut = null
            usbDevice = null
            isConnected.set(false)
            Log.d(TAG, "Disconnected from ACR122U")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from ACR122U", e)
        }
    }
    
    fun isConnected(): Boolean = isConnected.get()
    
    suspend fun getFirmwareVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val command = buildPcscCommand(
                CLA, INS_GET_FIRMWARE_VERSION, P1_GET_FIRMWARE_VERSION, P2_GET_FIRMWARE_VERSION, null
            )
            val response = sendPcscCommand(command)
            return@withContext if (response != null) String(response) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting firmware version", e)
            return@withContext null
        }
    }
    
    suspend fun powerOnCard(): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = buildPcscCommand(
                CLA, INS_POWER_ON_CARD, 0x00.toByte(), 0x00.toByte(), null
            )
            val response = sendPcscCommand(command)
            return@withContext response != null && response.size >= 2 && response[1] == 0x00.toByte()
        } catch (e: Exception) {
            Log.e(TAG, "Error powering on card", e)
            return@withContext false
        }
    }
    
    suspend fun getCardUid(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val command = buildPcscCommand(
                CLA, INS_GET_UID, P1_GET_UID, P2_GET_UID, null
            )
            val response = sendPcscCommand(command)
            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "Error getting card UID", e)
            return@withContext null
        }
    }
    
    suspend fun readCardData(blockNumber: Int, length: Int): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val command = buildPcscCommand(
                CLA, INS_READ_BINARY, blockNumber.toByte(), 0x00.toByte(), null
            )
            val response = sendPcscCommand(command)
            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "Error reading card data", e)
            return@withContext null
        }
    }
    
    suspend fun readVitalBraceletData(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Vital Bracelet cards typically store data in specific blocks
            // This is a simplified implementation - you may need to adjust based on the actual card structure
            val data = mutableListOf<Byte>()
            
            // Read multiple blocks to get the full character data
            for (block in 0..15) {
                val blockData = readCardData(block, 16)
                if (blockData != null) {
                    data.addAll(blockData.toList())
                }
            }
            
            return@withContext data.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Vital Bracelet data", e)
            return@withContext null
        }
    }
    
    private fun buildPcscCommand(cla: Byte, ins: Byte, p1: Byte, p2: Byte, data: ByteArray?): ByteArray {
        val command = mutableListOf<Byte>()
        command.add(cla)
        command.add(ins)
        command.add(p1)
        command.add(p2)
        
        if (data != null) {
            command.add(data.size.toByte())
            command.addAll(data.toList())
        } else {
            command.add(0x00.toByte()) // Le field (expected response length)
        }
        
        return command.toByteArray()
    }
    
    private fun sendPcscCommand(command: ByteArray): ByteArray? {
        if (!isConnected.get()) {
            Log.e(TAG, "Not connected to ACR122U")
            return null
        }
        
        return try {
            Log.d(TAG, "Sending PC/SC command: ${command.joinToString(", ") { "0x%02X".format(it) }}")
            
            // Send command
            val bytesWritten = usbConnection!!.bulkTransfer(usbEndpointOut, command, command.size, 1000)
            if (bytesWritten != command.size) {
                Log.e(TAG, "Failed to send complete command: $bytesWritten/${command.size} bytes")
                return null
            }
            
            // Read response
            val responseBuffer = ByteArray(1024)
            val bytesRead = usbConnection!!.bulkTransfer(usbEndpointIn, responseBuffer, responseBuffer.size, 1000)
            
            if (bytesRead > 0) {
                val response = responseBuffer.copyOf(bytesRead)
                Log.d(TAG, "Received response: ${response.joinToString(", ") { "0x%02X".format(it) }}")
                return response
            } else {
                Log.e(TAG, "No response received from ACR122U")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending PC/SC command to ACR122U", e)
            null
        }
    }
    
    fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction("android.hardware.usb.action.USB_PERMISSION")
        }
        context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
    
    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(permissionReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}
