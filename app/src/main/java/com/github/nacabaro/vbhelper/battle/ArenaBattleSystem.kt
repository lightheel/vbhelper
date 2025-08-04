package com.github.nacabaro.vbhelper.battle

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ArenaBattleSystem {
    companion object {
        private const val TAG = "ArenaBattleSystem"
    }

    // Attack phases: 0=Idle, 1=Player attack on player screen, 2=Player attack on opponent screen, 
    // 3=Opponent attack on opponent screen, 4=Opponent attack on player screen
    private var _attackPhase by mutableStateOf(0)
    val attackPhase: Int get() = _attackPhase

    private var _attackProgress by mutableStateOf(0f)
    val attackProgress: Float get() = _attackProgress

    private var _isPlayerAttacking by mutableStateOf(false)
    val isPlayerAttacking: Boolean get() = _isPlayerAttacking

    private var _attackIsHit by mutableStateOf(false)
    val attackIsHit: Boolean get() = _attackIsHit

    private var _isAttackButtonEnabled by mutableStateOf(true)
    val isAttackButtonEnabled: Boolean get() = _isAttackButtonEnabled

    private var _currentView by mutableStateOf(0)
    val currentView: Int get() = _currentView

    private var _playerHP by mutableStateOf(100f)
    val playerHP: Float get() = _playerHP

    private var _opponentHP by mutableStateOf(100f)
    val opponentHP: Float get() = _opponentHP

    private var _isBattleOver by mutableStateOf(false)
    val isBattleOver: Boolean get() = _isBattleOver

    private var _critBarProgress by mutableStateOf(0)
    val critBarProgress: Int get() = _critBarProgress

    fun startPlayerAttack() {
        Log.d(TAG, "Starting player attack")
        _attackPhase = 1
        _attackProgress = 0f
        _isPlayerAttacking = true
        _isAttackButtonEnabled = false
        _currentView = 0
    }

    fun startOpponentAttack() {
        Log.d(TAG, "Starting opponent attack")
        _attackPhase = 3
        _attackProgress = 0f
        _isPlayerAttacking = false
        _currentView = 1
    }

    fun advanceAttackPhase() {
        _attackPhase++
        _attackProgress = 0f
        Log.d(TAG, "Advanced to attack phase: $_attackPhase")
    }

    fun setAttackProgress(progress: Float) {
        _attackProgress = progress
    }

    fun setAttackHitState(isHit: Boolean) {
        _attackIsHit = isHit
    }

    fun switchToView(view: Int) {
        _currentView = view
        Log.d(TAG, "Switched to view: $view")
    }

    fun enableAttackButton() {
        _isAttackButtonEnabled = true
        Log.d(TAG, "Attack button enabled")
    }

    fun applyDamage(isPlayer: Boolean, damage: Float) {
        if (isPlayer) {
            _playerHP = (_playerHP - damage).coerceAtLeast(0f)
        } else {
            _opponentHP = (_opponentHP - damage).coerceAtLeast(0f)
        }
        Log.d(TAG, "Applied damage: ${if (isPlayer) "player" else "opponent"} -$damage")
    }

    fun updateHPFromAPI(playerHP: Float, opponentHP: Float) {
        _playerHP = playerHP
        _opponentHP = opponentHP
        Log.d(TAG, "Updated HP from API: Player=$playerHP, Opponent=$opponentHP")
    }

    fun initializeHP(playerMaxHP: Float, opponentMaxHP: Float) {
        _playerHP = playerMaxHP
        _opponentHP = opponentMaxHP
        Log.d(TAG, "Initialized HP: Player=$playerMaxHP, Opponent=$opponentMaxHP")
    }

    fun completeAttackAnimation(playerDamage: Float = 0f, opponentDamage: Float = 0f) {
        if (playerDamage > 0f) {
            applyDamage(true, playerDamage)
        }
        if (opponentDamage > 0f) {
            applyDamage(false, opponentDamage)
        }
        Log.d(TAG, "Completed attack animation with damage: Player=$playerDamage, Opponent=$opponentDamage")
    }

    fun resetAttackState() {
        _attackPhase = 0
        _attackProgress = 0f
        _isPlayerAttacking = false
        _attackIsHit = false
        _currentView = 0
        Log.d(TAG, "Reset attack state")
    }

    fun checkBattleOver(): Boolean {
        return _playerHP <= 0f || _opponentHP <= 0f
    }

    fun endBattle() {
        _isBattleOver = true
        Log.d(TAG, "Battle ended")
    }

    fun updateCritBarProgress(progress: Int) {
        _critBarProgress = progress
        //Log.d(TAG, "Updated crit bar progress: $progress")
    }
} 