package com.github.nacabaro.vbhelper.screens.scanScreen

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.github.cfogrady.vbnfc.TagCommunicator
import com.github.cfogrady.vbnfc.data.NfcCharacter
import com.github.nacabaro.vbhelper.ActivityLifecycleListener
import com.github.nacabaro.vbhelper.screens.scanScreen.converters.FromNfcConverter
import com.github.nacabaro.vbhelper.screens.scanScreen.converters.ToNfcConverter
import com.github.nacabaro.vbhelper.source.getCryptographicTransformerMap
import com.github.nacabaro.vbhelper.source.isMissingSecrets
import com.github.nacabaro.vbhelper.source.proto.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.github.nacabaro.vbhelper.nfc.Acr122uReader
import com.github.nacabaro.vbhelper.nfc.UsbBridgeReader
import org.json.JSONObject
import org.json.JSONArray

class ScanScreenControllerImpl(
    override val secretsFlow: Flow<Secrets>,
    private val componentActivity: ComponentActivity,
    private val registerActivityLifecycleListener: (String, ActivityLifecycleListener)->Unit,
    private val unregisterActivityLifecycleListener: (String)->Unit,
): ScanScreenController {

    private val nfcAdapter: NfcAdapter?
    private val acr122uReader: Acr122uReader
    private val usbBridgeReader: UsbBridgeReader

    init {
        val maybeNfcAdapter = NfcAdapter.getDefaultAdapter(componentActivity)
        if (maybeNfcAdapter == null) {
            Toast.makeText(componentActivity, "No NFC on device!", Toast.LENGTH_SHORT).show()
        }
        nfcAdapter = maybeNfcAdapter
        
        // Initialize ACR122U reader
        acr122uReader = Acr122uReader(componentActivity)
        acr122uReader.registerReceiver()
        val acr122uDevice = acr122uReader.findAcr122uDevice()
        if (acr122uDevice != null) {
            Toast.makeText(componentActivity, "ACR122U NFC reader detected!", Toast.LENGTH_SHORT).show()
            acr122uReader.requestPermission(acr122uDevice)
        }
        
        // Initialize USB Bridge reader
        usbBridgeReader = UsbBridgeReader(componentActivity)
        // Try to connect to the bridge server
        componentActivity.lifecycleScope.launch {
            if (usbBridgeReader.connect()) {
                Toast.makeText(componentActivity, "USB Bridge connected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(componentActivity, "USB Bridge not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        checkSecrets()
    }

    override fun onClickRead(secrets: Secrets, onComplete: ()->Unit) {
        handleTag(secrets) { tagCommunicator ->
            val character = tagCommunicator.receiveCharacter()
            val resultMessage = characterFromNfc(character)
            onComplete.invoke()
            resultMessage
        }
    }

    override fun cancelRead() {
        if(nfcAdapter?.isEnabled == true) {
            nfcAdapter.disableReaderMode(componentActivity)
        }
    }

    override fun isAcr122uConnected(): Boolean {
        return acr122uReader.isConnected() || usbBridgeReader.isConnected()
    }
    
    override fun isBuiltInNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    fun cleanup() {
        acr122uReader.unregisterReceiver()
        acr122uReader.disconnect()
        usbBridgeReader.disconnect()
    }

    override fun registerActivityLifecycleListener(
        key: String,
        activityLifecycleListener: ActivityLifecycleListener
    ) {
        registerActivityLifecycleListener.invoke(key, activityLifecycleListener)
    }

    override fun unregisterActivityLifecycleListener(key: String) {
        unregisterActivityLifecycleListener.invoke(key)
    }

    // EXTRACTED DIRECTLY FROM EXAMPLE APP
    private fun handleTag(secrets: Secrets, handlerFunc: (TagCommunicator)->String) {
        if (usbBridgeReader.isConnected()) {
            // Prioritize USB Bridge if connected
            handleUsbBridgeTag(secrets, handlerFunc)
        } else if (acr122uReader.isConnected()) {
            // Fallback to physical ACR122U if connected
            handleAcr122uTag(secrets, handlerFunc)
        } else if (nfcAdapter?.isEnabled == true) {
            // Fallback to built-in NFC
            val options = Bundle()
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            nfcAdapter.enableReaderMode(componentActivity, buildOnReadTag(secrets, handlerFunc), NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
            )
        } else {
            showWirelessSettings()
        }
    }

    private fun handleAcr122uTag(secrets: Secrets, handlerFunc: (TagCommunicator)->String) {
        componentActivity.lifecycleScope.launch {
            try {
                if (acr122uReader.powerOnCard()) {
                    val uid = acr122uReader.getCardUid()
                    if (uid != null) {
                        val cardData = acr122uReader.readVitalBraceletData()
                        if (cardData != null) {
                            componentActivity.runOnUiThread {
                                Toast.makeText(componentActivity, "Card read successfully via ACR122U!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            componentActivity.runOnUiThread {
                                Toast.makeText(componentActivity, "Failed to read card data via ACR122U", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        componentActivity.runOnUiThread {
                            Toast.makeText(componentActivity, "Failed to get card UID via ACR122U", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    componentActivity.runOnUiThread {
                        Toast.makeText(componentActivity, "Failed to power on card via ACR122U", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                componentActivity.runOnUiThread {
                    Toast.makeText(componentActivity, "ACR122U error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleUsbBridgeTag(secrets: Secrets, handlerFunc: (TagCommunicator)->String) {
        componentActivity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Read card data directly from the USB bridge
                val cardData = usbBridgeReader.readCardData()
                if (cardData != null) {
                    // Parse the card data to NfcCharacter format
                    val nfcCharacter = usbBridgeReader.parseCardDataToNfcCharacter(cardData)
                    
                    // Process the character data using the same pattern as the working NFC implementation
                    // Call characterFromNfc on the IO dispatcher to avoid main thread database access
                    val resultMessage = characterFromNfc(nfcCharacter)
                    
                    componentActivity.runOnUiThread {
                        Toast.makeText(componentActivity, resultMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    componentActivity.runOnUiThread {
                        Toast.makeText(componentActivity, "No card detected on ACR122U reader. Please place a Vital Bracelet card on the reader.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                componentActivity.runOnUiThread {
                    Toast.makeText(componentActivity, "USB Bridge error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("ScanScreenController", "Error handling USB bridge tag", e)
            }
        }
    }
    


    // EXTRACTED DIRECTLY FROM EXAMPLE APP
    private fun buildOnReadTag(secrets: Secrets, handlerFunc: (TagCommunicator)->String): (Tag)->Unit {
        return { tag->
            val nfcData = NfcA.get(tag)
            if (nfcData == null) {
                componentActivity.runOnUiThread {
                    Toast.makeText(componentActivity, "Tag detected is not VB", Toast.LENGTH_SHORT).show()
                }
            }
            nfcData.connect()
            nfcData.use {
                val tagCommunicator = TagCommunicator.getInstance(nfcData, secrets.getCryptographicTransformerMap())
                val successText = handlerFunc(tagCommunicator)
                componentActivity.runOnUiThread {
                    Toast.makeText(componentActivity, successText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkSecrets() {
        componentActivity.lifecycleScope.launch(Dispatchers.IO) {
            if(secretsFlow.stateIn(componentActivity.lifecycleScope).value.isMissingSecrets()) {
                componentActivity.runOnUiThread {
                    Toast.makeText(componentActivity, "Missing Secrets. Go to settings and import Vital Arena APK", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onClickWrite(
        secrets: Secrets,
        nfcCharacter: NfcCharacter,
        onComplete: () -> Unit
    ) {
        handleTag(secrets) { tagCommunicator ->
            try {
                tagCommunicator.sendCharacter(nfcCharacter)
                onComplete.invoke()
                "Sent character successfully!"
            } catch (e: Throwable) {
                Log.e("TAG", e.stackTraceToString())
                "Whoops"
            }
        }
    }

    override fun onClickCheckCard(
        secrets: Secrets,
        nfcCharacter: NfcCharacter,
        onComplete: () -> Unit
    ) {
        handleTag(secrets) { tagCommunicator ->
            tagCommunicator.prepareDIMForCharacter(nfcCharacter.dimId)
            onComplete.invoke()
            "Sent DIM successfully!"
        }
    }

    // EXTRACTED DIRECTLY FROM EXAMPLE APP
    private fun showWirelessSettings() {
        Toast.makeText(componentActivity, "NFC must be enabled", Toast.LENGTH_SHORT).show()
        componentActivity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    override fun characterFromNfc(nfcCharacter: NfcCharacter): String {
        val nfcConverter = FromNfcConverter(
            componentActivity = componentActivity
        )
        return nfcConverter.addCharacter(nfcCharacter)
    }

    override suspend fun characterToNfc(characterId: Long): NfcCharacter {
        val nfcGenerator = ToNfcConverter(
            componentActivity = componentActivity
        )
        return nfcGenerator.characterToNfc(characterId)
    }
}