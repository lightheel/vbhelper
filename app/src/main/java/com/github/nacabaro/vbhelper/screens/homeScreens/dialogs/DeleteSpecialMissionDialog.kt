package com.github.nacabaro.vbhelper.screens.homeScreens.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.nacabaro.vbhelper.R

@Composable
fun DeleteSpecialMissionDialog(
    onClickDismiss: () -> Unit,
    onClickDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onClickDismiss
    ) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_special_mission_delete_main),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Row {
                    Button(
                        onClick = onClickDismiss,
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        Text(text = stringResource(R.string.home_special_mission_delete_dismiss))
                    }

                    Button(
                        onClick = onClickDelete,
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        Text(text = stringResource(R.string.home_special_mission_delete_remove))
                    }
                }
            }
        }
    }
}