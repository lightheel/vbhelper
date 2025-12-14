package com.github.nacabaro.vbhelper.screens.cardScreen.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.nacabaro.vbhelper.di.VBHelper
import com.github.nacabaro.vbhelper.dtos.CharacterDtos
import com.github.nacabaro.vbhelper.source.DexRepository
import com.github.nacabaro.vbhelper.utils.BitmapData
import com.github.nacabaro.vbhelper.utils.getImageBitmap
import androidx.compose.ui.res.stringResource
import com.github.nacabaro.vbhelper.R

@Composable
fun DexCharaDetailsDialog(
    currentChara: CharacterDtos.CardCharaProgress,
    obscure: Boolean,
    onClickClose: () -> Unit
) {
    val nameMultiplier = 3
    val charaMultiplier = 4

    val application = LocalContext.current.applicationContext as VBHelper
    val database = application.container.db
    val dexRepository = DexRepository(database)

    var showFusions by remember { mutableStateOf(false) }

    val currentCharaPossibleTransformations by dexRepository
        .getCharacterPossibleTransformations(currentChara.id)
        .collectAsState(emptyList())

    val currentCharaPossibleFusions by dexRepository
        .getCharacterPossibleFusions(currentChara.id)
        .collectAsState(emptyList())

    val romanNumeralsStage = when (currentChara.stage) {
        1 -> "II"
        2 -> "III"
        3 -> "IV"
        4 -> "V"
        5 -> "VI"
        6 -> "VII"
        else -> "I"
    }

    val charaBitmapData = BitmapData(
        bitmap = currentChara.spriteIdle,
        width = currentChara.spriteWidth,
        height = currentChara.spriteHeight
    )
    val charaImageBitmapData = charaBitmapData.getImageBitmap(
        context = LocalContext.current,
        multiplier = charaMultiplier,
        obscure = obscure
    )

    val nameBitmapData = BitmapData(
        bitmap = currentChara.nameSprite,
        width = currentChara.nameSpriteWidth,
        height = currentChara.nameSpriteHeight
    )
    val nameImageBitmapData = nameBitmapData.getImageBitmap(
        context = LocalContext.current,
        multiplier = nameMultiplier,
        obscure = obscure
    )

    Dialog(
        onDismissRequest = onClickClose
    ) {
        Card (
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column (
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                ) {
                    Card (
                        colors = CardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.contentColorFor(
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    ) {
                        Image(
                            bitmap = charaImageBitmapData.imageBitmap,
                            contentDescription = stringResource(R.string.dex_chara_icon_description),
                            modifier = Modifier
                                .size(charaImageBitmapData.dpWidth)
                                .padding(8.dp),
                            colorFilter = when (obscure) {
                                true -> ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
                                false -> null
                            },
                            filterQuality = FilterQuality.None
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .padding(16.dp)
                    )
                    if (!obscure) {
                        Column {
                            Image(
                                bitmap = nameImageBitmapData.imageBitmap,
                                contentDescription = stringResource(R.string.dex_chara_name_icon_description),
                                modifier = Modifier
                                    .width(nameImageBitmapData.dpWidth)
                                    .height(nameImageBitmapData.dpHeight),
                                filterQuality = FilterQuality.None
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            if (currentChara.baseHp != 65535) {
                                Text(
                                    text = stringResource(
                                        R.string.dex_chara_stats,
                                        currentChara.baseHp,
                                        currentChara.baseBp,
                                        currentChara.baseAp
                                    )
                                )
                                Text(
                                    text = stringResource(
                                        R.string.dex_chara_stage_attribute,
                                        romanNumeralsStage,
                                        currentChara.attribute.toString().substring(0, 2)
                                    )
                                )
                            }
                        }
                    } else {
                        Column {
                            Text(stringResource(R.string.dex_chara_unknown_name))
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(stringResource(R.string.dex_chara_stage_attribute_unknown))
                            Text(stringResource(R.string.dex_chara_stats_unknown))
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(16.dp))
                Column {
                    currentCharaPossibleTransformations.map {
                        val selectedCharaBitmap = BitmapData(
                            bitmap = it.spriteIdle,
                            width = it.spriteWidth,
                            height = it.spriteHeight
                        )
                        val selectedCharaImageBitmap = selectedCharaBitmap.getImageBitmap(
                            context = LocalContext.current,
                            multiplier = 4,
                            obscure = it.discoveredOn == null
                        )

                        Card (
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                        ) {
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Card (
                                    colors = CardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.contentColorFor(
                                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.contentColorFor(
                                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    )
                                ) {
                                    Image(
                                        bitmap = selectedCharaImageBitmap.imageBitmap,
                                        contentDescription = stringResource(R.string.dex_chara_icon_description),
                                        modifier = Modifier
                                            .size(selectedCharaImageBitmap.dpWidth)
                                            .padding(8.dp),
                                        colorFilter = when (it.discoveredOn == null) {
                                            true -> ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
                                            false -> null
                                        },
                                        filterQuality = FilterQuality.None
                                    )
                                }
                                Spacer(
                                    modifier = Modifier
                                        .padding(16.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(
                                            R.string.dex_chara_requirements,
                                            it.requiredTrophies,
                                            it.requiredBattles,
                                            it.requiredVitals,
                                            it.requiredWinRate,
                                            it.changeTimerHours
                                        )
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.dex_chara_adventure_level,
                                            it.requiredAdventureLevelCompleted + 1
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Row {
                    if (currentCharaPossibleFusions.isNotEmpty()) {
                        Button(
                            onClick = {
                                showFusions = true
                            }
                        ) {
                            Text(stringResource(R.string.dex_chara_fusions_button))
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .padding(4.dp)
                    )

                    Button(
                        onClick = onClickClose
                    ) {
                        Text(stringResource(R.string.dex_chara_close_button))
                    }
                }
            }
        }
    }

    if (showFusions) {
        DexCharaFusionsDialog(
            currentChara = currentChara,
            currentCharaPossibleFusions = currentCharaPossibleFusions,
            onClickDismiss = {
                showFusions = false
            },
            obscure = obscure
        )
    }
}