package com.pickleball.scorer

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF0B1220)
val Panel = Color(0xFF16202F)
val Accent = Color(0xFFD7F03A)
val TeamBlue = Color(0xFF1E6FD9)
val TeamOrange = Color(0xFFE8650A)
val CourtGreen = Color(0xFF1F6B5C)
val KitchenGreen = Color(0xFF2A8A76)
val ReceiverGreen = Color(0xFF69F0AE)
val Muted = Color(0xFFB0BEC5)

private fun teamColor(team: Int) = if (team == 0) TeamBlue else TeamOrange

@Composable
fun AppRoot(vm: GameViewModel) {
    val state = vm.state
    val activity = LocalContext.current as? Activity
    val scoring = state != null
    // Setup = portrait, scoring = landscape.
    LaunchedEffect(scoring) {
        activity?.requestedOrientation = if (scoring) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    if (state == null) {
        SetupScreen(initial = vm.config, onStart = vm::start)
    } else {
        ScoreScreen(vm, state)
    }
}

// ───────────────────────────── Setup ─────────────────────────────

@Composable
fun SetupScreen(initial: GameConfig, onStart: (GameConfig) -> Unit) {
    var doubles by rememberSaveable { mutableStateOf(initial.mode == Mode.DOUBLES) }
    var n00 by rememberSaveable { mutableStateOf(initial.players[0].getOrElse(0) { "" }) }
    var n01 by rememberSaveable { mutableStateOf(initial.players[0].getOrElse(1) { "" }) }
    var n10 by rememberSaveable { mutableStateOf(initial.players[1].getOrElse(0) { "" }) }
    var n11 by rememberSaveable { mutableStateOf(initial.players[1].getOrElse(1) { "" }) }
    var points by rememberSaveable { mutableStateOf(initial.pointsToWin) }
    var first by rememberSaveable { mutableStateOf(initial.firstServer) }
    var switchEnds by rememberSaveable { mutableStateOf(initial.switchEnds) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pickleball Scorer", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Accent)

        Label("Game type")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Choice("Doubles", doubles) { doubles = true }
            Choice("Singles", !doubles) { doubles = false }
        }

        Label("Play to")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(11, 15, 21).forEach { p -> Choice("$p", points == p) { points = p } }
        }

        TeamEditor("TEAM 1", TeamBlue, doubles, n00, { n00 = it }, n01, { n01 = it }, Modifier.fillMaxWidth())
        TeamEditor("TEAM 2", TeamOrange, doubles, n10, { n10 = it }, n11, { n11 = it }, Modifier.fillMaxWidth())

        Label("First serve")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Choice("Team 1", first == 0) { first = 0 }
            Choice("Team 2", first == 1) { first = 1 }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Switch ends at midpoint", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Teams swap sides when a team reaches ${(points + 1) / 2} (game to $points)",
                    fontSize = 13.sp,
                    color = Muted,
                )
            }
            Switch(checked = switchEnds, onCheckedChange = { switchEnds = it })
        }

        Button(
            onClick = {
                fun nm(v: String, d: String) = v.trim().ifEmpty { d }
                val players = if (doubles) {
                    listOf(
                        listOf(nm(n00, "Player 1"), nm(n01, "Player 2")),
                        listOf(nm(n10, "Player 3"), nm(n11, "Player 4")),
                    )
                } else {
                    listOf(listOf(nm(n00, "Player 1")), listOf(nm(n10, "Player 2")))
                }
                onStart(
                    GameConfig(
                        mode = if (doubles) Mode.DOUBLES else Mode.SINGLES,
                        players = players,
                        pointsToWin = points,
                        firstServer = first,
                        switchEnds = switchEnds,
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            Text("START GAME", fontSize = 22.sp, fontWeight = FontWeight.Black)
        }

        Text(
            "Side-out scoring · win by 2 · doubles starts at 0-0-2",
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted)
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Bg else Color.White,
        )
    }
}

@Composable
private fun TeamEditor(
    title: String,
    color: Color,
    doubles: Boolean,
    a: String,
    onA: (String) -> Unit,
    b: String,
    onB: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.25f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        OutlinedTextField(
            value = a,
            onValueChange = onA,
            label = { Text(if (doubles) "Player 1" else "Player name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (doubles) {
            OutlinedTextField(
                value = b,
                onValueChange = onB,
                label = { Text("Player 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ───────────────────────────── Scoreboard ─────────────────────────────

@Composable
fun ScoreScreen(vm: GameViewModel, s: GameState) {
    val haptic = LocalHapticFeedback.current
    var confirmNew by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .safeDrawingPadding()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TeamPanel(s, 0, Modifier.weight(1f).fillMaxHeight()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.rallyWon(0)
        }
        CenterColumn(vm, s, onNew = { confirmNew = true }, modifier = Modifier.weight(1.7f).fillMaxHeight())
        TeamPanel(s, 1, Modifier.weight(1f).fillMaxHeight()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.rallyWon(1)
        }
    }

    val w = s.winner
    if (w != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game over") },
            text = {
                Column {
                    Text(
                        "${s.teamLabel(w)} ${if (s.isDoubles) "win" else "wins"} ${s.score[w]}–${s.score[1 - w]}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = vm::undo) { Text("↶ Undo last point") }
                }
            },
            confirmButton = { TextButton(onClick = vm::rematch) { Text("Rematch") } },
            dismissButton = { TextButton(onClick = vm::backToSetup) { Text("New setup") } },
        )
    }

    if (s.pendingSwitch && s.winner == null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Switch ends!") },
            text = {
                Text(
                    "Score reached ${s.switchAt}. Teams swap sides of the court. " +
                        "Each player keeps the same right/left court.",
                )
            },
            confirmButton = { TextButton(onClick = vm::ackSwitch) { Text("Done") } },
        )
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Start a new game?") },
            text = { Text("The current score will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNew = false
                    vm.backToSetup()
                }) { Text("New game") }
            },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TeamPanel(s: GameState, team: Int, modifier: Modifier, onTap: () -> Unit) {
    val serving = s.servingTeam == team
    val color = teamColor(team)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (serving) color else color.copy(alpha = 0.28f))
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "TEAM ${team + 1}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f),
        )
        s.config.players[team].forEachIndexed { idx, name ->
            val isServer = serving && idx == s.serverIdx
            val isReceiver = !serving && idx == s.receiverIdx()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    name,
                    fontSize = 20.sp,
                    fontWeight = if (isServer) FontWeight.Black else FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isServer) Tag("SERVER", Accent) else if (isReceiver) Tag("RECEIVER", ReceiverGreen)
            }
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val fs = minOf(maxHeight.value * 0.85f, maxWidth.value / 1.2f)
            Text(
                s.score[team].toString(),
                fontSize = fs.sp,
                lineHeight = fs.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
        }
        Text(
            if (serving) "SERVING · tap = point" else "tap if they win the rally",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Bg,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun CenterColumn(vm: GameViewModel, s: GameState, onNew: () -> Unit, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("SCORE CALL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
        Text(
            s.scoreCall(),
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Black,
            color = Accent,
            maxLines = 1,
        )
        val serverLine = if (s.isDoubles) "${s.serverName()} · Server ${s.serverNumber}" else s.serverName()
        Text(
            serverLine,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val side = if (s.serverRight()) "RIGHT" else "LEFT"
        Text(
            "serves from the $side court → ${s.receiverName()}",
            fontSize = 15.sp,
            color = Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val status = when {
            s.isGamePoint() -> "GAME POINT"
            s.isDoubles && s.serverNumber == 2 && s.score[0] + s.score[1] == 0 -> "Opening serve: only one server"
            else -> ""
        }
        Text(
            status,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (s.isGamePoint()) Accent else Muted,
            modifier = Modifier.height(20.dp),
        )

        CourtView(s, Modifier.weight(1f).fillMaxWidth())

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = vm::undo,
                enabled = vm.history.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text("↶ UNDO", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f)) {
                Text("NEW GAME", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────────── Court diagram ─────────────────────────────
// Top-down view, net in the middle. Team 1 on the left half, Team 2 on the right half.
// Each team faces the net, so Team 1's right hand points DOWN and Team 2's right hand points UP.

@Composable
private fun CourtView(s: GameState, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .clip(shape)
            .background(CourtGreen)
            .border(2.dp, Color.White, shape),
    ) {
        HalfCourt(s, if (s.swapped) 1 else 0, true, Modifier.weight(1f).fillMaxHeight())
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(Color.White),
        )
        HalfCourt(s, if (s.swapped) 0 else 1, false, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun HalfCourt(s: GameState, team: Int, onLeft: Boolean, modifier: Modifier) {
    Row(modifier) {
        if (onLeft) {
            ServiceArea(s, team, true, Modifier.weight(15f).fillMaxHeight())
            Kitchen(Modifier.weight(7f).fillMaxHeight())
        } else {
            Kitchen(Modifier.weight(7f).fillMaxHeight())
            ServiceArea(s, team, false, Modifier.weight(15f).fillMaxHeight())
        }
    }
}

@Composable
private fun Kitchen(modifier: Modifier) {
    Box(
        modifier
            .background(KitchenGreen)
            .border(1.dp, Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text("KITCHEN", fontSize = 9.sp, color = Color.White.copy(alpha = 0.55f))
    }
}

@Composable
private fun ServiceArea(s: GameState, team: Int, onLeft: Boolean, modifier: Modifier) {
    // Facing the net: the left-half team's right hand points down, the right-half team's points up.
    val topIsRight = !onLeft
    Column(modifier) {
        Cell(s, team, topIsRight, Modifier.weight(1f).fillMaxWidth())
        Cell(s, team, !topIsRight, Modifier.weight(1f).fillMaxWidth())
    }
}

/** Index of the player standing in this team's right/left court, or null if nobody is there (singles). */
private fun playerAt(s: GameState, team: Int, isRight: Boolean): Int? =
    if (s.isDoubles) {
        if (isRight) s.rightIdx[team] else 1 - s.rightIdx[team]
    } else {
        if (s.serverRight() == isRight) 0 else null
    }

@Composable
private fun Cell(s: GameState, team: Int, isRight: Boolean, modifier: Modifier) {
    val idx = playerAt(s, team, isRight)
    Box(
        modifier.border(1.dp, Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isRight) "RIGHT" else "LEFT",
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp),
        )
        if (idx != null) {
            PlayerChip(
                name = s.config.players[team][idx],
                team = team,
                isServer = team == s.servingTeam && idx == s.serverIdx,
                isReceiver = team == s.receivingTeam && idx == s.receiverIdx(),
                serverNumber = if (s.isDoubles) s.serverNumber else 0,
            )
        }
    }
}

@Composable
private fun PlayerChip(name: String, team: Int, isServer: Boolean, isReceiver: Boolean, serverNumber: Int) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isServer) Accent else Color(0xCC0B1220)
    val fg = if (isServer) Bg else Color.White
    Column(
        Modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, if (isReceiver) ReceiverGreen else teamColor(team), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (isServer) {
            Text(
                if (serverNumber > 0) "SERVES · S$serverNumber" else "SERVES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
        } else if (isReceiver) {
            Text("RECEIVES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ReceiverGreen)
        }
    }
}
