package com.github.nacabaro.vbhelper.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import com.github.nacabaro.vbhelper.battle.APIBattleCharacter
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.battle.RetrofitHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen() {
    var currentView by remember { mutableStateOf("main") }

    var opponentsList by remember { mutableStateOf(ArrayList<APIBattleCharacter>()) }

    var activeCharacter by remember { mutableStateOf<APIBattleCharacter?>(null) }

    var expanded by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf("") }

    val context = LocalContext.current

    val rookieButton = @Composable {
        Button(
            onClick = {
                println("Rookie button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "rookie") { opponents ->
                        println("API call completed successfully")
                        try {
                            println("Received opponents data: $opponents")
                            println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            for (opponent in opponents.opponentsList) {
                                println("Opponent: ${opponent.name}")
                            }

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            println("Updated opponentsList size: ${opponentsList.size}")
                            println("About to change view to rookie")
                            currentView = "rookie"
                            println("View changed to rookie")
                        } catch (e: Exception) {
                            println("Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("Error calling getOpponents: ${e.message}")
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
                println("Champion button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "champion") { opponents ->
                        println("API call completed successfully")
                        try {
                            println("Received opponents data: $opponents")
                            println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            for (opponent in opponents.opponentsList) {
                                println("Opponent: ${opponent.name}")
                            }

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            println("Updated opponentsList size: ${opponentsList.size}")
                            println("About to change view to champion")
                            currentView = "champion"
                            println("View changed to champion")
                        } catch (e: Exception) {
                            println("Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("Error calling getOpponents: ${e.message}")
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
                println("Ultimate button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "ultimate") { opponents ->
                        println("API call completed successfully")
                        try {
                            println("Received opponents data: $opponents")
                            println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            for (opponent in opponents.opponentsList) {
                                println("Opponent: ${opponent.name}")
                            }

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            println("Updated opponentsList size: ${opponentsList.size}")
                            println("About to change view to ultimate")
                            currentView = "ultimate"
                            println("View changed to ultimate")
                        } catch (e: Exception) {
                            println("Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("Error calling getOpponents: ${e.message}")
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
                println("Mega button clicked - starting API call")
                try {
                    RetrofitHelper().getOpponents(context, "mega") { opponents ->
                        println("API call completed successfully")
                        try {
                            println("Received opponents data: $opponents")
                            println("Opponents list size: ${opponents.opponentsList.size}")

                            // For loop to check opponents and print their names
                            for (opponent in opponents.opponentsList) {
                                println("Opponent: ${opponent.name}")
                            }

                            // Store the opponents in your ArrayList
                            opponentsList.clear()
                            opponentsList.addAll(opponents.opponentsList)

                            println("Updated opponentsList size: ${opponentsList.size}")
                            println("About to change view to mega")
                            currentView = "mega"
                            println("View changed to mega")
                        } catch (e: Exception) {
                            println("Error processing opponents data: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("Error calling getOpponents: ${e.message}")
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
            }
        }
    }
}
