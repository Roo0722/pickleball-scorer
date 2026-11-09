package com.pickleball.scorer

/**
 * Pure Kotlin pickleball side-out scoring engine (no Android imports, so it is unit-testable).
 *
 * Rules implemented (USA Pickleball traditional scoring):
 *  - Only the serving side scores. Games to [GameConfig.pointsToWin], win by 2.
 *  - Doubles call = serverScore - receiverScore - serverNumber (1 or 2). Singles = two numbers.
 *  - Game starts 0-0-2: the first serving team gets only ONE server before a side out.
 *  - Doubles: server 1 faults -> partner serves (server 2); server 2 faults -> side out.
 *  - After a side out the player standing in the RIGHT court of the new serving team serves first (server 1).
 *  - The serving team's two players swap courts ONLY when they score a point. Nobody swaps on a fault/side out.
 *  - Singles: server serves from the right court when own score is even, left when odd. Side out on every fault.
 *  - Serve is always diagonal: server in right court -> receiver in the opponents' right court (their own right).
 */
enum class Mode { SINGLES, DOUBLES }

data class GameConfig(
    val mode: Mode,
    /** players[team][index]; singles teams have exactly 1 player, doubles teams exactly 2. */
    val players: List<List<String>>,
    val pointsToWin: Int = 11,
    val firstServer: Int = 0,
    /** Teams swap ends once, when the first team reaches the midpoint (6 of 11, 8 of 15, 11 of 21). */
    val switchEnds: Boolean = true,
) {
    companion object {
        fun default() = GameConfig(
            mode = Mode.DOUBLES,
            players = listOf(listOf("Player 1", "Player 2"), listOf("Player 3", "Player 4")),
        )
    }
}

data class GameState(
    val config: GameConfig,
    val score: List<Int>,
    val servingTeam: Int,
    /** 1 or 2 in doubles (always 1 in singles). */
    val serverNumber: Int,
    /** Index (within the serving team) of the player currently serving. */
    val serverIdx: Int,
    /** For each team: index of the player standing in the RIGHT court (doubles only). */
    val rightIdx: List<Int>,
    val winner: Int? = null,
    /** True once the teams have switched ends (team 1 then plays on the right half of the diagram). */
    val swapped: Boolean = false,
    /** True right after the midpoint is reached, until the scorer confirms the switch. */
    val pendingSwitch: Boolean = false,
) {
    val isDoubles: Boolean get() = config.mode == Mode.DOUBLES
    val receivingTeam: Int get() = 1 - servingTeam

    /** Score at which the teams switch ends: 6 for 11, 8 for 15, 11 for 21. */
    val switchAt: Int get() = (config.pointsToWin + 1) / 2

    /** True when the server stands in the right (even) court. */
    fun serverRight(): Boolean =
        if (isDoubles) rightIdx[servingTeam] == serverIdx else score[servingTeam] % 2 == 0

    /** Index (within the receiving team) of the player receiving the serve (diagonal). */
    fun receiverIdx(): Int =
        if (!isDoubles) 0
        else if (serverRight()) rightIdx[receivingTeam] else 1 - rightIdx[receivingTeam]

    fun serverName(): String = config.players[servingTeam][serverIdx]
    fun receiverName(): String = config.players[receivingTeam][receiverIdx()]

    fun teamLabel(team: Int): String = config.players[team].joinToString(" & ")

    /** The call said out loud before each serve, e.g. "4-3-2" (doubles) or "4-3" (singles). */
    fun scoreCall(): String {
        val s = score[servingTeam]
        val r = score[receivingTeam]
        return if (isDoubles) "$s – $r – $serverNumber" else "$s – $r"
    }

    /** True when the next point by the serving team would win the game. */
    fun isGamePoint(): Boolean {
        if (winner != null) return false
        val s = score[servingTeam] + 1
        val o = score[receivingTeam]
        return s >= config.pointsToWin && s - o >= 2
    }

    /** The team that won the rally. */
    fun rallyWon(team: Int): GameState {
        if (winner != null) return this
        return if (team == servingTeam) scorePoint() else serverLost()
    }

    private fun scorePoint(): GameState {
        val newScore = score.toMutableList()
        newScore[servingTeam] = newScore[servingTeam] + 1
        val s = newScore[servingTeam]
        val o = newScore[receivingTeam]
        val won = s >= config.pointsToWin && s - o >= 2
        val newRight = rightIdx.toMutableList()
        if (isDoubles) newRight[servingTeam] = 1 - newRight[servingTeam]
        val doSwitch = config.switchEnds && !swapped && !won && s >= (config.pointsToWin + 1) / 2
        return copy(
            score = newScore,
            rightIdx = newRight,
            winner = if (won) servingTeam else null,
            swapped = swapped || doSwitch,
            pendingSwitch = doSwitch,
        )
    }

    private fun serverLost(): GameState {
        if (!isDoubles) {
            return copy(servingTeam = receivingTeam, serverNumber = 1, serverIdx = 0)
        }
        if (serverNumber == 1) {
            // Partner becomes server 2, no position change.
            return copy(serverNumber = 2, serverIdx = 1 - serverIdx)
        }
        // Server 2 lost (or opening 0-0-2 server lost): side out. Player in the right court serves first.
        val next = receivingTeam
        return copy(servingTeam = next, serverNumber = 1, serverIdx = rightIdx[next])
    }
}

object GameEngine {
    fun newGame(config: GameConfig): GameState = GameState(
        config = config,
        score = listOf(0, 0),
        servingTeam = config.firstServer,
        serverNumber = if (config.mode == Mode.DOUBLES) 2 else 1,
        serverIdx = 0, // player 0 starts in the right court
        rightIdx = listOf(0, 0),
    )
}
