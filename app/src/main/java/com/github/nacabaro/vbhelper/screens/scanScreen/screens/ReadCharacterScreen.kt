package com.github.nacabaro.vbhelper.screens.scanScreen.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.nacabaro.vbhelper.components.TopBanner
import com.github.nacabaro.vbhelper.R

@Composable
fun ReadCharacterScreen(
    onClickCancel: () -> Unit,
    onClickConfirm: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBanner(
                text = stringResource(R.string.read_character_title),
                onBackClick = onClickCancel
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.read_character_prepare_device),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.read_character_go_to_connect),
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.padding(8.dp)
            )

            Button(
                onClick = onClickConfirm,
            ) {
                Text(stringResource(R.string.read_character_confirm))
            }
        }
    }
}