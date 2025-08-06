package com.github.nacabaro.vbhelper.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.github.nacabaro.vbhelper.battle.APIBattleCharacter
import android.util.Log
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.battle.RetrofitHelper
import com.github.nacabaro.vbhelper.battle.AttackSpriteImage
import com.github.nacabaro.vbhelper.battle.SpriteFileManager
import com.github.nacabaro.vbhelper.battle.ArenaBattleSystem
import com.github.nacabaro.vbhelper.battle.DigimonAnimationType
import com.github.nacabaro.vbhelper.battle.AnimatedSpriteImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun AnimatedDamageNumber(
    damage: Int,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    println("DEBUG: AnimatedDamageNumber called with damage=$damage, isVisible=$isVisible")
    
    var animationProgress by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var alpha by remember { mutableStateOf(1f) }
    var yOffset by remember { mutableStateOf(0.dp) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            println("DEBUG: Starting damage number animation for damage=$damage")
            // Start animation
            animationProgress = 0f
            scale = 0.5f
            alpha = 1f
            yOffset = 0.dp
            
            // Animate scale up
            while (scale < 1.5f) {
                scale += 0.1f
                delay(16)
            }
            
            // Hold at max scale briefly
            delay(200)
            
            // Animate fade out and move up
            while (alpha > 0f) {
                alpha -= 0.05f
                yOffset -= 1.dp
                delay(16)
            }
            println("DEBUG: Damage number animation completed for damage=$damage")
        }
    }
    
    Text(
        text = "-$damage",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Red,
        textAlign = TextAlign.Center,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black,
                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                blurRadius = 4f
            )
        ),
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .offset(y = yOffset)
    )
}

@Composable
fun BattleScreen(
    stage: String,
    playerName: String,
    opponentName: String,
    activeCharacter: APIBattleCharacter?,
    opponentCharacter: APIBattleCharacter?,
    onAttackClick: () -> Unit,
    context: android.content.Context? = null
) {
    val battleSystem = remember { ArenaBattleSystem() }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize HP when battle starts
    LaunchedEffect(activeCharacter, opponentCharacter) {
        val playerMaxHP = activeCharacter?.baseHp?.toFloat() ?: 100f
        val opponentMaxHP = opponentCharacter?.baseHp?.toFloat() ?: 100f
        battleSystem.initializeHP(playerMaxHP, opponentMaxHP)
    }
    
    // Pending damage state for API integration
    var pendingPlayerDamage by remember { mutableStateOf(0f) }
    var pendingOpponentDamage by remember { mutableStateOf(0f) }
    
    // Damage number animation state
    var showPlayerDamageNumber by remember { mutableStateOf(false) }
    var showOpponentDamageNumber by remember { mutableStateOf(false) }
    var playerDamageValue by remember { mutableStateOf(0) }
    var opponentDamageValue by remember { mutableStateOf(0) }
    
    // Critical bar timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            if (battleSystem.attackPhase == 0) { // Only update when not attacking
                battleSystem.updateCritBarProgress((battleSystem.critBarProgress + 5) % 101)
            }
        }
    }
    
    // Animation for attack phases
    LaunchedEffect(battleSystem.attackPhase) {
        when (battleSystem.attackPhase) {
            1 -> {
                // Phase 1: Both attacks from middle screen
                println("Starting Phase 1: Both attacks from middle screen")
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    delay(16) // 60 FPS
                }
                println("Phase 1 completed, advancing to Phase 2")
                battleSystem.advanceAttackPhase()
            }
            2 -> {
                // Phase 2: Player attack on enemy screen
                println("Starting Phase 2: Player attack on enemy screen")
                battleSystem.switchToView(2) // Enemy screen
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    
                    // Trigger animation when attack reaches the enemy (around 50% progress for enemy dodge)
                    if (progress >= 0.50f && !battleSystem.isOpponentHit && !battleSystem.isOpponentDodging) {
                        if (battleSystem.attackIsHit) {
                            // Player attack hits enemy
                            println("Player attack hits enemy at progress $progress")
                            battleSystem.startOpponentHit()
                            // Show damage number when attack reaches enemy
                            if (pendingOpponentDamage > 0) {
                                showOpponentDamageNumber = true
                                println("DEBUG: Showing opponent damage number at progress $progress")
                            }
                        } else {
                            // Player attack misses, enemy dodges
                            println("Player attack misses, enemy dodges at progress $progress")
                            battleSystem.startOpponentDodge()
                        }
                    }
                    
                    delay(16) // 60 FPS
                }
                println("Phase 2 completed, applying damage and starting Phase 3")
                battleSystem.completeAttackAnimation(opponentDamage = pendingOpponentDamage)
                
                // Hide damage number and reset pending damage after animation
                if (showOpponentDamageNumber) {
                    delay(800) // Wait for damage number animation (scale up + hold + fade out)
                    showOpponentDamageNumber = false
                    pendingOpponentDamage = 0f
                    println("DEBUG: Hiding opponent damage number and resetting pending damage")
                }
                
                delay(100)
                
                // Check if there should be a counter-attack
                if (battleSystem.shouldCounterAttack) {
                    println("Starting counter-attack from Phase 2")
                    battleSystem.startCounterAttack()
                } else {
                    println("No counter-attack, advancing to Phase 3")
                    battleSystem.advanceAttackPhase()
                }
            }
            3 -> {
                // Phase 3: Enemy attack on player screen
                println("Starting Phase 3: Enemy attack on player screen")
                battleSystem.switchToView(1) // Player screen
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    
                    // Trigger animation when attack reaches the player (around 50% progress for player dodge)
                    if (progress >= 0.50f && !battleSystem.isPlayerHit && !battleSystem.isPlayerDodging) {
                        println("Phase 3: Checking player animation at progress $progress, opponentAttackIsHit=${battleSystem.opponentAttackIsHit}")
                        println("Phase 3: Player animation decision - opponentAttackIsHit=${battleSystem.opponentAttackIsHit}, will ${if (battleSystem.opponentAttackIsHit) "HIT" else "DODGE"}")
                        if (battleSystem.opponentAttackIsHit) {
                            // Enemy attack hits player
                            println("Enemy attack hits player at progress $progress")
                            battleSystem.startPlayerHit()
                            // Show damage number when attack reaches player
                            if (pendingPlayerDamage > 0) {
                                showPlayerDamageNumber = true
                                println("DEBUG: Showing player damage number at progress $progress")
                            }
                        } else {
                            // Enemy attack misses, player dodges
                            println("Enemy attack misses, player dodges at progress $progress")
                            battleSystem.startPlayerDodge()
                        }
                    }
                    
                    delay(16) // 60 FPS
                }
                println("Phase 3 completed, applying damage and resetting")
                println("DEBUG: pendingPlayerDamage = $pendingPlayerDamage")
                battleSystem.completeAttackAnimation(playerDamage = pendingPlayerDamage)
                
                // Hide damage number and reset pending damage after animation
                if (showPlayerDamageNumber) {
                    delay(800) // Wait for damage number animation (scale up + hold + fade out)
                    showPlayerDamageNumber = false
                    pendingPlayerDamage = 0f
                    println("DEBUG: Hiding player damage number and resetting pending damage")
                }
                
                battleSystem.resetAttackState()
                battleSystem.enableAttackButton()
                
                // Check if battle is over
                if (battleSystem.checkBattleOver()) {
                    battleSystem.endBattle()
                    onAttackClick()
                }
            }
        }
    }

    // Player dodge animation
    LaunchedEffect(battleSystem.isPlayerDodging) {
        if (battleSystem.isPlayerDodging) {
            println("Starting player dodge animation")
            var dodgeProgress = 0f
            var dodgeDirection = 1f // Start moving up
            
            // Move up
            while (dodgeProgress < 1f) {
                dodgeProgress += 0.05f // Faster dodge movement
                battleSystem.setPlayerDodgeProgress(dodgeProgress)
                battleSystem.setPlayerDodgeDirection(dodgeDirection)
                delay(16) // 60 FPS
            }
            
            // Wait at the top
            delay(200)
            
            // Move back down
            dodgeDirection = -1f
            dodgeProgress = 0f
            while (dodgeProgress < 1f) {
                dodgeProgress += 0.05f
                battleSystem.setPlayerDodgeProgress(dodgeProgress)
                battleSystem.setPlayerDodgeDirection(dodgeDirection)
                delay(16)
            }
            
            battleSystem.endPlayerDodge()
            println("Player dodge animation completed")
        }
    }

    // Opponent dodge animation
    LaunchedEffect(battleSystem.isOpponentDodging) {
        if (battleSystem.isOpponentDodging) {
            println("Starting opponent dodge animation")
            var dodgeProgress = 0f
            var dodgeDirection = 1f // Start moving up
            
            // Move up
            while (dodgeProgress < 1f) {
                dodgeProgress += 0.05f // Faster dodge movement
                battleSystem.setOpponentDodgeProgress(dodgeProgress)
                battleSystem.setOpponentDodgeDirection(dodgeDirection)
                delay(16) // 60 FPS
            }
            
            // Wait at the top
            delay(200)
            
            // Move back down
            dodgeDirection = -1f
            dodgeProgress = 0f
            while (dodgeProgress < 1f) {
                dodgeProgress += 0.05f
                battleSystem.setOpponentDodgeProgress(dodgeProgress)
                battleSystem.setOpponentDodgeDirection(dodgeDirection)
                delay(16)
            }
            
            battleSystem.endOpponentDodge()
            println("Opponent dodge animation completed")
        }
    }

    // Player hit animation
    LaunchedEffect(battleSystem.isPlayerHit) {
        if (battleSystem.isPlayerHit) {
            println("Starting player hit animation")
            var hitProgress = 0f
            
            // Quick hit effect
            while (hitProgress < 1f) {
                hitProgress += 0.1f // Fast hit effect
                battleSystem.setHitProgress(hitProgress)
                delay(16)
            }
            
            delay(100) // Brief pause
            
            battleSystem.endPlayerHit()
            println("Player hit animation completed")
        }
    }

    // Opponent hit animation
    LaunchedEffect(battleSystem.isOpponentHit) {
        if (battleSystem.isOpponentHit) {
            println("Starting opponent hit animation")
            var hitProgress = 0f
            
            // Quick hit effect
            while (hitProgress < 1f) {
                hitProgress += 0.1f // Fast hit effect
                battleSystem.setHitProgress(hitProgress)
                delay(16)
            }
            
            delay(100) // Brief pause
            
            battleSystem.endOpponentHit()
            println("Opponent hit animation completed")
        }
    }

    // Damage number handling - store pending damage but don't show immediately
    LaunchedEffect(pendingPlayerDamage) {
        if (pendingPlayerDamage > 0) {
            println("DEBUG: LaunchedEffect triggered for pendingPlayerDamage = $pendingPlayerDamage")
            playerDamageValue = pendingPlayerDamage.toInt()
            // Don't show immediately - wait for attack animation to reach the Digimon
        }
    }

    LaunchedEffect(pendingOpponentDamage) {
        if (pendingOpponentDamage > 0) {
            println("DEBUG: LaunchedEffect triggered for pendingOpponentDamage = $pendingOpponentDamage")
            opponentDamageValue = pendingOpponentDamage.toInt()
            // Don't show immediately - wait for attack animation to reach the Digimon
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (battleSystem.currentView) {
            0 -> {
                // Middle screen - both Digimon
                MiddleBattleView(
                    battleSystem = battleSystem,
                    stage = stage,
                    playerName = playerName,
                    opponentName = opponentName,
                    attackAnimationProgress = battleSystem.attackProgress,
                    onAttackClick = {
                        battleSystem.startPlayerAttack()
                    },
                    activeCharacter = activeCharacter,
                    opponentCharacter = opponentCharacter,
                    context = context,
                    onSetPendingDamage = { playerDamage, opponentDamage ->
                        pendingPlayerDamage = playerDamage
                        pendingOpponentDamage = opponentDamage
                    },
                    coroutineScope = coroutineScope
                )
            }
            1 -> {
                // Player screen - enemy attack
                PlayerBattleView(
                    battleSystem = battleSystem,
                    stage = stage,
                    playerName = playerName,
                    attackAnimationProgress = battleSystem.attackProgress,
                    onAttackClick = {
                        battleSystem.startPlayerAttack()
                    },
                    activeCharacter = activeCharacter,
                    context = context,
                    opponent = opponentCharacter,
                    onSetPendingDamage = { playerDamage, opponentDamage ->
                        pendingPlayerDamage = playerDamage
                        pendingOpponentDamage = opponentDamage
                    },
                    coroutineScope = coroutineScope
                )
            }
            2 -> {
                // Enemy screen - player attack
                EnemyBattleView(
                    battleSystem = battleSystem,
                    stage = stage,
                    opponentName = opponentName,
                    attackAnimationProgress = battleSystem.attackProgress,
                    activeCharacter = opponentCharacter,
                    playerCharacter = activeCharacter
                )
            }
        }
        
        // Damage number overlays - moved inside the Box for proper positioning
        when (battleSystem.currentView) {
            0 -> {
                // Middle screen - NO damage numbers should show here
                // This screen is for the initial attack phase only
            }
            1 -> {
                // Player screen - show player damage (when opponent attacks player)
                println("DEBUG: Player screen damage overlay - playerDamageValue=$playerDamageValue, showPlayerDamageNumber=$showPlayerDamageNumber")
                AnimatedDamageNumber(
                    damage = playerDamageValue,
                    isVisible = showPlayerDamageNumber,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-50).dp)
                        //.background(Color.Yellow.copy(alpha = 0.3f)) // Debug background
                )
                
                // Debug text overlay
                /*
                Text(
                    text = "View: ${battleSystem.currentView}, Player Damage: $playerDamageValue, Show: $showPlayerDamageNumber",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 200.dp)
                        .background(Color.White.copy(alpha = 0.8f))
                )
                */
            }
            2 -> {
                // Enemy screen - show opponent damage (when player attacks opponent)
                println("DEBUG: Enemy screen damage overlay - opponentDamageValue=$opponentDamageValue, showOpponentDamageNumber=$showOpponentDamageNumber")
                AnimatedDamageNumber(
                    damage = opponentDamageValue,
                    isVisible = showOpponentDamageNumber,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-50).dp)
                )
            }
        }
    }
}

@Composable
fun MiddleBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    playerName: String,
    opponentName: String,
    attackAnimationProgress: Float,
    onAttackClick: () -> Unit,
    activeCharacter: APIBattleCharacter?,
    opponentCharacter: APIBattleCharacter?,
    context: android.content.Context?,
    onSetPendingDamage: (Float, Float) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Track previous character ID to detect transitions
    var previousCharacterId by remember { mutableStateOf<String?>(null) }
    var previousAttackPhase by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    var lastApiResult by remember { mutableStateOf<com.github.nacabaro.vbhelper.battle.PVPDataModel?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated background - positioned underneath all other sprites
        AnimatedBattleBackground(
            modifier = Modifier.fillMaxSize()
        )
        
        // Top section: Exit button, HP bars, and HP numbers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exit button at the top-right
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { /* TODO: Add exit functionality */ },
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Exit", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debug display
            /*
            if (lastApiResult != null) {
                Text(
                    text = "Debug: state=${lastApiResult!!.state}, playerAttackHit=${lastApiResult!!.playerAttackHit}, opponentDamage=${lastApiResult!!.opponentAttackDamage}",
                    color = Color.Red,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            */

            // Enemy HP bar (top)
            LinearProgressIndicator(
                progress = battleSystem.opponentHP / (opponentCharacter?.baseHp?.toFloat() ?: 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Red,
                trackColor = Color.Gray
            )

            // Enemy HP display numbers
            Text(
                text = "Enemy HP: ${battleSystem.opponentHP.toInt()}/${opponentCharacter?.baseHp ?: 100}",
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player HP display numbers
            /*
            Text(
                text = "HP: ${battleSystem.playerHP.toInt()}/${activeCharacter?.baseHp ?: 100}",
                fontSize = 14.sp,
                color = Color.Black
            )
            */
        }

        // Middle section: Both Digimon with horizontal line separator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Enemy Digimon (top half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Determine animation type for enemy
                    val enemyAnimationType = when {
                        battleSystem.attackPhase == 1 -> DigimonAnimationType.ATTACK  // Both attacking in Phase 1
                        battleSystem.isOpponentDodging -> DigimonAnimationType.WALK
                        battleSystem.isOpponentHit -> DigimonAnimationType.SLEEP
                        else -> DigimonAnimationType.IDLE
                    }
                    
                    // Calculate vertical offset for enemy dodge animation
                    val enemyVerticalOffset = if (battleSystem.isOpponentDodging) {
                        val dodgeHeight = 30.dp
                        val progress = battleSystem.opponentDodgeProgress
                        val direction = battleSystem.opponentDodgeDirection
                        
                        if (direction > 0) {
                            -(progress * dodgeHeight.value).dp
                        } else {
                            -((1f - progress) * dodgeHeight.value).dp
                        }
                    } else {
                        0.dp
                    }
                    
                    // Calculate hit effect for enemy
                    val enemyHitOffset = if (battleSystem.isOpponentHit) {
                        val shakeAmount = 5.dp
                        val progress = battleSystem.hitProgress
                        val shake = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                        (shake * shakeAmount.value).dp
                    } else {
                        0.dp
                    }
                    
                    AnimatedSpriteImage(
                        characterId = opponentCharacter?.charaId ?: "dim011_mon01",
                        animationType = enemyAnimationType,
                        modifier = Modifier
                            .size(80.dp)
                            .offset(
                                x = enemyHitOffset,
                                y = enemyVerticalOffset + 40.dp
                            ),
                        contentScale = ContentScale.Fit,
                        reloadMappings = false
                    )
                    
                    // Enemy attack sprite (Phase 1 only)
                    if (battleSystem.attackPhase == 1) {
                        val xOffset = (-attackAnimationProgress * 400).dp  // Start at center, move left off screen
                        val yOffset = 30.dp  // Lower enemy attack sprite by 30 pixels
                        
                        AttackSpriteImage(
                            characterId = opponentCharacter?.charaId ?: "dim011_mon01",
                            isLarge = true,
                            modifier = Modifier
                                .size(60.dp)
                                .offset(
                                    x = xOffset,
                                    y = yOffset
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // Horizontal line separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
                
                // Player Digimon (bottom half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Determine animation type for player
                    val playerAnimationType = when {
                        battleSystem.attackPhase == 1 -> DigimonAnimationType.ATTACK  // Both attacking in Phase 1
                        battleSystem.isPlayerDodging -> DigimonAnimationType.WALK
                        battleSystem.isPlayerHit -> DigimonAnimationType.SLEEP
                        else -> DigimonAnimationType.IDLE
                    }
                    
                    // Calculate vertical offset for player dodge animation
                    val playerVerticalOffset = if (battleSystem.isPlayerDodging) {
                        val dodgeHeight = 30.dp
                        val progress = battleSystem.playerDodgeProgress
                        val direction = battleSystem.playerDodgeDirection
                        
                        if (direction > 0) {
                            -(progress * dodgeHeight.value).dp
                        } else {
                            -((1f - progress) * dodgeHeight.value).dp
                        }
                    } else {
                        0.dp
                    }
                    
                    // Calculate hit effect for player
                    val playerHitOffset = if (battleSystem.isPlayerHit) {
                        val shakeAmount = 5.dp
                        val progress = battleSystem.hitProgress
                        val shake = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                        (shake * shakeAmount.value).dp
                    } else {
                        0.dp
                    }
                    
                    AnimatedSpriteImage(
                        characterId = activeCharacter?.charaId ?: "dim011_mon01",
                        animationType = playerAnimationType,
                        modifier = Modifier
                            .size(80.dp)
                            .scale(-1f, 1f) // Flip player Digimon horizontally
                            .offset(
                                x = playerHitOffset,
                                y = playerVerticalOffset - 40.dp
                            ),
                        contentScale = ContentScale.Fit,
                        reloadMappings = false
                    )
                    
                    // Player attack sprite (Phase 1 only)
                    if (battleSystem.attackPhase == 1) {
                        val xOffset = (attackAnimationProgress * 400).dp  // Start at center, move right off screen
                        val yOffset = (-30).dp  // Raise player attack sprite by 30 pixels
                        
                        AttackSpriteImage(
                            characterId = activeCharacter?.charaId ?: "dim011_mon01",
                            isLarge = true,
                            modifier = Modifier
                                .size(60.dp)
                                .offset(
                                    x = xOffset,
                                    y = yOffset
                                )
                                .scale(-1f, 1f), // Flip attack sprite
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Bottom section: Player HP bar, Critical bar and Attack button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Critical bar
            LinearProgressIndicator(
                progress = battleSystem.critBarProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Yellow,
                trackColor = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Player HP bar
            LinearProgressIndicator(
                progress = battleSystem.playerHP / (activeCharacter?.baseHp?.toFloat() ?: 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Green,
                trackColor = Color.Gray
            )

            // Player HP display numbers
            Text(
                text = "HP: ${battleSystem.playerHP.toInt()}/${activeCharacter?.baseHp ?: 100}",
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Attack button
            Button(
                onClick = {
                    println("Attack button clicked!")
                    
                    // Get crit bar progress as float (0.0f to 100.0f)
                    val critBarProgressFloat = battleSystem.critBarProgress.toFloat()
                    
                    // Determine player and opponent stages
                    val playerStage = when (activeCharacter?.stage) {
                        0 -> 0 // rookie
                        1 -> 1 // champion
                        2 -> 2 // ultimate
                        3 -> 3 // mega
                        else -> 0
                    }
                    
                    val opponentStage = when (opponentCharacter?.stage) {
                        0 -> 0 // rookie
                        1 -> 1 // champion
                        2 -> 2 // ultimate
                        3 -> 3 // mega
                        else -> 0
                    }
                    
                    // Send API call with all parameters
                    context?.let { ctx ->
                        // Start both attacks simultaneously
                        battleSystem.startPlayerAttack()
                        
                        RetrofitHelper().getPVPWinner(
                            ctx, 
                            1, 
                            2, 
                            activeCharacter?.name ?: "Player", 
                            playerStage, 
                            opponentStage, 
                            opponentCharacter?.name ?: "Opponent", 
                            opponentStage
                        ) { apiResult ->
                            // Handle API response here
                            println("API Result: $apiResult")
                            lastApiResult = apiResult // Store for debug display
                            
                            // Update HP based on API response
                            when (apiResult.state) {
                                 1 -> {
                                     // Match is still ongoing - update HP and continue
                                     println("Round ${apiResult.currentRound}: Player HP=${apiResult.playerHP}, Opponent HP=${apiResult.opponentHP}")
                                     
                                     // Set pending damage based on API result
                                     if (apiResult.playerAttackDamage > 0) {
                                         // Player attack hit - enemy takes damage at end of player animation
                                         println("Player attack hit! Enemy will take ${apiResult.playerAttackDamage} damage")
                                         onSetPendingDamage(0f, apiResult.playerAttackDamage.toFloat()) // Opponent takes damage
                                         battleSystem.setAttackHitState(true)
                                         
                                         // Also check if enemy counter-attacks and hits
                                         if (apiResult.opponentAttackDamage > 0) {
                                             println("Enemy counter-attack hits! Player takes ${apiResult.opponentAttackDamage} damage")
                                             onSetPendingDamage(apiResult.opponentAttackDamage.toFloat(), apiResult.playerAttackDamage.toFloat()) // Both take damage
                                         }
                                     } else {
                                         // Player attack missed - enemy counter-attacks
                                         println("Player attack missed! Enemy counter-attacks")
                                         battleSystem.setAttackHitState(false)
                                         // Set up counter-attack - determine if it hits based on API result
                                         val counterAttackHits = apiResult.opponentAttackDamage > 0
                                         println("Setting up counter-attack: counterAttackHits=$counterAttackHits, opponentAttackDamage=${apiResult.opponentAttackDamage}")
                                         println("Full API response: status=${apiResult.status}, state=${apiResult.state}, playerAttackHit=${apiResult.playerAttackHit}, playerAttackDamage=${apiResult.playerAttackDamage}, opponentAttackDamage=${apiResult.opponentAttackDamage}, playerHP=${apiResult.playerHP}, opponentHP=${apiResult.opponentHP}")
                                         println("DEBUG: Using playerAttackDamage > 0 instead of playerAttackHit for hit detection")
                                         
                                         // Use opponentAttackDamage to determine counter-attack hit
                                         val finalCounterAttackHits = counterAttackHits
                                         println("Using opponentAttackDamage > 0 for counter-attack: $finalCounterAttackHits")
                                         
                                         if (finalCounterAttackHits) {
                                             println("Counter-attack hits! Player takes ${apiResult.opponentAttackDamage} damage")
                                             onSetPendingDamage(apiResult.opponentAttackDamage.toFloat(), 0f) // Player takes damage
                                         } else {
                                             println("Counter-attack misses! Player dodges")
                                             onSetPendingDamage(0f, 0f) // No damage
                                         }
                                         battleSystem.setupCounterAttack(finalCounterAttackHits)
                                         // Set the opponent attack hit state for Phase 3
                                         battleSystem.handleOpponentAttackResult(finalCounterAttackHits)
                                     }
                                 }
                                 2 -> {
                                     // Match is over - transition to results screen
                                     println("Match is over! Winner: ${apiResult.winner}")
                                     battleSystem.updateHPFromAPI(apiResult.playerHP.toFloat(), apiResult.opponentHP.toFloat())
                                     onAttackClick() // This will transition to battle-results screen
                                 }
                                 -1 -> {
                                     // Error occurred
                                     println("API Error: ${apiResult.status}")
                                     battleSystem.resetAttackState()
                                     battleSystem.enableAttackButton()
                                 }
                             }
                         }
                     }
                 },
                 enabled = battleSystem.isAttackButtonEnabled,
                 modifier = Modifier
                     .fillMaxWidth(0.5f)
                     .height(35.dp),
                 colors = ButtonDefaults.buttonColors(
                     containerColor = Color.Blue,
                     disabledContainerColor = Color.Gray
                 ),
                 shape = RoundedCornerShape(8.dp)
             ) {
                 Text("Attack", color = Color.White, fontSize = 12.sp)
             }
        }
    }
}

@Composable
fun PlayerBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    playerName: String,
    attackAnimationProgress: Float,
    onAttackClick: () -> Unit,
    activeCharacter: APIBattleCharacter?,
    context: android.content.Context?,
    opponent: APIBattleCharacter?,
    onSetPendingDamage: (Float, Float) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Track previous character ID to detect transitions
    var previousCharacterId by remember { mutableStateOf<String?>(null) }
    var previousAttackPhase by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    var lastApiResult by remember { mutableStateOf<com.github.nacabaro.vbhelper.battle.PVPDataModel?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top section: Exit button, HP bar, and HP numbers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exit button at the top-right
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { /* TODO: Add exit functionality */ },
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Exit", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debug display
            /*
            if (lastApiResult != null) {
                Text(
                    text = "Debug: state=${lastApiResult!!.state}, playerAttackHit=${lastApiResult!!.playerAttackHit}, opponentDamage=${lastApiResult!!.opponentAttackDamage}",
                    color = Color.Red,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            */

            // Health bar
            LinearProgressIndicator(
                progress = battleSystem.playerHP / (activeCharacter?.baseHp?.toFloat() ?: 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Green,
                trackColor = Color.Gray
            )

            // Health display numbers
            Text(
                text = "HP: ${battleSystem.playerHP.toInt()}/${activeCharacter?.baseHp ?: 100}",
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        // Middle section: Player Digimon only
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Player Digimon (left side)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(80.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Determine animation type based on battle state
                val animationType = when {
                    battleSystem.isPlayerDodging -> DigimonAnimationType.WALK  // Use walk animation for dodge
                    battleSystem.isPlayerHit -> DigimonAnimationType.SLEEP     // Use sleep animation for hit effect (injured sprite)
                    battleSystem.attackPhase == 1 -> DigimonAnimationType.ATTACK  // Player attack on player screen
                    battleSystem.attackPhase == 2 -> DigimonAnimationType.ATTACK  // Player attack on opponent screen
                    battleSystem.attackPhase == 3 -> DigimonAnimationType.IDLE    // Opponent attack on opponent screen
                    battleSystem.attackPhase == 4 -> DigimonAnimationType.IDLE    // Opponent attack on player screen
                    else -> DigimonAnimationType.IDLE
                }
                
                // Calculate vertical offset for dodge animation
                val verticalOffset = if (battleSystem.isPlayerDodging) {
                    val dodgeHeight = 30.dp
                    val progress = battleSystem.playerDodgeProgress
                    val direction = battleSystem.playerDodgeDirection
                    
                    if (direction > 0) {
                        // Moving up (negative offset to move UP visually)
                        -(progress * dodgeHeight.value).dp
                    } else {
                        // Moving back down (from negative peak to 0)
                        -((1f - progress) * dodgeHeight.value).dp
                    }
                } else {
                    0.dp
                }
                
                // Calculate hit effect (slight shake)
                val hitOffset = if (battleSystem.isPlayerHit) {
                    val shakeAmount = 5.dp
                    val progress = battleSystem.hitProgress
                    // Simple shake effect without complex math
                    val shake = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                    (shake * shakeAmount.value).dp
                } else {
                    0.dp
                }
                
                AnimatedSpriteImage(
                    characterId = activeCharacter?.charaId ?: "dim011_mon01",
                    animationType = animationType,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(-1f, 1f) // Flip player Digimon horizontally
                        .offset(
                            x = hitOffset,
                            y = verticalOffset
                        ),
                    contentScale = ContentScale.Fit,
                    reloadMappings = false
                )
                
                // Attack sprite visibility and positioning based on attack phase
                val shouldShowAttack = when (battleSystem.attackPhase) {
                    1 -> false // Both attacks from middle screen
                    2 -> false // Player attack on enemy screen
                    3 -> true  // Enemy attack on player screen
                    else -> false
                }
                
                if (shouldShowAttack) {
                    val xOffset = when (battleSystem.attackPhase) {
                        3 -> (-attackAnimationProgress * 400 + 350).dp  // Enemy attack on player screen - start more to the right
                        else -> 0.dp
                    }
                    
                    // Use opponent character ID for Phase 3 (enemy attack)
                    val characterId = when (battleSystem.attackPhase) {
                        3 -> opponent?.charaId ?: "dim011_mon01"  // Use opponent's character ID
                        else -> activeCharacter?.charaId ?: "dim011_mon01"  // Use player's character ID
                    }
                    
                    // Handle sprite transition
                    LaunchedEffect(characterId, battleSystem.attackPhase) {
                        if ((previousCharacterId != null && previousCharacterId != characterId) ||
                            (previousAttackPhase != null && previousAttackPhase != battleSystem.attackPhase)) {
                            // Character ID or attack phase changed, start transition
                            isTransitioning = true
                            delay(100) // Brief invisibility period
                            isTransitioning = false
                        }
                        previousCharacterId = characterId
                        previousAttackPhase = battleSystem.attackPhase
                    }
                    
                    println("PlayerBattleView - Attack sprite - Phase: ${battleSystem.attackPhase}, Progress: $attackAnimationProgress, X Offset: $xOffset, CurrentView: ${battleSystem.currentView}")
                    
                    if (!isTransitioning) {
                        AttackSpriteImage(
                            characterId = characterId,
                            isLarge = true,
                            modifier = Modifier
                                .size(60.dp)
                                .offset(
                                    x = xOffset,
                                    y = 0.dp
                                )
                                .scale(if (battleSystem.attackPhase == 3) 1f else -1f, 1f), // Don't flip enemy attacks
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Bottom section: Critical bar and Attack button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Critical bar
            LinearProgressIndicator(
                progress = battleSystem.critBarProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Yellow,
                trackColor = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Attack button
            Button(
                onClick = {
                    println("Attack button clicked!")
                    
                    // Get crit bar progress as float (0.0f to 100.0f)
                    val critBarProgressFloat = battleSystem.critBarProgress.toFloat()
                    
                    // Determine player and opponent stages
                    val playerStage = when (activeCharacter?.stage) {
                        0 -> 0 // rookie
                        1 -> 1 // champion
                        2 -> 2 // ultimate
                        3 -> 3 // mega
                        else -> 0
                    }
                    
                    val opponentStage = when (opponent?.stage) {
                        0 -> 0 // rookie
                        1 -> 1 // champion
                        2 -> 2 // ultimate
                        3 -> 3 // mega
                        else -> 0
                    }
                    
                    // Send API call with all parameters
                    context?.let { ctx ->
                        // Start player attack animation
                        battleSystem.startPlayerAttack()
                        
                        RetrofitHelper().getPVPWinner(
                            ctx, 
                            1, 
                            2, 
                            activeCharacter?.name ?: "Player", 
                            playerStage, 
                            opponentStage, 
                            opponent?.name ?: "Opponent", 
                            opponentStage
                        ) { apiResult ->
                            // Handle API response here
                            println("API Result: $apiResult")
                            lastApiResult = apiResult // Store for debug display
                            
                            // Update HP based on API response
                            when (apiResult.state) {
                                 1 -> {
                                     // Match is still ongoing - update HP and continue
                                     println("Round ${apiResult.currentRound}: Player HP=${apiResult.playerHP}, Opponent HP=${apiResult.opponentHP}")
                                     
                                     // Set pending damage based on API result
                                     if (apiResult.playerAttackDamage > 0) {
                                         // Player attack hit - enemy takes damage at end of player animation
                                         println("Player attack hit! Enemy will take ${apiResult.playerAttackDamage} damage")
                                         onSetPendingDamage(0f, apiResult.playerAttackDamage.toFloat()) // Opponent takes damage
                                         battleSystem.setAttackHitState(true)
                                         
                                         // Also check if enemy counter-attacks and hits
                                         if (apiResult.opponentAttackDamage > 0) {
                                             println("Enemy counter-attack hits! Player takes ${apiResult.opponentAttackDamage} damage")
                                             onSetPendingDamage(apiResult.opponentAttackDamage.toFloat(), apiResult.playerAttackDamage.toFloat()) // Both take damage
                                         }
                                     } else {
                                         // Player attack missed - enemy counter-attacks
                                         println("Player attack missed! Enemy counter-attacks")
                                         battleSystem.setAttackHitState(false)
                                         // Set up counter-attack - determine if it hits based on API result
                                         val counterAttackHits = apiResult.opponentAttackDamage > 0
                                         println("Setting up counter-attack: counterAttackHits=$counterAttackHits, opponentAttackDamage=${apiResult.opponentAttackDamage}")
                                         println("Full API response: status=${apiResult.status}, state=${apiResult.state}, playerAttackHit=${apiResult.playerAttackHit}, playerAttackDamage=${apiResult.playerAttackDamage}, opponentAttackDamage=${apiResult.opponentAttackDamage}, playerHP=${apiResult.playerHP}, opponentHP=${apiResult.opponentHP}")
                                         println("DEBUG: Using playerAttackDamage > 0 instead of playerAttackHit for hit detection")
                                         
                                         // Use opponentAttackDamage to determine counter-attack hit
                                         val finalCounterAttackHits = counterAttackHits
                                         println("Using opponentAttackDamage > 0 for counter-attack: $finalCounterAttackHits")
                                         
                                         if (finalCounterAttackHits) {
                                             println("Counter-attack hits! Player takes ${apiResult.opponentAttackDamage} damage")
                                             onSetPendingDamage(apiResult.opponentAttackDamage.toFloat(), 0f) // Player takes damage
                                         } else {
                                             println("Counter-attack misses! Player dodges")
                                             onSetPendingDamage(0f, 0f) // No damage
                                         }
                                         battleSystem.setupCounterAttack(finalCounterAttackHits)
                                         // Set the opponent attack hit state for Phase 3
                                         battleSystem.handleOpponentAttackResult(finalCounterAttackHits)
                                     }
                                 }
                                 2 -> {
                                     // Match is over - transition to results screen
                                     println("Match is over! Winner: ${apiResult.winner}")
                                     battleSystem.updateHPFromAPI(apiResult.playerHP.toFloat(), apiResult.opponentHP.toFloat())
                                     onAttackClick() // This will transition to battle-results screen
                                 }
                                 -1 -> {
                                     // Error occurred
                                     println("API Error: ${apiResult.status}")
                                     battleSystem.resetAttackState()
                                     battleSystem.enableAttackButton()
                                 }
                             }
                         }
                     }
                 },
                 enabled = battleSystem.isAttackButtonEnabled,
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(50.dp),
                 colors = ButtonDefaults.buttonColors(
                     containerColor = Color.Red,
                     disabledContainerColor = Color.Gray
                 ),
                 shape = RoundedCornerShape(8.dp)
             ) {
                 Text("Attack", color = Color.White, fontSize = 18.sp)
             }
        }
    }
}

@Composable
fun EnemyBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    opponentName: String,
    attackAnimationProgress: Float,
    activeCharacter: APIBattleCharacter? = null,
    playerCharacter: APIBattleCharacter? = null
) {
    // Track previous character ID to detect transitions
    var previousCharacterId by remember { mutableStateOf<String?>(null) }
    var previousAttackPhase by remember { mutableStateOf<Int?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top section: Enemy HP bar and HP numbers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Enemy HP bar
            LinearProgressIndicator(
                progress = battleSystem.opponentHP / (activeCharacter?.baseHp?.toFloat() ?: 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color.Red,
                trackColor = Color.Gray
            )

            // Enemy HP display numbers
            Text(
                text = "Enemy HP: ${battleSystem.opponentHP.toInt()}/${activeCharacter?.baseHp ?: 100}",
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        // Middle section: Enemy Digimon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Enemy Digimon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(80.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Determine animation type based on battle state
                val animationType = when {
                    battleSystem.isOpponentDodging -> DigimonAnimationType.WALK  // Use walk animation for dodge
                    battleSystem.isOpponentHit -> DigimonAnimationType.SLEEP     // Use sleep animation for hit effect (injured sprite)
                    battleSystem.attackPhase == 2 -> DigimonAnimationType.IDLE    // Player attack on enemy screen
                    else -> DigimonAnimationType.IDLE
                }
                
                // Calculate vertical offset for dodge animation
                val verticalOffset = if (battleSystem.isOpponentDodging) {
                    val dodgeHeight = 30.dp
                    val progress = battleSystem.opponentDodgeProgress
                    val direction = battleSystem.opponentDodgeDirection
                    
                    if (direction > 0) {
                        // Moving up (negative offset to move UP visually)
                        -(progress * dodgeHeight.value).dp
                    } else {
                        // Moving back down (from negative peak to 0)
                        -((1f - progress) * dodgeHeight.value).dp
                    }
                } else {
                    0.dp
                }
                
                // Calculate hit effect (slight shake)
                val hitOffset = if (battleSystem.isOpponentHit) {
                    val shakeAmount = 5.dp
                    val progress = battleSystem.hitProgress
                    // Simple shake effect without complex math
                    val shake = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                    (shake * shakeAmount.value).dp
                } else {
                    0.dp
                }
                
                AnimatedSpriteImage(
                    characterId = activeCharacter?.charaId ?: "dim011_mon01",
                    animationType = animationType,
                    modifier = Modifier
                        .size(80.dp)
                        .offset(
                            x = hitOffset,
                            y = verticalOffset
                        ),
                    contentScale = ContentScale.Fit,
                    reloadMappings = false
                )
                
                // Attack sprite visibility and positioning based on attack phase
                val shouldShowAttack = when (battleSystem.attackPhase) {
                    2 -> true  // Player attack on enemy screen
                    else -> false
                }
                
                if (shouldShowAttack) {
                    val xOffset = (attackAnimationProgress * 400 - 350).dp  // Player attack on enemy screen - start more to the left
                    
                    // Use player's character ID for player attack
                    val characterId = playerCharacter?.charaId ?: "dim011_mon01"
                    
                    // Handle sprite transition
                    LaunchedEffect(characterId, battleSystem.attackPhase) {
                        if ((previousCharacterId != null && previousCharacterId != characterId) ||
                            (previousAttackPhase != null && previousAttackPhase != battleSystem.attackPhase)) {
                            // Character ID or attack phase changed, start transition
                            isTransitioning = true
                            delay(100) // Brief invisibility period
                            isTransitioning = false
                        }
                        previousCharacterId = characterId
                        previousAttackPhase = battleSystem.attackPhase
                    }
                    
                    println("EnemyBattleView - Attack sprite - Phase: ${battleSystem.attackPhase}, Progress: $attackAnimationProgress, X Offset: $xOffset, CurrentView: ${battleSystem.currentView}")
                    
                    if (!isTransitioning) {
                        AttackSpriteImage(
                            characterId = characterId,
                            isLarge = true,
                            modifier = Modifier
                                .size(60.dp)
                                .offset(
                                    x = xOffset,
                                    y = 0.dp
                                )
                                .scale(-1f, 1f), // Flip player attacks
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen() {
    val TAG = "BattleScreen"

    var currentView by remember { mutableStateOf("main") }

    var opponentsList by remember { mutableStateOf(ArrayList<APIBattleCharacter>()) }

    var activeCharacter by remember { mutableStateOf<APIBattleCharacter?>(null) }
    var selectedOpponent by remember { mutableStateOf<APIBattleCharacter?>(null) }

    var expanded by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf("") }
    var currentStage by remember { mutableStateOf("rookie") }
    
    // Sprite animation tester state
    var showSpriteTester by remember { mutableStateOf(false) }
    var spriteTesterView by remember { mutableStateOf("entry") } // "entry" or "testing"
    var dimId by remember { mutableStateOf("") }
    var monId by remember { mutableStateOf("") }
    var currentTestAnimation by remember { mutableStateOf(DigimonAnimationType.IDLE) }
    var testCharacterId by remember { mutableStateOf("") }

    // Create hardcoded character lists for each stage
    val rookieCharacters = listOf(
        APIBattleCharacter("AGUMON", "degimon_name_Dim012_003", "dim012_mon03", 0, 1, 1800, 1800, 2400.0f, 700.0f),
        APIBattleCharacter("PULSEMON", "degimon_name_Dim000_003", "dim000_mon03", 0, 1, 1800, 1800, 2400.0f, 700.0f),
        APIBattleCharacter("DORUMON", "degimon_name_dim137_mon03", "dim137_mon03", 0, 1, 3000, 3000, 5100.0f, 1050.0f)
    )

    val championCharacters = listOf(
        APIBattleCharacter("GREYMON","degimon_name_Dim012_004","dim012_mon04",1,1,2000, 2000, 3000.0f,900.0f),
        APIBattleCharacter("TYRANNOMON","degimon_name_Dim008_006","dim008_mon06",1,3,2000, 2000, 2400.0f,600.0f),
        APIBattleCharacter("DORUGAMON","degimon_name_dim137_mon05","dim137_mon05",1,3,3500, 3500, 5200.0f,1200.0f)
    )

    val ultimateCharacters = listOf(
        APIBattleCharacter("METALGREYMON (VIRUS)","degimon_name_Dim014_005","dim014_mon05",2,2,2640, 2640, 2450.0f,800.0f),
        APIBattleCharacter("MAMEMON", "degimon_name_Dim000_005", "dim000_mon05", 2, 1, 3000, 3000, 4000.0f, 1000.0f),
        APIBattleCharacter("DORUGREYMON","degimon_name_dim137_mon09","dim137_mon09",2,3,5000, 5000, 6400.0f,1400.0f)
    )

    val megaCharacters = listOf(
        APIBattleCharacter("WARGREYMON","degimon_name_Dim012_014","dim012_mon14",3,1,3080, 3080, 3825.0f,800.0f),
        APIBattleCharacter("SLAYERDRAMON","degimon_name_dim129_mon15","dim129_mon15",3,1,4800, 4800, 6300.0f,1950.0f),
        APIBattleCharacter("BREAKDRAMON","degimon_name_dim129_mon17","dim129_mon17",3,2,6000, 6000, 4000.0f,1980.0f)
    )

    // Get the appropriate character list based on current stage
    val characterList = when (currentStage.lowercase()) {
        "rookie" -> rookieCharacters
        "champion" -> championCharacters
        "ultimate" -> ultimateCharacters
        "mega" -> megaCharacters
        else -> rookieCharacters
    }

    val context = LocalContext.current
    
    // Initialize sprite files on first load
    LaunchedEffect(Unit) {
        println("BATTLESCREEN: LaunchedEffect triggered - checking sprite files...")
        val spriteFileManager = SpriteFileManager(context)
        if (!spriteFileManager.checkSpriteFilesExist()) {
            println("BATTLESCREEN: Copying sprite files to internal storage...")
            spriteFileManager.copySpriteFilesToInternalStorage()
        } else {
            println("BATTLESCREEN: Sprite files already exist in internal storage")
        }
    }

    val rookieButton = @Composable {
        Button(
            onClick = {
                try {
                    RetrofitHelper().getOpponents(context, "rookie") { opponents ->
                        try {
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)
                            currentView = "rookie"
                            currentStage = "rookie"
                        } catch (e: Exception) {
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG,"Error calling getOpponents: ${e.message}")
                    e.printStackTrace()
                }
            }
        ) {
            Text("Rookie Battles")
        }
    }

    val championButton = @Composable {
        Button(
            onClick = {
                try {
                    RetrofitHelper().getOpponents(context, "champion") { opponents ->
                        try {
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)
                            currentView = "champion"
                            currentStage = "champion"
                        } catch (e: Exception) {
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG,"Error calling getOpponents: ${e.message}")
                    e.printStackTrace()
                }
            }
        ) {
            Text("Champion Battles")
        }
    }

    val ultimateButton = @Composable {
        Button(
            onClick = {
                try {
                    RetrofitHelper().getOpponents(context, "ultimate") { opponents ->
                        try {
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)
                            currentView = "ultimate"
                            currentStage = "ultimate"
                        } catch (e: Exception) {
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG,"Error calling getOpponents: ${e.message}")
                    e.printStackTrace()
                }
            }
        ) {
            Text("Ultimate Battles")
        }
    }

    val megaButton = @Composable {
        Button(
            onClick = {
                try {
                    RetrofitHelper().getOpponents(context, "mega") { opponents ->
                        try {
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)
                            currentView = "mega"
                            currentStage = "mega"
                        } catch (e: Exception) {
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG,"Error calling getOpponents: ${e.message}")
                    e.printStackTrace()
                }
            }
        ) {
            Text("Mega Battles")
        }
    }

    val backButton = @Composable {
        Button(
            onClick = { currentView = "main" }
        ) {
            Text("Back")
        }
    }

    val characterDropdown = @Composable { currentStage: String ->
        // Get the appropriate character list based on the passed currentStage parameter
        val characterListForStage = when (currentStage.lowercase()) {
            "rookie" -> rookieCharacters
            "champion" -> championCharacters
            "ultimate" -> ultimateCharacters
            "mega" -> megaCharacters
            else -> rookieCharacters
        }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedStage.ifEmpty { "Select Character" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                characterListForStage.forEach { character ->
                    DropdownMenuItem(
                        text = { Text(character.name) },
                        onClick = {
                            selectedStage = character.name
                            activeCharacter = character
                            expanded = false
                            println("Selected character: ${character.name}")
                        }
                    )
                }
            }
        }
    }
    
    val spriteTesterEntry = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Sprite Animation Tester", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // DIM ID input
            OutlinedTextField(
                value = dimId,
                onValueChange = { dimId = it },
                label = { Text("DIM ID (e.g., 012)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mon ID input
            OutlinedTextField(
                value = monId,
                onValueChange = { monId = it },
                label = { Text("Mon ID (e.g., 03)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Load sprite button
            Button(
                onClick = {
                    if (dimId.isNotEmpty() && monId.isNotEmpty()) {
                        testCharacterId = "dim${dimId}_mon${monId}"
                        println("Testing sprite for: $testCharacterId")
                        spriteTesterView = "testing"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Sprite")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showSpriteTester = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Main")
            }
        }
    }
    
    val spriteTesterTesting = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Sprite Animation Testing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Character: $testCharacterId", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display sprite
            AnimatedSpriteImage(
                characterId = testCharacterId,
                animationType = currentTestAnimation,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
                reloadMappings = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Animation buttons in a grid
            Text("Animation Buttons:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val animationTypes = listOf(
                DigimonAnimationType.IDLE to "IDLE",
                DigimonAnimationType.IDLE2 to "IDLE2",
                DigimonAnimationType.WALK to "WALK",
                DigimonAnimationType.WALK2 to "WALK2",
                DigimonAnimationType.RUN to "RUN",
                DigimonAnimationType.RUN2 to "RUN2",
                DigimonAnimationType.WORKOUT to "WORKOUT",
                DigimonAnimationType.WORKOUT2 to "WORKOUT2",
                DigimonAnimationType.HAPPY to "HAPPY",
                DigimonAnimationType.SLEEP to "SLEEP",
                DigimonAnimationType.ATTACK to "ATTACK",
                DigimonAnimationType.FLEE to "FLEE"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Create rows of 3 buttons each
                animationTypes.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { (animationType, label) ->
                            Button(
                                onClick = { 
                                    currentTestAnimation = animationType
                                    println("Switched to animation: $label")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentTestAnimation == animationType) Color.Blue else Color.Gray
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(label, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { spriteTesterView = "entry" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to Entry")
                }
                
                Button(
                    onClick = { 
                        showSpriteTester = false
                        spriteTesterView = "entry"
                        testCharacterId = ""
                        dimId = ""
                        monId = ""
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to Main")
                }
            }
        }
    }

    Scaffold (
        topBar = {
            // Only show TopBanner when not in battle mode
            if (currentView != "battle-main" && currentView != "battle-results") {
                TopBanner(
                    text = "Online battles"
                )
            }
        }
    ) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            when (currentView) {
                "main" -> {
                    if (showSpriteTester) {
                        when (spriteTesterView) {
                            "entry" -> spriteTesterEntry()
                            "testing" -> spriteTesterTesting()
                            else -> spriteTesterEntry()
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            rookieButton()
                            championButton()
                            ultimateButton()
                            megaButton()
                            Button(
                                onClick = { 
                                    showSpriteTester = true
                                    spriteTesterView = "entry"
                                    testCharacterId = ""
                                    dimId = ""
                                    monId = ""
                                    currentTestAnimation = DigimonAnimationType.IDLE
                                }
                            ) {
                                Text("Sprite Animation Tester")
                            }
                            Button(
                                onClick = {
                                    val spriteFileManager = SpriteFileManager(context)
                                    spriteFileManager.clearSpriteFiles()
                                    println("Sprite files cleared!")
                                }
                            ) {
                                Text("Clear Sprite Files")
                            }
                        }
                    }
                }

                "rookie" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Rookie Battle View")

                            // Add character selection dropdown
                            characterDropdown("rookie")

                            // Display buttons for each opponent
                            opponentsList.forEach { opponent ->
                                Button(
                                    onClick = {
                                        activeCharacter?.let {
                                            selectedOpponent = opponent
                                            RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 0, 0, opponent.name, 0) { apiResult ->
                                                currentView = "battle-main"
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("Battle ${opponent.name}")
                                }
                            }

                            // Show selected character info
                            activeCharacter?.let { character ->
                                Text("Active Character: ${character.name}")
                                Text("HP: ${character.currentHp}/${character.baseHp}")
                                Text("BP: ${character.baseBp}")
                                Text("AP: ${character.baseAp}")
                            }

                            backButton()
                        }
                }

                "champion" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Champion Battle View")

                            // Add character selection dropdown
                            characterDropdown("champion")

                            // Display buttons for each opponent
                            opponentsList.forEach { opponent ->
                                Button(
                                    onClick = {
                                        activeCharacter?.let {
                                            selectedOpponent = opponent
                                            RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 1, 0, opponent.name, 1) { apiResult ->
                                                currentView = "battle-main"
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("Battle ${opponent.name}")
                                }
                            }

                            // Show selected character info
                            activeCharacter?.let { character ->
                                Text("Active Character: ${character.name}")
                                Text("HP: ${character.currentHp}/${character.baseHp}")
                                Text("BP: ${character.baseBp}")
                                Text("AP: ${character.baseAp}")
                            }

                            backButton()
                        }
                }

                "ultimate" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Ultimate Battle View")

                            // Add character selection dropdown
                            characterDropdown("ultimate")

                            // Display buttons for each opponent
                            opponentsList.forEach { opponent ->
                                Button(
                                    onClick = {
                                        activeCharacter?.let {
                                            selectedOpponent = opponent
                                            RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 2, 0, opponent.name, 2) { apiResult ->
                                                currentView = "battle-main"
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("Battle ${opponent.name}")
                                }
                            }

                            // Show selected character info
                            activeCharacter?.let { character ->
                                Text("Active Character: ${character.name}")
                                Text("HP: ${character.currentHp}/${character.baseHp}")
                                Text("BP: ${character.baseBp}")
                                Text("AP: ${character.baseAp}")
                            }

                            backButton()
                        }
                }

                "mega" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Mega Battle View")

                            // Add character selection dropdown
                            characterDropdown("mega")

                            // Display buttons for each opponent
                            opponentsList.forEach { opponent ->
                                Button(
                                    onClick = {
                                        activeCharacter?.let {
                                            selectedOpponent = opponent
                                            RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 3, 0, opponent.name, 3) { apiResult ->
                                                currentView = "battle-main"
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("Battle ${opponent.name}")
                                }
                            }

                            // Show selected character info
                            activeCharacter?.let { character ->
                                Text("Active Character: ${character.name}")
                                Text("HP: ${character.currentHp}/${character.baseHp}")
                                Text("BP: ${character.baseBp}")
                                Text("AP: ${character.baseAp}")
                            }

                            backButton()
                        }
                }

                "battle-main" -> {
                    BattleScreen(
                        stage = currentStage,
                        playerName = activeCharacter?.name ?: "Player",
                        opponentName = selectedOpponent?.name ?: "Opponent",
                        activeCharacter = activeCharacter,
                        opponentCharacter = selectedOpponent,
                        onAttackClick = {
                            // This will be called when the battle is over
                            currentView = "battle-results"
                        },
                        context = context
                    )
                }

                "battle-results" -> {
                    var winnerName by remember { mutableStateOf("") }
                    var isWinnerLoaded by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        // Determine player and opponent stages
                        val playerStage = when (activeCharacter?.stage) {
                            0 -> 0 // rookie
                            1 -> 1 // champion
                            2 -> 2 // ultimate
                            3 -> 3 // mega
                            else -> 0
                        }
                        
                        val opponentStage = when (selectedOpponent?.stage) {
                            0 -> 0 // rookie
                            1 -> 1 // champion
                            2 -> 2 // ultimate
                            3 -> 3 // mega
                            else -> 0
                        }
                        
                        // First get the winner info
                        RetrofitHelper().getPVPWinner(
                            context, 
                            1, 
                            2, 
                            activeCharacter?.name ?: "Player", 
                            playerStage, 
                            opponentStage, 
                            selectedOpponent?.name ?: "Opponent", 
                            opponentStage
                        ) { apiResult ->
                            winnerName = apiResult.winner ?: "Unknown"
                            isWinnerLoaded = true
                            
                            // Then send the cleanup call
                            RetrofitHelper().getPVPWinner(
                                context, 
                                2, 
                                2, 
                                activeCharacter?.name ?: "Player", 
                                playerStage, 
                                opponentStage, 
                                selectedOpponent?.name ?: "Opponent", 
                                opponentStage
                            ) { cleanupResult ->
                                println("Cleanup call completed")
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Battle Complete!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isWinnerLoaded) {
                                Text(
                                    text = "Winner: $winnerName",
                                    fontSize = 20.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Text(
                                    text = "Loading results...",
                                    fontSize = 20.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        // Exit button
                        Button(
                            onClick = { currentView = "main" },
                            modifier = Modifier.align(Alignment.TopCenter),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Exit", color = Color.White)
                        }
                    }
                }
            }
        }
    }
} 

@Composable
fun AnimatedBattleBackground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var backgroundBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var xOffset by remember { mutableStateOf(0f) }
    var screenWidth by remember { mutableStateOf(0.dp) }
    var screenHeight by remember { mutableStateOf(0.dp) }

    // Get screen dimensions
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    LaunchedEffect(Unit) {
        screenWidth = with(density) { configuration.screenWidthDp.dp }
        screenHeight = with(density) { configuration.screenHeightDp.dp }
        println("DEBUG: Screen dimensions = ${screenWidth.value}x${screenHeight.value}dp")
    }

    // Load background image from internal storage
    LaunchedEffect(Unit) {
        try {
            val backgroundFile = File(context.filesDir, "battle_sprites/extracted_battlebgs/BattleBg_0015_BattleBg_0012.png")
            if (backgroundFile.exists()) {
                backgroundBitmap = BitmapFactory.decodeFile(backgroundFile.absolutePath)
                println("Successfully loaded battle background: ${backgroundFile.absolutePath}")
                println("DEBUG: Image dimensions = ${backgroundBitmap?.width}x${backgroundBitmap?.height} pixels")
            } else {
                println("Battle background file not found: ${backgroundFile.absolutePath}")
            }
        } catch (e: Exception) {
            println("Error loading battle background: ${e.message}")
        }
    }

    // Animate horizontal movement to the left with perfect loop
    LaunchedEffect(screenWidth) {
        if (screenWidth > 0.dp) {
            while (true) {
                delay(50) // Update every 50ms for smooth animation
                xOffset -= 1f // Move 1 pixel to the left
                
                // Create perfect loop by resetting when one full screen width has moved
                if (xOffset <= -screenWidth.value) {
                    xOffset = 0f
                    println("DEBUG: Background loop reset at xOffset = ${xOffset}")
                }
            }
        }
    }
    
    backgroundBitmap?.let { bitmap ->
        Box(modifier = modifier.fillMaxSize()) {
            // First image (main)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Animated Battle Background 1",
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = xOffset.dp),
                contentScale = ContentScale.FillBounds
            )
            
            // Second image (for seamless loop)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Animated Battle Background 2",
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (xOffset + screenWidth.value).dp),
                contentScale = ContentScale.FillBounds
            )
        }
    }
} 