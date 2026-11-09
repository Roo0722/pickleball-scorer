package com.pickleball.scorer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val doubles = GameConfig(
        Mode.DOUBLES,
        listOf(listOf("A1", "A2"), listOf("B1", "B2")),
    )
    private val singles = GameConfig(
        Mode.SINGLES,
        listOf(listOf("A"), listOf("B")),
    )

    @Test
    fun doublesStartsAtZeroZeroTwoWithServerOnRight() {
        val s = GameEngine.newGame(doubles)
        assertEquals("0 – 0 – 2", s.scoreCall())
        assertTrue(s.serverRight())
        assertEquals("A1", s.serverName())
        assertEquals("B1", s.receiverName())
    }

    @Test
    fun openingServerFaultIsImmediateSideOut() {
        val s = GameEngine.newGame(doubles).rallyWon(1)
        assertEquals(1, s.servingTeam)
        assertEquals(1, s.serverNumber)
        assertEquals(listOf(0, 0), s.score)
        assertEquals("B1", s.serverName()) // B1 stands on the right
        assertEquals("0 – 0 – 1", s.scoreCall())
    }

    @Test
    fun scoringSwapsServerCourtAndReceiverIsDiagonal() {
        val s = GameEngine.newGame(doubles).rallyWon(0)
        assertEquals("1 – 0 – 2", s.scoreCall())
        assertFalse(s.serverRight())
        assertEquals("A1", s.serverName())
        // A2 moved to the right court
        assertEquals(1, s.rightIdx[0])
        // server on left -> receiver is the player in B's left court (B2)
        assertEquals("B2", s.receiverName())
    }

    @Test
    fun firstServerFaultGivesPartnerSecondServeWithoutMoving() {
        val sideOut = GameEngine.newGame(doubles).rallyWon(1) // B serving, B1 server 1
        val s = sideOut.rallyWon(0) // B1 faults
        assertEquals(1, s.servingTeam)
        assertEquals(2, s.serverNumber)
        assertEquals("B2", s.serverName())
        assertEquals(sideOut.rightIdx, s.rightIdx)
        assertEquals(listOf(0, 0), s.score)
    }

    @Test
    fun secondServerFaultIsSideOutAndRightCourtPlayerServes() {
        var s = GameEngine.newGame(doubles).rallyWon(0) // A scores: 1-0-2, A1 left, A2 right
        s = s.rallyWon(1) // A1 (server 2 at start) faults -> side out to B
        assertEquals(1, s.servingTeam)
        assertEquals(1, s.serverNumber)
        assertEquals("B1", s.serverName())
        s = s.rallyWon(0) // B1 faults -> B2 server 2
        s = s.rallyWon(0) // B2 faults -> side out to A
        assertEquals(0, s.servingTeam)
        assertEquals(1, s.serverNumber)
        assertEquals("A2", s.serverName()) // A2 is in the right court after A's point
        assertEquals("1 – 0 – 1", s.scoreCall())
        assertTrue(s.serverRight())
    }

    @Test
    fun gameEndsAtElevenWithTwoPointLead() {
        var s = GameEngine.newGame(doubles)
        repeat(10) { s = s.rallyWon(0) }
        assertNull(s.winner)
        assertTrue(s.isGamePoint())
        s = s.rallyWon(0)
        assertEquals(0, s.winner)
        assertEquals(11, s.score[0])
    }

    @Test
    fun mustWinByTwo() {
        var s = GameEngine.newGame(doubles).copy(score = listOf(10, 10))
        s = s.rallyWon(0)
        assertNull(s.winner) // 11-10 is not enough
        s = s.rallyWon(0)
        assertEquals(0, s.winner)
        assertEquals(12, s.score[0])
    }

    @Test
    fun endsSwitchOnceAtMidpointForElevenPointGame() {
        var s = GameEngine.newGame(doubles)
        repeat(5) { s = s.rallyWon(0) }
        assertFalse(s.swapped)
        s = s.rallyWon(0) // 6 points
        assertTrue(s.swapped)
        assertTrue(s.pendingSwitch)
        assertEquals(6, s.switchAt)
        s = s.rallyWon(0)
        assertFalse(s.pendingSwitch)
        assertTrue(s.swapped) // stays switched, never switches back
    }

    @Test
    fun midpointIsEightForFifteenAndElevenForTwentyOne() {
        var s15 = GameEngine.newGame(doubles.copy(pointsToWin = 15))
        repeat(7) { s15 = s15.rallyWon(0) }
        assertFalse(s15.swapped)
        s15 = s15.rallyWon(0)
        assertTrue(s15.swapped)

        var s21 = GameEngine.newGame(doubles.copy(pointsToWin = 21))
        repeat(10) { s21 = s21.rallyWon(0) }
        assertFalse(s21.swapped)
        s21 = s21.rallyWon(0)
        assertTrue(s21.swapped)
    }

    @Test
    fun switchCanBeDisabled() {
        var s = GameEngine.newGame(doubles.copy(switchEnds = false))
        repeat(8) { s = s.rallyWon(0) }
        assertFalse(s.swapped)
        assertFalse(s.pendingSwitch)
    }

    @Test
    fun eventsAfterGameOverAreIgnored() {
        var s = GameEngine.newGame(doubles).copy(score = listOf(10, 3))
        s = s.rallyWon(0)
        assertEquals(0, s.winner)
        val after = s.rallyWon(1)
        assertEquals(s, after)
    }

    @Test
    fun singlesServeSideFollowsOwnScoreParityAndFaultIsImmediateSideOut() {
        var s = GameEngine.newGame(singles)
        assertEquals("0 – 0", s.scoreCall())
        assertTrue(s.serverRight())
        s = s.rallyWon(0)
        assertEquals("1 – 0", s.scoreCall())
        assertFalse(s.serverRight())
        s = s.rallyWon(1) // server loses
        assertEquals(1, s.servingTeam)
        assertEquals(listOf(1, 0), s.score)
        assertEquals("0 – 1", s.scoreCall())
        assertTrue(s.serverRight()) // B has 0 points -> right court
        assertEquals("A", s.receiverName())
    }
}
