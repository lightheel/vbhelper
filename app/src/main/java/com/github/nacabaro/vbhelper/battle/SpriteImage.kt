package com.github.nacabaro.vbhelper.battle

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.github.nacabaro.vbhelper.battle.BattleSpriteManager

@Composable
fun SpriteImage(
    spriteName: String,
    atlasName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val spriteManager = remember { BattleSpriteManager(context) }
    
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(spriteName, atlasName) {
        println("Loading sprite: $spriteName from atlas: $atlasName")
        bitmap = spriteManager.loadSprite(spriteName, atlasName)
        if (bitmap == null) {
            println("Failed to load sprite: $spriteName from atlas: $atlasName")
        } else {
            println("Successfully loaded sprite: $spriteName from atlas: $atlasName")
        }
    }
    
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Sprite: $spriteName",
            modifier = modifier,
            contentScale = contentScale
        )
    }
}