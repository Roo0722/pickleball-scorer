package com.pickleball.scorer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saves the current game to SharedPreferences so an app restart does not lose the score. */
class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences("pickleball_scorer", Context.MODE_PRIVATE)

    fun save(config: GameConfig, state: GameState?) {
        prefs.edit()
            .putString(KEY_CONFIG, configToJson(config).toString())
            .apply {
                if (state == null) remove(KEY_STATE) else putString(KEY_STATE, stateToJson(state).toString())
            }
            .apply()
    }

    fun loadConfig(): GameConfig? = try {
        prefs.getString(KEY_CONFIG, null)?.let { configFromJson(JSONObject(it)) }
    } catch (e: Exception) {
        null
    }

    fun loadState(): GameState? = try {
        prefs.getString(KEY_STATE, null)?.let { stateFromJson(JSONObject(it)) }
    } catch (e: Exception) {
        null
    }

    private fun configToJson(c: GameConfig): JSONObject = JSONObject().apply {
        put("mode", c.mode.name)
        put("points", c.pointsToWin)
        put("first", c.firstServer)
        put("switch", c.switchEnds)
        put("players", JSONArray().also { teams ->
            c.players.forEach { team -> teams.put(JSONArray().also { arr -> team.forEach { arr.put(it) } }) }
        })
    }

    private fun configFromJson(o: JSONObject): GameConfig {
        val teams = o.getJSONArray("players")
        val players = (0 until teams.length()).map { t ->
            val arr = teams.getJSONArray(t)
            (0 until arr.length()).map { arr.getString(it) }
        }
        return GameConfig(
            mode = Mode.valueOf(o.getString("mode")),
            players = players,
            pointsToWin = o.getInt("points"),
            firstServer = o.getInt("first"),
            switchEnds = o.optBoolean("switch", true),
        )
    }

    private fun stateToJson(s: GameState): JSONObject = JSONObject().apply {
        put("config", configToJson(s.config))
        put("score", JSONArray(s.score))
        put("serving", s.servingTeam)
        put("serverNumber", s.serverNumber)
        put("serverIdx", s.serverIdx)
        put("right", JSONArray(s.rightIdx))
        put("winner", s.winner ?: -1)
        put("swapped", s.swapped)
        put("pending", s.pendingSwitch)
    }

    private fun stateFromJson(o: JSONObject): GameState {
        val score = o.getJSONArray("score")
        val right = o.getJSONArray("right")
        val winner = o.getInt("winner")
        return GameState(
            config = configFromJson(o.getJSONObject("config")),
            score = listOf(score.getInt(0), score.getInt(1)),
            servingTeam = o.getInt("serving"),
            serverNumber = o.getInt("serverNumber"),
            serverIdx = o.getInt("serverIdx"),
            rightIdx = listOf(right.getInt(0), right.getInt(1)),
            winner = if (winner < 0) null else winner,
            swapped = o.optBoolean("swapped", false),
            pendingSwitch = o.optBoolean("pending", false),
        )
    }

    private companion object {
        const val KEY_CONFIG = "config"
        const val KEY_STATE = "state"
    }
}
