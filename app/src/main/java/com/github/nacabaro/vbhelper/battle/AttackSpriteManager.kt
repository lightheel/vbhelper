package com.github.nacabaro.vbhelper.battle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import java.io.File

data class CharacterData(
    val name: String,
    val charaId: String,
    val smalefilename: String,
    val laugeFileName: String
)

class AttackSpriteManager(private val context: Context) {
    private val gson = Gson()
    private val characterDataCache = mutableMapOf<String, CharacterData>()
    
    // Base path for attack textures
    private val attackTexturesPath = "Battle_Sprites_Reference/extracted_assets/atk_textures"
    
    fun getAttackSprite(characterId: String, isLarge: Boolean = false): Bitmap? {
        try {
            // Get character data
            val characterData = getCharacterData(characterId) ?: return null
            
            // Determine which attack file to use
            val attackFileName = if (isLarge) {
                characterData.laugeFileName
            } else {
                characterData.smalefilename
            }
            
            // Skip if no attack file
            if (attackFileName == "0") return null
            
            // Load the attack sprite
            val attackFilePath = "$attackTexturesPath/$attackFileName.png"
            val attackFile = File(context.filesDir, attackFilePath)
            
            return if (attackFile.exists()) {
                BitmapFactory.decodeFile(attackFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun getCharacterData(characterId: String): CharacterData? {
        // Check cache first
        if (characterDataCache.containsKey(characterId)) {
            return characterDataCache[characterId]
        }
        
        try {
            // Load character data from JSON file
            val characterDataFile = File(context.filesDir, "Battle_Sprites_Reference/extracted_digimon_stats/character_data/CharacterData.json")
            
            if (!characterDataFile.exists()) {
                return null
            }
            
            val jsonContent = characterDataFile.readText()
            // Parse the JSON and find the character with matching charaId
            // This is a simplified version - you'll need to parse the actual JSON structure
            
            // For now, return a default character data
            val characterData = CharacterData(
                name = characterId,
                charaId = characterId,
                smalefilename = "atk_s_02", // Default small attack
                laugeFileName = "atk_l_04"   // Default large attack
            )
            
            characterDataCache[characterId] = characterData
            return characterData
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
} 