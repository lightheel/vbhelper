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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.github.nacabaro.vbhelper.battle.APIBattleCharacter
import android.util.Log
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.battle.RetrofitHelper
import com.github.nacabaro.vbhelper.battle.SpriteImage
import com.github.nacabaro.vbhelper.battle.AttackSpriteImage
import com.github.nacabaro.vbhelper.battle.SpriteFileManager
import com.github.nacabaro.vbhelper.battle.ArenaBattleSystem

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
    
    // Initialize HP when battle starts
    LaunchedEffect(activeCharacter, opponentCharacter) {
        val playerMaxHP = activeCharacter?.baseHp?.toFloat() ?: 100f
        val opponentMaxHP = opponentCharacter?.baseHp?.toFloat() ?: 100f
        battleSystem.initializeHP(playerMaxHP, opponentMaxHP)
    }
    
    // Pending damage state for API integration
    var pendingPlayerDamage by remember { mutableStateOf(0f) }
    var pendingOpponentDamage by remember { mutableStateOf(0f) }
    
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
                // Phase 1: Player attack on player screen
                println("Starting Phase 1: Player attack on player screen")
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
                // Phase 2: Player attack on opponent screen
                println("Starting Phase 2: Player attack on opponent screen")
                battleSystem.switchToView(1)
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    delay(16) // 60 FPS
                }
                                 println("Phase 2 completed, applying damage and starting opponent attack")
                 // Apply player's damage and start opponent attack
                 battleSystem.completeAttackAnimation(opponentDamage = pendingOpponentDamage)
                 pendingOpponentDamage = 0f
                 delay(500)
                 battleSystem.startOpponentAttack()
            }
            3 -> {
                // Phase 3: Opponent attack on opponent screen
                println("Starting Phase 3: Opponent attack on opponent screen")
                battleSystem.switchToView(1)
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    delay(16) // 60 FPS
                }
                println("Phase 3 completed, advancing to Phase 4")
                battleSystem.advanceAttackPhase()
            }
            4 -> {
                // Phase 4: Opponent attack on player screen
                println("Starting Phase 4: Opponent attack on player screen")
                battleSystem.switchToView(0)
                var progress = 0f
                while (progress < 1f) {
                    progress += 0.016f // 60 FPS
                    battleSystem.setAttackProgress(progress)
                    delay(16) // 60 FPS
                }
                                 println("Phase 4 completed, applying damage and resetting")
                 // Apply opponent's damage and reset
                 battleSystem.completeAttackAnimation(playerDamage = pendingPlayerDamage)
                 pendingPlayerDamage = 0f
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (battleSystem.currentView) {
            0 -> {
                // Player view
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
                    }
                )
            }
            1 -> {
                // Opponent view
                OpponentBattleView(
                    battleSystem = battleSystem,
                    stage = stage,
                    opponentName = opponentName,
                    attackAnimationProgress = battleSystem.attackProgress,
                    activeCharacter = opponentCharacter,
                    playerCharacter = activeCharacter
                )
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
    onSetPendingDamage: (Float, Float) -> Unit
) {
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
                    Text("Exit", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                SpriteImage(
                    spriteName = activeCharacter?.charaId ?: "dim011_mon01",
                    atlasName = activeCharacter?.charaId ?: "dim011_mon01",
                    modifier = Modifier
                        .size(80.dp)
                        .scale(-1f, 1f), // Flip player Digimon horizontally
                    contentScale = ContentScale.Fit
                )
                
                // Attack sprite visibility and positioning based on attack phase
                val shouldShowAttack = when (battleSystem.attackPhase) {
                    1 -> true  // Player attack on player screen
                    2 -> true  // Player attack on opponent screen  
                    3 -> false // Opponent attack on opponent screen
                    4 -> true  // Opponent attack on player screen
                    else -> false
                }
                
                if (shouldShowAttack) {
                    val xOffset = when (battleSystem.attackPhase) {
                        1 -> (attackAnimationProgress * 400 + 50).dp  // Player attack on player screen - start and end more to the right
                        2 -> (attackAnimationProgress * 400 - 200).dp  // Player attack on opponent screen
                        4 -> (-attackAnimationProgress * 400 + 200).dp  // Opponent attack on player screen
                        else -> 0.dp
                    }
                    
                    // Use opponent character ID for Phase 4 (opponent attack)
                    val characterId = when (battleSystem.attackPhase) {
                        4 -> opponent?.charaId ?: "dim011_mon01"  // Use opponent's character ID
                        else -> activeCharacter?.charaId ?: "dim011_mon01"  // Use player's character ID
                    }
                    
                    println("PlayerBattleView - Attack sprite - Phase: ${battleSystem.attackPhase}, Progress: $attackAnimationProgress, X Offset: $xOffset, CurrentView: ${battleSystem.currentView}")
                    
                    AttackSpriteImage(
                        characterId = characterId,
                        isLarge = true,
                        modifier = Modifier
                            .size(60.dp)
                            .offset(
                                x = xOffset,
                                y = 0.dp
                            )
                            .scale(if (battleSystem.attackPhase == 4) 1f else -1f, 1f), // Don't flip opponent attacks
                        contentScale = ContentScale.Fit
                    )
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
                            
                            // Update HP based on API response
                            when (apiResult.state) {
                                 1 -> {
                                     // Match is still ongoing - update HP and continue
                                     println("Round ${apiResult.currentRound}: Player HP=${apiResult.playerHP}, Opponent HP=${apiResult.opponentHP}")
                                     
                                                                                                         // Set pending damage based on API result
                                      if (apiResult.playerAttackHit) {
                                          // Player attack hit - enemy takes damage at end of player animation
                                          println("Player attack hit! Enemy will take ${apiResult.playerAttackDamage} damage")
                                          onSetPendingDamage(0f, apiResult.playerAttackDamage.toFloat()) // Opponent takes damage
                                          battleSystem.setAttackHitState(true)
                                      } else {
                                          // Player attack missed - enemy counter-attacks and player takes damage
                                          println("Player attack missed! Enemy counter-attacks and player takes ${apiResult.opponentAttackDamage} damage")
                                          onSetPendingDamage(apiResult.opponentAttackDamage.toFloat(), 0f) // Player takes damage
                                          battleSystem.setAttackHitState(false)
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
fun OpponentBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    opponentName: String,
    attackAnimationProgress: Float,
    activeCharacter: APIBattleCharacter? = null,
    playerCharacter: APIBattleCharacter? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Opponent Digimon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(80.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            SpriteImage(
                spriteName = activeCharacter?.charaId ?: "dim011_mon01",
                atlasName = activeCharacter?.charaId ?: "dim011_mon01",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit
            )
            
            // Attack sprite visibility and positioning based on attack phase
            val shouldShowAttack = when (battleSystem.attackPhase) {
                1 -> false // Player attack on player screen
                2 -> true  // Player attack on opponent screen  
                3 -> true  // Opponent attack on opponent screen
                4 -> false // Opponent attack on player screen
                else -> false
            }
            
            if (shouldShowAttack) {
                val xOffset = when (battleSystem.attackPhase) {
                    2 -> (attackAnimationProgress * 400 - 350).dp  // Player attack on opponent screen - start more to the left
                    3 -> (-attackAnimationProgress * 400 + 200).dp  // Opponent attack on opponent screen
                    else -> 0.dp
                }
                
                // Use correct character ID based on attack phase
                val characterId = when (battleSystem.attackPhase) {
                    2 -> playerCharacter?.charaId ?: "dim011_mon01"  // Use player's character ID for player attack
                    3 -> activeCharacter?.charaId ?: "dim011_mon01"  // Use opponent's character ID for opponent attack
                    else -> "dim011_mon01"
                }
                
                println("OpponentBattleView - Attack sprite - Phase: ${battleSystem.attackPhase}, Progress: $attackAnimationProgress, X Offset: $xOffset, CurrentView: ${battleSystem.currentView}")
                
                AttackSpriteImage(
                    characterId = characterId,
                    isLarge = true,
                    modifier = Modifier
                        .size(60.dp)
                        .offset(
                            x = xOffset,
                            y = 0.dp
                        )
                        .scale(if (battleSystem.attackPhase == 2) -1f else 1f, 1f), // Flip player attacks only
                    contentScale = ContentScale.Fit
                )
            }
        }

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

        // Spacer for layout balance
        Spacer(modifier = Modifier.height(120.dp))
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
        val spriteFileManager = SpriteFileManager(context)
        if (!spriteFileManager.checkSpriteFilesExist()) {
            println("Copying sprite files to internal storage...")
            spriteFileManager.copySpriteFilesToInternalStorage()
        } else {
            println("Sprite files already exist in internal storage")
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
                characterList.forEach { character ->
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

    Scaffold (
        topBar = {
            TopBanner(
                text = "Online battles"
            )
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        rookieButton()
                        championButton()
                        ultimateButton()
                        megaButton()
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