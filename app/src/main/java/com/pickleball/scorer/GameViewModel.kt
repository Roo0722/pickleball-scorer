package com.pickleball.scorer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GameStore(app)

    var config by mutableStateOf(store.loadConfig() ?: GameConfig.default())
        private set

    /** null = show the setup screen. */
    var state by mutableStateOf(store.loadState())
        private set

    /** Previous states, for undo. */
    val history = mutableStateListOf<GameState>()

    fun start(newConfig: GameConfig) {
        config = newConfig
        history.clear()
        state = GameEngine.newGame(newConfig)
        persist()
    }

    /** [team] is the team that won the rally. */
    fun rallyWon(team: Int) {
        val current = state ?: return
        if (current.winner != null) return
        history.add(current)
        if (history.size > 300) history.removeAt(0)
        state = current.rallyWon(team)
        persist()
    }

    fun undo() {
        if (history.isEmpty()) return
        state = history.removeAt(history.lastIndex)
        persist()
    }

    /** Same players, the other team serves first. */
    fun rematch() {
        start(config.copy(firstServer = 1 - config.firstServer))
    }

    fun backToSetup() {
        history.clear()
        state = null
        persist()
    }

    private fun persist() = store.save(config, state)
}
