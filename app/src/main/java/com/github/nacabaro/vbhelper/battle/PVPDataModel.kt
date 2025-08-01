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
    val winner: String
):java.io.Serializable