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
import com.github.nacabaro.vbhelper.components.TopBanner
import androidx.compose.ui.res.stringResource
import com.github.nacabaro.vbhelper.R


@Composable
fun BattlesScreen() {
    Scaffold (
        topBar = {
            TopBanner(
                text = stringResource(R.string.battles_online_title)
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
            Text(stringResource(R.string.battles_coming_soon))
        }
    }
}
