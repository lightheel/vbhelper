package com.github.nacabaro.vbhelper.battle

data class PVPDataModel (
    val status: String,
    val state: Int,
    val currentRound: Int,
    val playerHP: Int,
    val opponentHP: Int,
    val playerAttackHit: Boolean,
    val playerAttackDamage: Int,
    val opponentAttackDamage: Int,
    val winner: String,
    val opponentCharaId: String? = null  // TODO: Server will add this - opponent's charaId from the match
):java.io.Serializable