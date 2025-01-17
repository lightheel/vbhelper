package com.github.nacabaro.vbhelper.screens.settingsScreen

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.navigation.AppNavigation
import com.github.nacabaro.vbhelper.navigation.NavigationItems

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsScreenController: SettingsScreenController,
    newSettingsScreenController: NewSettingsScreenControllerImpl,
    onClickImportCard: () -> Unit
) {
    Scaffold (
        topBar = {
            TopBanner(
                text = "Settings",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
    ) { contentPadding ->
        Column (
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("NFC Communication")
            SettingsEntry(title = "Import APK", description = "Import Secrets From Vital Arean 2.1.0 APK") {
                settingsScreenController.apkFilePickLauncher.launch(arrayOf("*/*"))
            }
            SettingsSection("Data management")
            SettingsEntry(title = "Export data", description = "Export application database") {
                newSettingsScreenController.onClickOpenDirectory()
            }
            SettingsEntry(title = "Import data", description = "Import application database") {
                newSettingsScreenController.onClickImportDatabase()
            }
            SettingsSection("DiM/BEm management")
            SettingsEntry(title = "Import DiM card", description = "Import DiM/BEm card file", onClick = onClickImportCard)
            SettingsEntry(title = "Rename DiM/BEm", description = "Set card name") { }
            SettingsSection("About and credits")
            SettingsEntry(title = "Credits", description = "Credits") { }
            SettingsEntry(title = "About", description = "About") { }
        }
    }
}

fun buildFilePickLauncher(activity: ComponentActivity, onItemPicked: (Uri?) -> Unit): ActivityResultLauncher<Array<String>> {
    return activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        onItemPicked.invoke(it)
    }
}

@Composable
fun SettingsEntry(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(text = title)
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SettingsSection(
    title: String
) {
    Box (
        modifier = Modifier
            .padding(start = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}