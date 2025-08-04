package com.github.nacabaro.vbhelper.battle

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun AnimatedSpriteImage(
    characterId: String,
    animationType: DigimonAnimationType = DigimonAnimationType.IDLE,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val spriteManager = remember { BattleSpriteManager(context) }
    val animationStateMachine = remember { DigimonAnimationStateMachine(characterId, context) }
    val coroutineScope = rememberCoroutineScope()
    
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Start the animation when the component is first created
    LaunchedEffect(characterId) {
        coroutineScope.launch {
            animationStateMachine.playIdleAnimation()
        }
    }
    
    // Change animation when animationType changes
    LaunchedEffect(animationType) {
        coroutineScope.launch {
            if (animationType == DigimonAnimationType.IDLE) {
                animationStateMachine.playIdleAnimation()
            } else {
                animationStateMachine.playAnimation(animationType)
            }
        }
    }
    
    // Update sprite when animation state changes
    LaunchedEffect(animationStateMachine.currentSpriteIndex) {
        val spriteName = animationStateMachine.getCurrentSpriteName()
        val atlasName = animationStateMachine.getCurrentAtlasName()
        
        println("Loading animated sprite: $spriteName from atlas: $atlasName")
        bitmap = spriteManager.loadSprite(spriteName, atlasName)
        
        if (bitmap == null) {
            println("Failed to load animated sprite: $spriteName from atlas: $atlasName")
        } else {
            println("Successfully loaded animated sprite: $spriteName from atlas: $atlasName")
        }
    }
    
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Animated Sprite: $characterId - ${animationStateMachine.currentAnimation}",
            modifier = modifier,
            contentScale = contentScale
        )
    }
} 