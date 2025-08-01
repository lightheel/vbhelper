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
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
//import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.github.nacabaro.vbhelper.battle.APIBattleCharacter
//import com.github.nacabaro.vbhelper.battle.BattleSpriteManager
import android.util.Log
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.battle.RetrofitHelper

class ArenaBattleSystem {
    companion object {
        private const val TAG = "VBArenaBattleSystem"
        const val ANIMATION_DURATION = 1500L
    }

    // Battle state
    private var _playerCurrentHP by mutableStateOf(100f)
    private var _playerMaxHP by mutableStateOf(100f)
    private var _opponentCurrentHP by mutableStateOf(100f)
    private var _opponentMaxHP by mutableStateOf(100f)
    private var _isAttacking by mutableStateOf(false)
    private var _attackProgress by mutableStateOf(0f)
    private var _currentView by mutableStateOf(0) // 0 = player, 1 = opponent
    private var _isAttackVisible by mutableStateOf(false)
    private var _critBarProgress by mutableStateOf(0)
    private var _isAttackButtonEnabled by mutableStateOf(true)

    // Exposed state for Compose
    val playerCurrentHP: Float get() = _playerCurrentHP
    val playerMaxHP: Float get() = _playerMaxHP
    val opponentCurrentHP: Float get() = _opponentCurrentHP
    val opponentMaxHP: Float get() = _opponentMaxHP
    val isAttacking: Boolean get() = _isAttacking
    val attackProgress: Float get() = _attackProgress
    val currentView: Int get() = _currentView
    val isAttackVisible: Boolean get() = _isAttackVisible
    val critBarProgress: Int get() = _critBarProgress
    val isAttackButtonEnabled: Boolean get() = _isAttackButtonEnabled

    //Initialize battle with character data
    fun initializeBattle(
        playerHP: Float = 100f,
        opponentHP: Float = 100f,
        playerMaxHP: Float = 100f,
        opponentMaxHP: Float = 100f
    ) {
        _playerCurrentHP = playerHP
        _playerMaxHP = playerMaxHP
        _opponentCurrentHP = opponentHP
        _opponentMaxHP = opponentMaxHP
        _currentView = 0
        _isAttacking = false
        _attackProgress = 0f
        _isAttackVisible = false
        _critBarProgress = 0
        _isAttackButtonEnabled = true

        Log.d(TAG, "Battle initialized: Player HP $playerHP/$playerMaxHP, Opponent HP $opponentHP/$opponentMaxHP")
    }

    //Start player attack
    fun startPlayerAttack() {
        _isAttacking = true
        _currentView = 0
        _isAttackVisible = true
        _isAttackButtonEnabled = false
        Log.d(TAG, "Player attack started")
    }

    //Start opponent attack
    fun startOpponentAttack() {
        _isAttacking = true
        _currentView = 1
        _isAttackVisible = true
        Log.d(TAG, "Opponent attack started")
    }

    //Update attack animation progress
    fun updateAttackAnimation(progress: Float) {
        _attackProgress = progress
    }

    //Complete attack animation
    fun completeAttackAnimation() {
        _isAttacking = false
        _isAttackVisible = false
        _attackProgress = 0f
        _currentView = if (_currentView == 0) 1 else 0
        _isAttackButtonEnabled = true
        Log.d(TAG, "Attack animation completed")
    }

    //Apply damage to player or opponent
    fun applyDamage(isPlayer: Boolean, damage: Float) {
        if (isPlayer) {
            _playerCurrentHP = (_playerCurrentHP - damage).coerceAtLeast(0f)
            Log.d(TAG, "Player took $damage damage. HP: ${_playerCurrentHP}/${_playerMaxHP}")
        } else {
            _opponentCurrentHP = (_opponentCurrentHP - damage).coerceAtLeast(0f)
            Log.d(TAG, "Opponent took $damage damage. HP: ${_opponentCurrentHP}/${_opponentMaxHP}")
        }
    }

    //Update critical bar progress
    fun updateCritBarProgress(progress: Int) {
        _critBarProgress = progress
    }


    //Check if battle is over
    fun isBattleOver(): Boolean {
        return _playerCurrentHP <= 0f || _opponentCurrentHP <= 0f
    }

    //Get battle winner
    fun getWinner(): String? {
        return when {
            _playerCurrentHP <= 0f -> "opponent"
            _opponentCurrentHP <= 0f -> "player"
            else -> null
        }
    }

    //Reset battle state
    fun resetBattle() {
        _playerCurrentHP = _playerMaxHP
        _opponentCurrentHP = _opponentMaxHP
        _isAttacking = false
        _attackProgress = 0f
        _currentView = 0
        _isAttackVisible = false
        _critBarProgress = 0
        _isAttackButtonEnabled = true
        Log.d(TAG, "Battle reset")
    }

    //Clean up resources
    fun cleanup() {
        _isAttacking = false
        _isAttackVisible = false
        _attackProgress = 0f
        Log.d(TAG, "Battle system cleaned up")
    }
}

@Composable
fun BattleScreen(
    battleSystem: ArenaBattleSystem,
    stage: String = "rookie",
    playerName: String = "Player",
    opponentName: String = "Opponent",
    onBattleComplete: (String?) -> Unit = {},
    onExitBattle: () -> Unit = {}
) {
    var animationProgress by remember { mutableStateOf(0f) }

    // Critical bar timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            if (!battleSystem.isAttacking) {
                battleSystem.updateCritBarProgress((battleSystem.critBarProgress + 5) % 101)
            }
        }
    }

    // Attack animation
    LaunchedEffect(battleSystem.isAttacking) {
        if (battleSystem.isAttacking) {
            animationProgress = 0f
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(ArenaBattleSystem.ANIMATION_DURATION.toInt())
            ) { value, _ ->
                animationProgress = value
                battleSystem.updateAttackAnimation(value)
            }
            battleSystem.completeAttackAnimation()

            // Check if battle is over
            if (battleSystem.isBattleOver()) {
                onBattleComplete(battleSystem.getWinner())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (battleSystem.currentView) {
            0 -> PlayerBattleView(
                battleSystem = battleSystem,
                stage = stage,
                playerName = playerName,
                attackAnimationProgress = animationProgress,
                onAttackClick = {
                    battleSystem.startPlayerAttack()
                    // Apply damage after animation
                    battleSystem.applyDamage(false, 20f) // Opponent takes damage
                }
            )
            1 -> OpponentBattleView(
                battleSystem = battleSystem,
                stage = stage,
                opponentName = opponentName,
                attackAnimationProgress = animationProgress
            )
        }

        // Exit button
        Button(
            onClick = onExitBattle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Exit", color = Color.White)
        }
    }
}

@Composable
fun PlayerBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    playerName: String,
    attackAnimationProgress: Float,
    onAttackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Health display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Current HP", color = Color.White, fontSize = 12.sp)
                Text(
                    text = battleSystem.playerCurrentHP.toInt().toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text("Max HP", color = Color.White, fontSize = 12.sp)
                Text(
                    text = battleSystem.playerMaxHP.toInt().toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Health bar
        LinearProgressIndicator(
            progress = (battleSystem.playerCurrentHP / battleSystem.playerMaxHP).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            color = Color.Green,
            trackColor = Color.Gray
        )

        // Player character with attack animation
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    when (stage) {
                        "rookie" -> R.drawable.agumon
                        "champion" -> R.drawable.greymon
                        "ultimate" -> R.drawable.doruguremon
                        "mega" -> R.drawable.machinedramon
                        else -> R.drawable.agumon
                    }
                ),
                contentDescription = "Player Character",
                modifier = Modifier
                    .size(120.dp)
                    .scale(2f),
                contentScale = ContentScale.Fit
            )

            // Attack animation overlay
            if (attackAnimationProgress > 0) {
                Image(
                    painter = painterResource(R.drawable.atk_l_00),
                    contentDescription = "Attack Animation",
                    modifier = Modifier
                        .size(60.dp)
                        .offset(
                            x = (attackAnimationProgress * 200 - 100).dp,
                            y = 0.dp
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Critical bar
        LinearProgressIndicator(
            progress = battleSystem.critBarProgress / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = Color.Yellow,
            trackColor = Color.Gray
        )

        // Attack button
        Button(
            onClick = onAttackClick,
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

@Composable
fun OpponentBattleView(
    battleSystem: ArenaBattleSystem,
    stage: String,
    opponentName: String,
    attackAnimationProgress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Health display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Current HP", color = Color.White, fontSize = 12.sp)
                Text(
                    text = battleSystem.opponentCurrentHP.toInt().toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text("Max HP", color = Color.White, fontSize = 12.sp)
                Text(
                    text = battleSystem.opponentMaxHP.toInt().toString(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Health bar
        LinearProgressIndicator(
            progress = (battleSystem.opponentCurrentHP / battleSystem.opponentMaxHP).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            color = Color.Green,
            trackColor = Color.Gray
        )

        // Opponent character with attack animation
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    when (stage) {
                        "rookie" -> R.drawable.agumon
                        "champion" -> R.drawable.greymon
                        "ultimate" -> R.drawable.doruguremon
                        "mega" -> R.drawable.machinedramon
                        else -> R.drawable.agumon
                    }
                ),
                contentDescription = "Opponent Character",
                modifier = Modifier
                    .size(120.dp)
                    .scale(-2f, 2f), // Flip horizontally
                contentScale = ContentScale.Fit
            )

            // Attack animation overlay
            if (attackAnimationProgress > 0) {
                Image(
                    painter = painterResource(R.drawable.atk_l_00),
                    contentDescription = "Attack Animation",
                    modifier = Modifier
                        .size(60.dp)
                        .offset(
                            x = (-attackAnimationProgress * 200 + 100).dp,
                            y = 0.dp
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

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

    var expanded by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf("") }

    val context = LocalContext.current

    val rookieButton = @Composable {
        Button(
            onClick = {
                //println("Rookie button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "rookie") { opponents ->
                        //println("API call completed successfully")
                        try {
                            //println("Received opponents data: $opponents")
                            //println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            //for (opponent in opponents.opponentsList) {
                            //    println("Opponent: ${opponent.name}")
                            //}

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            //println("Updated opponentsList size: ${opponentsList.size}")
                            //println("About to change view to rookie")
                            currentView = "rookie"
                            //println("View changed to rookie")
                        } catch (e: Exception) {
                            //println("Error processing opponents data: ${e.message}")
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    //println("Error calling getOpponents: ${e.message}")
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
                //println("Champion button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "champion") { opponents ->
                        //println("API call completed successfully")
                        try {
                            //println("Received opponents data: $opponents")
                            //println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            //for (opponent in opponents.opponentsList) {
                            //    println("Opponent: ${opponent.name}")
                            //}

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            //println("Updated opponentsList size: ${opponentsList.size}")
                            //println("About to change view to champion")
                            currentView = "champion"
                            //println("View changed to champion")
                        } catch (e: Exception) {
                            //println("Error processing opponents data: ${e.message}")
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    //println("Error calling getOpponents: ${e.message}")
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
                //println("Ultimate button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "ultimate") { opponents ->
                        //println("API call completed successfully")
                        try {
                            //println("Received opponents data: $opponents")
                            //println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            //for (opponent in opponents.opponentsList) {
                            //    println("Opponent: ${opponent.name}")
                            //}

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            //println("Updated opponentsList size: ${opponentsList.size}")
                            //println("About to change view to ultimate")
                            currentView = "ultimate"
                            //println("View changed to ultimate")
                        } catch (e: Exception) {
                            //println("Error processing opponents data: ${e.message}")
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    //println("Error calling getOpponents: ${e.message}")
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
                //println("Mega button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "mega") { opponents ->
                        //println("API call completed successfully")
                        try {
                            //println("Received opponents data: $opponents")
                            //println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            //for (opponent in opponents.opponentsList) {
                            //    println("Opponent: ${opponent.name}")
                            //}

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            //println("Updated opponentsList size: ${opponentsList.size}")
                            //println("About to change view to mega")
                            currentView = "mega"
                            //println("View changed to mega")
                        } catch (e: Exception) {
                            //println("Error processing opponents data: ${e.message}")
                            Log.d(TAG, "Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    //println("Error calling getOpponents: ${e.message}")
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
            onClick = {
                currentView = "main"
            }
        ) {
            Text("Back")
        }
    }

    val characterDropdown = @Composable { currentStage: String ->
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
                                        RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 0, 0, opponent.name, 0) { apiResult ->
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
                                        RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 1, 0, opponent.name, 1) { apiResult ->
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
                                        RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 2, 0, opponent.name, 2) { apiResult ->
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
                                        RetrofitHelper().getPVPWinner(context, 0, 2, it.name, 3, 0, opponent.name, 3) { apiResult ->
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

                }

                "battle-results" -> {

                }
            }
        }
    }
}
