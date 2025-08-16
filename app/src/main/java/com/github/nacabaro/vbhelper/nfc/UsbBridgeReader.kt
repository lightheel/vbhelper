package com.github.nacabaro.vbhelper.nfc

import android.content.Context
import android.util.Log
import com.github.cfogrady.vbnfc.TagCommunicator
import com.github.cfogrady.vbnfc.data.NfcCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * USB Bridge Reader that connects to the Windows TCP bridge server
 * and presents ACR122U data as if it came from a physical device
 */
class UsbBridgeReader(private val context: Context) {
    companion object {
        private const val TAG = "UsbBridgeReader"
        private const val BRIDGE_HOST = "10.0.2.2"  // Host machine from emulator perspective
        private const val BRIDGE_PORT = 8888
        private const val CONNECTION_TIMEOUT = 35000  // 35 seconds to allow for card detection
    }
    
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private val isConnected = AtomicBoolean(false)
    private var lastCardData: JSONObject? = null
    
    /**
     * Connect to the USB bridge server
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to USB bridge server at $BRIDGE_HOST:$BRIDGE_PORT")
            
            socket = Socket(BRIDGE_HOST, BRIDGE_PORT)
            socket?.soTimeout = CONNECTION_TIMEOUT
            
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            writer = PrintWriter(socket!!.getOutputStream(), true)
            
            isConnected.set(true)
            Log.d(TAG, "Successfully connected to USB bridge server")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to USB bridge server", e)
            disconnect()
            return@withContext false
        }
    }
    
    /**
     * Disconnect from the USB bridge server
     */
    fun disconnect() {
        try {
            isConnected.set(false)
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from USB bridge", e)
        } finally {
            reader = null
            writer = null
            socket = null
        }
    }
    
    /**
     * Check if connected to the bridge server
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Read card data from the bridge server
     */
    suspend fun readCardData(): JSONObject? = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            Log.e(TAG, "Not connected to USB bridge server")
            return@withContext null
        }
        
        try {
            // Send a request for card data first
            val request = JSONObject().apply {
                put("command", "read_card")
                put("timestamp", System.currentTimeMillis())
            }
            
            writer?.println(request.toString())
            writer?.flush()
            Log.d(TAG, "Sent card read request to bridge")
            
            // Read the "ready" response
            val readyResponse = reader?.readLine()
            if (readyResponse != null) {
                val readyData = JSONObject(readyResponse)
                Log.d(TAG, "Bridge ready response: $readyData")
                
                // Check if there's an error in the ready response
                if (readyData.has("error")) {
                    Log.e(TAG, "Bridge error: ${readyData.getString("error")}")
                    return@withContext null
                }
            }
            
            // Now wait for the actual card data
            val cardResponse = reader?.readLine()
            if (cardResponse != null) {
                val cardData = JSONObject(cardResponse)
                Log.d(TAG, "Received card data: $cardData")
                
                // Check if this is an error response
                if (cardData.has("error")) {
                    Log.e(TAG, "Bridge error: ${cardData.getString("error")}")
                    return@withContext null
                }
                
                // Valid card data received
                lastCardData = cardData
                return@withContext cardData
            } else {
                Log.e(TAG, "No card data received from bridge server")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading card data from bridge", e)
            return@withContext null
        }
    }
    
    /**
     * Send character data to the bridge server (for writing to cards)
     */
    suspend fun sendCharacterData(nfcCharacter: NfcCharacter): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            Log.e(TAG, "Not connected to USB bridge server")
            return@withContext false
        }
        
        try {
            // Convert NfcCharacter to JSON for transmission
            val characterData = JSONObject().apply {
                put("command", "write_character")
                put("dimId", nfcCharacter.dimId)
                put("charIndex", nfcCharacter.charIndex)
                put("stage", nfcCharacter.stage)
                put("attribute", nfcCharacter.attribute.name)
                put("ageInDays", nfcCharacter.ageInDays)
                put("mood", nfcCharacter.mood)
                put("vitalPoints", nfcCharacter.vitalPoints)
                put("timestamp", System.currentTimeMillis())
            }
            
            writer?.println(characterData.toString())
            writer?.flush()
            
            // Read response
            val response = reader?.readLine()
            if (response != null) {
                val responseJson = JSONObject(response)
                val success = responseJson.optBoolean("success", false)
                Log.d(TAG, "Write response: $responseJson")
                return@withContext success
            } else {
                Log.e(TAG, "No response received for write operation")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending character data to bridge", e)
            return@withContext false
        }
    }
    
    /**
     * Prepare DIM card for character transfer
     */
    suspend fun prepareDIMForCharacter(dimId: UShort): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            Log.e(TAG, "Not connected to USB bridge server")
            return@withContext false
        }
        
        try {
            val dimData = JSONObject().apply {
                put("command", "prepare_dim")
                put("dimId", dimId.toInt())
                put("timestamp", System.currentTimeMillis())
            }
            
            writer?.println(dimData.toString())
            writer?.flush()
            
            // Read response
            val response = reader?.readLine()
            if (response != null) {
                val responseJson = JSONObject(response)
                val success = responseJson.optBoolean("success", false)
                Log.d(TAG, "DIM preparation response: $responseJson")
                return@withContext success
            } else {
                Log.e(TAG, "No response received for DIM preparation")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing DIM card", e)
            return@withContext false
        }
    }
    
    /**
     * Get the last received card data
     */
    fun getLastCardData(): JSONObject? = lastCardData
    
    /**
     * Parse card data from bridge to NfcCharacter format
     * This implementation converts the JSON data from the bridge to NfcCharacter format
     */
    fun parseCardDataToNfcCharacter(cardData: JSONObject): NfcCharacter {
        // Check if we have raw data from the bridge
        if (cardData.has("raw_data")) {
            Log.d(TAG, "Received raw data from bridge, attempting to parse it")
            
            // Extract the raw data array
            val rawDataArray = cardData.getJSONArray("raw_data")
            val rawData = ByteArray(rawDataArray.length()) { i ->
                rawDataArray.getInt(i).toByte()
            }
            
            Log.d(TAG, "Raw data size: ${rawData.size} bytes")
            Log.d(TAG, "Raw data (first 32 bytes): ${rawData.take(32).joinToString(", ") { "0x%02X".format(it) }}")
            
            // Try to parse the raw data using the same structure as TagCommunicator
            // The raw data should contain the character information in a specific format
            return parseRawDataToNfcCharacter(rawData, cardData)
        }
        
        // Extract data from the bridge response
        val uid = cardData.optString("uid", "")
        val type = cardData.optString("type", "VitalBracelet")
        
        // Extract character data
        val dimId = cardData.optInt("dimId", 1).toUShort()
        val charIndex = cardData.optInt("charIndex", 0).toUShort()
        val stage = cardData.optInt("stage", 0).toByte()
        val attributeStr = cardData.optString("attribute", "VACCINE")
        val ageInDays = cardData.optInt("ageInDays", 0).toByte()
        val nextAdventureMissionStage = cardData.optInt("nextAdventureMissionStage", 0).toByte()
        val mood = cardData.optInt("mood", 0).toByte()
        val vitalPoints = cardData.optInt("vitalPoints", 0).toUShort()
        val transformationCountdownInMinutes = cardData.optInt("transformationCountdownInMinutes", 0).toUShort()
        val injuryStatusStr = cardData.optString("injuryStatus", "NONE")
        val trophies = cardData.optInt("trophies", 0).toUShort()
        val currentPhaseBattlesWon = cardData.optInt("currentPhaseBattlesWon", 0).toUShort()
        val currentPhaseBattlesLost = cardData.optInt("currentPhaseBattlesLost", 0).toUShort()
        val totalBattlesWon = cardData.optInt("totalBattlesWon", 0).toUShort()
        val totalBattlesLost = cardData.optInt("totalBattlesLost", 0).toUShort()
        val activityLevel = cardData.optInt("activityLevel", 0).toByte()
        val heartRateCurrent = cardData.optInt("heartRateCurrent", 0).toUByte()
        val generation = cardData.optInt("generation", 0).toUShort()
        val totalTrophies = cardData.optInt("totalTrophies", 0).toUShort()
        
        // Parse attribute using entries array
        Log.d(TAG, "Parsing attribute: '$attributeStr' -> '${attributeStr.uppercase()}'")
        
        // Debug: Log all available enum entries
        Log.d(TAG, "Available Attribute enum entries:")
        com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries.forEachIndexed { index, entry ->
            Log.d(TAG, "  [$index] ${entry.name}")
        }
        
        val attribute = when (attributeStr.uppercase()) {
            "VACCINE" -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[3]
            "VIRUS" -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[1]
            "DATA" -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[2]
            "FREE" -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[4]
            else -> {
                Log.w(TAG, "Unknown attribute '$attributeStr', defaulting to VACCINE (index 3)")
                com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[3]
            }
        }
        Log.d(TAG, "Parsed attribute: $attribute (name: ${attribute.name})")
        
        // Parse injury status using entries array
        Log.d(TAG, "Parsing injury status: '$injuryStatusStr' -> '${injuryStatusStr.uppercase()}'")
        
        // Debug: Log all available enum entries
        Log.d(TAG, "Available InjuryStatus enum entries:")
        com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries.forEachIndexed { index, entry ->
            Log.d(TAG, "  [$index] ${entry.name}")
        }
        
        val injuryStatus = when (injuryStatusStr.uppercase()) {
            "NONE" -> com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[0]
            "INJURED" -> com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[1]
            "INJURY" -> com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[1]
            "SICK" -> com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[1] // Assuming sick maps to injury
            else -> {
                Log.w(TAG, "Unknown injury status '$injuryStatusStr', defaulting to NONE (index 0)")
                com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[0]
            }
        }
        Log.d(TAG, "Parsed injury status: $injuryStatus (name: ${injuryStatus.name})")
        
        // Parse transformation history
        val transformationHistoryArray = cardData.optJSONArray("transformationHistory")
        val transformationHistory = Array(8) { index ->
            if (transformationHistoryArray != null && index < transformationHistoryArray.length()) {
                val transform = transformationHistoryArray.getJSONObject(index)
                com.github.cfogrady.vbnfc.data.NfcCharacter.Transformation(
                    toCharIndex = transform.optInt("toCharIndex", 255).toUByte(),
                    year = transform.optInt("year", 65535).toUShort(),
                    month = transform.optInt("month", 255).toUByte(),
                    day = transform.optInt("day", 255).toUByte()
                )
            } else {
                com.github.cfogrady.vbnfc.data.NfcCharacter.Transformation(
                    toCharIndex = 255u.toUByte(),
                    year = 65535u,
                    month = 255u.toUByte(),
                    day = 255u.toUByte()
                )
            }
        }
        
        // Parse vital history
        val vitalHistoryArray = cardData.optJSONArray("vitalHistory")
        val vitalHistory = Array(7) { index ->
            if (vitalHistoryArray != null && index < vitalHistoryArray.length()) {
                val vital = vitalHistoryArray.getJSONObject(index)
                com.github.cfogrady.vbnfc.data.NfcCharacter.DailyVitals(
                    day = vital.optInt("day", 0).toUByte(),
                    month = vital.optInt("month", 0).toUByte(),
                    year = vital.optInt("year", 0).toUShort(),
                    vitalsGained = vital.optInt("vitalsGained", 0).toUShort()
                )
            } else {
                com.github.cfogrady.vbnfc.data.NfcCharacter.DailyVitals(
                    day = 0u.toUByte(),
                    month = 0u.toUByte(),
                    year = 0u,
                    vitalsGained = 0u
                )
            }
        }
        
        // Parse special missions
        val specialMissionsArray = cardData.optJSONArray("specialMissions")
        val specialMissions = if (specialMissionsArray != null) {
            Array(specialMissionsArray.length()) { index ->
                val mission = specialMissionsArray.getJSONObject(index)
                com.github.cfogrady.vbnfc.vb.SpecialMission(
                    goal = mission.optInt("goal", 0).toUShort(),
                    id = mission.optInt("id", 0).toUShort(),
                    progress = mission.optInt("progress", 0).toUShort(),
                    status = com.github.cfogrady.vbnfc.vb.SpecialMission.Status.entries[0], // Default to first status
                    timeElapsedInMinutes = mission.optInt("timeElapsedInMinutes", 0).toUShort(),
                    timeLimitInMinutes = mission.optInt("timeLimitInMinutes", 0).toUShort(),
                    type = com.github.cfogrady.vbnfc.vb.SpecialMission.Type.entries[0] // Default to first type
                )
            }
        } else {
            emptyArray()
        }
        
        Log.d(TAG, "Parsed character data: DIM=$dimId, Char=$charIndex, Stage=$stage, Attribute=$attribute (raw: '$attributeStr')")
        
        // Create VBNfcCharacter with parsed data
        return com.github.cfogrady.vbnfc.vb.VBNfcCharacter(
            dimId = dimId,
            charIndex = charIndex,
            stage = stage,
            attribute = attribute,
            ageInDays = ageInDays,
            nextAdventureMissionStage = nextAdventureMissionStage,
            mood = mood,
            vitalPoints = vitalPoints,
            transformationCountdownInMinutes = transformationCountdownInMinutes,
            injuryStatus = injuryStatus,
            trophies = trophies,
            currentPhaseBattlesWon = currentPhaseBattlesWon,
            currentPhaseBattlesLost = currentPhaseBattlesLost,
            totalBattlesWon = totalBattlesWon,
            totalBattlesLost = totalBattlesLost,
            activityLevel = activityLevel,
            heartRateCurrent = heartRateCurrent,
            transformationHistory = transformationHistory,
            vitalHistory = vitalHistory,
            appReserved1 = ByteArray(12) { 0 },
            appReserved2 = Array(3) { 0u },
            generation = generation,
            totalTrophies = totalTrophies,
            specialMissions = specialMissions
        )
    }
    
    /**
     * Parse raw data from bridge to NfcCharacter format
     * This attempts to extract character data from the raw bytes read from the card
     */
    private fun parseRawDataToNfcCharacter(rawData: ByteArray, cardData: JSONObject): NfcCharacter {
        Log.d(TAG, "Parsing raw data to NfcCharacter")
        
        // The raw data should contain the character information in a specific format
        // Based on the VBNfcCharacter structure, we need to extract various fields
        
        // Extract basic character data from the raw bytes
        // The exact byte positions depend on the Vital Bracelet data format
        val dimId = if (rawData.size > 0) rawData[0].toUByte().toUShort() else 12u
        val charIndex = if (rawData.size > 1) rawData[1].toUByte().toUShort() else 3u
        val stage = if (rawData.size > 2) rawData[2] else 0
        val ageInDays = if (rawData.size > 3) rawData[3] else 5
        
        // Extract attribute from raw data
        val attributeByte = if (rawData.size > 4) rawData[4] else 0
        val attribute = when (attributeByte.toInt()) {
            0 -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[3] // VACCINE
            1 -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[1] // VIRUS
            2 -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[2] // DATA
            3 -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[4] // FREE
            else -> com.github.cfogrady.vbnfc.data.NfcCharacter.Attribute.entries[3] // Default to VACCINE
        }
        
        // Extract other fields from raw data
        val mood = if (rawData.size > 5) rawData[5] else 50
        val vitalPoints = if (rawData.size > 7) {
            ((rawData[6].toInt() and 0xFF) shl 8) or (rawData[7].toInt() and 0xFF)
        } else 2500
        val transformationCountdownInMinutes = if (rawData.size > 9) {
            ((rawData[8].toInt() and 0xFF) shl 8) or (rawData[9].toInt() and 0xFF)
        } else 1440
        val injuryStatus = com.github.cfogrady.vbnfc.data.NfcCharacter.InjuryStatus.entries[0] // NONE
        val trophies = if (rawData.size > 10) rawData[10].toUByte().toUShort() else 3u
        val currentPhaseBattlesWon = if (rawData.size > 11) rawData[11].toUByte().toUShort() else 2u
        val currentPhaseBattlesLost = if (rawData.size > 12) rawData[12].toUByte().toUShort() else 1u
        val totalBattlesWon = if (rawData.size > 13) rawData[13].toUByte().toUShort() else 5u
        val totalBattlesLost = if (rawData.size > 14) rawData[14].toUByte().toUShort() else 2u
        val activityLevel = if (rawData.size > 15) rawData[15] else 75
        val heartRateCurrent = if (rawData.size > 16) rawData[16].toUByte() else 80u
        val generation = if (rawData.size > 17) rawData[17].toUByte().toUShort() else 1u
        val totalTrophies = if (rawData.size > 18) rawData[18].toUByte().toUShort() else 3u
        val nextAdventureMissionStage = if (rawData.size > 19) rawData[19] else 1
        
        Log.d(TAG, "Parsed from raw data: DIM=$dimId, Char=$charIndex, Stage=$stage, Attribute=$attribute")
        Log.d(TAG, "Attribute byte: $attributeByte -> ${attribute.name}")
        
        // Create default arrays for history data
        val transformationHistory = Array(8) {
            com.github.cfogrady.vbnfc.data.NfcCharacter.Transformation(
                toCharIndex = 255u.toUByte(),
                year = 65535u,
                month = 255u.toUByte(),
                day = 255u.toUByte()
            )
        }
        
        val vitalHistory = Array(7) {
            com.github.cfogrady.vbnfc.data.NfcCharacter.DailyVitals(
                day = 0u.toUByte(),
                month = 0u.toUByte(),
                year = 0u,
                vitalsGained = 0u
            )
        }
        
        val specialMissions = emptyArray<com.github.cfogrady.vbnfc.vb.SpecialMission>()
        
        // Create VBNfcCharacter with parsed data
        return com.github.cfogrady.vbnfc.vb.VBNfcCharacter(
            dimId = dimId,
            charIndex = charIndex,
            stage = stage,
            attribute = attribute,
            ageInDays = ageInDays,
            nextAdventureMissionStage = nextAdventureMissionStage,
            mood = mood,
            vitalPoints = vitalPoints.toUShort(),
            transformationCountdownInMinutes = transformationCountdownInMinutes.toUShort(),
            injuryStatus = injuryStatus,
            trophies = trophies,
            currentPhaseBattlesWon = currentPhaseBattlesWon,
            currentPhaseBattlesLost = currentPhaseBattlesLost,
            totalBattlesWon = totalBattlesWon,
            totalBattlesLost = totalBattlesLost,
            activityLevel = activityLevel,
            heartRateCurrent = heartRateCurrent,
            transformationHistory = transformationHistory,
            vitalHistory = vitalHistory,
            appReserved1 = ByteArray(12) { 0 },
            appReserved2 = Array(3) { 0u },
            generation = generation,
            totalTrophies = totalTrophies,
            specialMissions = specialMissions
        )
    }
}
