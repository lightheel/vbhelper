package com.github.nacabaro.vbhelper.battle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import android.content.Context
import java.io.File
import com.google.gson.Gson

enum class DigimonAnimationType {
    IDLE,
    IDLE2,
    WALK,
    WALK2,
    RUN,
    RUN2,
    WORKOUT,
    WORKOUT2,
    HAPPY,
    SLEEP,
    ATTACK,
    FLEE
}

data class AnimationState(
    val type: DigimonAnimationType,
    val spriteIndex: Int, // 00, 01, 02, etc.
    val duration: Long = 100L, // Duration in milliseconds
    val loop: Boolean = true
)

class DigimonAnimationStateMachine(
    private val characterId: String,
    private val context: Context
) {
    var currentAnimation by mutableStateOf<DigimonAnimationType>(DigimonAnimationType.IDLE)
        private set
    
    var currentSpriteIndex by mutableStateOf(0)
        private set
    
    var isPlaying by mutableStateOf(false)
        private set
    
    // Animation mapping based on m_Name values
    private val mNameToAnimationType = mapOf(
        "01" to DigimonAnimationType.IDLE,
        "02" to DigimonAnimationType.IDLE2,
        "03" to DigimonAnimationType.WALK,
        "04" to DigimonAnimationType.WALK2,
        "05" to DigimonAnimationType.RUN,
        "06" to DigimonAnimationType.RUN2,
        "07" to DigimonAnimationType.WORKOUT,
        "08" to DigimonAnimationType.WORKOUT2,
        "09" to DigimonAnimationType.HAPPY,
        "10" to DigimonAnimationType.SLEEP,
        "11" to DigimonAnimationType.ATTACK,
        "12" to DigimonAnimationType.FLEE
    )
    
    // Cache for sprite file mappings
    private var spriteFileMappings: Map<DigimonAnimationType, List<String>> = emptyMap()
    
    // Animation durations for each type
    private val animationDurations = mapOf(
        DigimonAnimationType.IDLE to 500L,
        DigimonAnimationType.IDLE2 to 500L,
        DigimonAnimationType.WALK to 200L,
        DigimonAnimationType.WALK2 to 200L,
        DigimonAnimationType.RUN to 150L,
        DigimonAnimationType.RUN2 to 150L,
        DigimonAnimationType.WORKOUT to 300L,
        DigimonAnimationType.WORKOUT2 to 300L,
        DigimonAnimationType.HAPPY to 400L,
        DigimonAnimationType.SLEEP to 1000L,
        DigimonAnimationType.ATTACK to 300L,
        DigimonAnimationType.FLEE to 150L
    )
    
    init {
        loadSpriteFileMappings()
    }
    
    private fun loadSpriteFileMappings() {
        try {
            val spriteBaseDir = File(context.filesDir, "battle_sprites/extracted_assets/sprites")
            val gson = Gson()
            
            val mappings = mutableMapOf<DigimonAnimationType, MutableList<String>>()
            
            // Initialize all animation types
            DigimonAnimationType.values().forEach { animationType ->
                mappings[animationType] = mutableListOf()
            }
            
            println("Loading sprite mappings for character: $characterId")
            
            // Scan all sprite files for this character
            val spriteFiles = spriteBaseDir.listFiles { file ->
                file.name.startsWith("${characterId}_sprite_") && file.name.endsWith(".json")
            }
            
            println("Found ${spriteFiles?.size ?: 0} sprite files for $characterId")
            
            spriteFiles?.forEach { spriteFile ->
                println("Processing sprite file: ${spriteFile.name}")
                val spriteDataJson = spriteFile.readText()
                val spriteData = gson.fromJson(spriteDataJson, SpriteData::class.java)
                
                println("  m_Name: ${spriteData.m_Name}")
                
                // Get the animation type from m_Name
                val animationType = mNameToAnimationType[spriteData.m_Name]
                if (animationType != null) {
                    // Extract the sprite index from filename (e.g., "dim000_mon01_sprite_00.json" -> "00")
                    val spriteIndex = spriteFile.name.substringAfter("_sprite_").substringBefore(".json")
                    mappings[animationType]?.add(spriteIndex)
                    println("  Mapped to animation type: $animationType with sprite index: $spriteIndex")
                } else {
                    println("  Unknown m_Name: ${spriteData.m_Name}")
                }
            }
            
            // Convert to immutable map
            spriteFileMappings = mappings.mapValues { it.value.sorted() }
            
            println("Final sprite mappings for $characterId: $spriteFileMappings")
            
        } catch (e: Exception) {
            println("Error loading sprite file mappings: ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun playAnimation(animationType: DigimonAnimationType) {
        if (currentAnimation == animationType && isPlaying) {
            return // Already playing this animation
        }
        
        currentAnimation = animationType
        isPlaying = true
        
        val frameSequence = spriteFileMappings[animationType] ?: listOf("00")
        val duration = animationDurations[animationType] ?: 100L
        
        // Ensure we have at least one frame
        if (frameSequence.isEmpty()) {
            println("Warning: No sprite files found for animation type $animationType")
            currentSpriteIndex = 0
            return
        }
        
        // For non-looping animations like ATTACK, play once and return to IDLE
        if (animationType == DigimonAnimationType.ATTACK) {
            currentSpriteIndex = frameSequence.firstOrNull()?.toIntOrNull() ?: 0
            delay(duration)
            playAnimation(DigimonAnimationType.IDLE)
        } else {
            // For looping animations, cycle through frames
            var frameIndex = 0
            while (isPlaying && currentAnimation == animationType) {
                val spriteIndex = frameSequence[frameIndex % frameSequence.size]
                currentSpriteIndex = spriteIndex.toIntOrNull() ?: 0
                delay(duration)
                frameIndex++
            }
        }
    }
    
    // Special method for idle animation that cycles between IDLE and IDLE2
    suspend fun playIdleAnimation() {
        if (currentAnimation == DigimonAnimationType.IDLE && isPlaying) {
            return // Already playing idle animation
        }
        
        currentAnimation = DigimonAnimationType.IDLE
        isPlaying = true

        val idleFrames = spriteFileMappings[DigimonAnimationType.IDLE] ?: listOf("00")

        val idle2Frames = spriteFileMappings[DigimonAnimationType.HAPPY] ?: listOf("08")
        
        // Combine frames for cycling idle animation
        val combinedFrames = (idleFrames + idle2Frames).distinct()
        
        if (combinedFrames.isEmpty()) {
            println("Warning: No idle sprite files found")
            currentSpriteIndex = 0
            return
        }
        
        val duration = animationDurations[DigimonAnimationType.IDLE] ?: 500L
        
        // Cycle through idle frames
        var frameIndex = 0
        while (isPlaying && currentAnimation == DigimonAnimationType.IDLE) {
            val spriteIndex = combinedFrames[frameIndex % combinedFrames.size]
            currentSpriteIndex = spriteIndex.toIntOrNull() ?: 0
            delay(duration)
            frameIndex++
        }
    }
    
    fun stopAnimation() {
        isPlaying = false
    }
    
    fun getCurrentSpriteName(): String {
        return "${characterId}_sprite_${String.format("%02d", currentSpriteIndex)}"
    }
    
    fun getCurrentAtlasName(): String {
        return characterId
    }
} 