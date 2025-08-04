package com.github.nacabaro.vbhelper.battle

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

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
    private val characterId: String
) {
    var currentAnimation by mutableStateOf<DigimonAnimationType>(DigimonAnimationType.IDLE)
        private set
    
    var currentSpriteIndex by mutableStateOf(0)
        private set
    
    var isPlaying by mutableStateOf(false)
        private set
    
    // Animation mapping - maps animation types to sprite indices
    // For now, we'll assume the sprite indices 0-11 correspond to the 12 animation types
    private val animationMapping = mapOf(
        DigimonAnimationType.IDLE to 0,
        DigimonAnimationType.IDLE2 to 1,
        DigimonAnimationType.WALK to 2,
        DigimonAnimationType.WALK2 to 3,
        DigimonAnimationType.RUN to 4,
        DigimonAnimationType.RUN2 to 5,
        DigimonAnimationType.WORKOUT to 6,
        DigimonAnimationType.WORKOUT2 to 7,
        DigimonAnimationType.HAPPY to 8,
        DigimonAnimationType.SLEEP to 9,
        DigimonAnimationType.ATTACK to 10,
        DigimonAnimationType.FLEE to 11
    )
    
    // Animation frame sequences - defines which frames to cycle through for each animation
    private val animationFrameSequences = mapOf(
        DigimonAnimationType.IDLE to listOf(0, 1), // Cycle between idle frames
        DigimonAnimationType.IDLE2 to listOf(1, 0), // Alternative idle cycle
        DigimonAnimationType.WALK to listOf(2, 3), // Walk animation frames
        DigimonAnimationType.WALK2 to listOf(3, 2), // Alternative walk cycle
        DigimonAnimationType.RUN to listOf(4, 5), // Run animation frames
        DigimonAnimationType.RUN2 to listOf(5, 4), // Alternative run cycle
        DigimonAnimationType.WORKOUT to listOf(6, 7), // Workout animation frames
        DigimonAnimationType.WORKOUT2 to listOf(7, 6), // Alternative workout cycle
        DigimonAnimationType.HAPPY to listOf(8), // Single happy frame
        DigimonAnimationType.SLEEP to listOf(9), // Single sleep frame
        DigimonAnimationType.ATTACK to listOf(10), // Single attack frame
        DigimonAnimationType.FLEE to listOf(11) // Single flee frame
    )
    
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
        DigimonAnimationType.ATTACK to 300L, // Longer for attack animation
        DigimonAnimationType.FLEE to 150L
    )
    
    suspend fun playAnimation(animationType: DigimonAnimationType) {
        if (currentAnimation == animationType && isPlaying) {
            return // Already playing this animation
        }
        
        currentAnimation = animationType
        isPlaying = true
        
        val frameSequence = animationFrameSequences[animationType] ?: listOf(0)
        val duration = animationDurations[animationType] ?: 100L
        
        // For non-looping animations like ATTACK, play once and return to IDLE
        if (animationType == DigimonAnimationType.ATTACK) {
            currentSpriteIndex = frameSequence[0]
            delay(duration)
            playAnimation(DigimonAnimationType.IDLE)
        } else {
            // For looping animations, cycle through frames
            var frameIndex = 0
            while (isPlaying && currentAnimation == animationType) {
                currentSpriteIndex = frameSequence[frameIndex % frameSequence.size]
                delay(duration)
                frameIndex++
            }
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