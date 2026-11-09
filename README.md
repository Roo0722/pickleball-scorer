# Pickleball Scorer (Android)

Landscape scoreboard for pickleball with big score numbers, singles/doubles, player names,
service turns (Server 1 / 2), and a court diagram showing who serves from which court.

## Get the APK (GitHub Actions only)
1. Create a new GitHub repo and upload **all** files from this folder (keep the `.github` folder).
2. Open the repo's **Actions** tab. The `Build APK` workflow runs on every push (or use *Run workflow*).
3. When it finishes, open the run and download the artifact **pickleball-scorer-debug-apk**. Unzip it and install `app-debug.apk` on your phone (allow "install unknown apps").

No Gradle wrapper is needed; the workflow installs Gradle 8.9 and JDK 17 itself.

## How to use
- **Setup (portrait):** pick Doubles/Singles, play-to (11/15/21), names, who serves first, and whether to switch ends. Scoring switches to landscape automatically.
- **Switch ends:** when a team first reaches the midpoint (6 of 11, 8 of 15, 11 of 21) a "Switch ends!" prompt appears and the court diagram flips. Players keep their right/left courts. Can be turned off in setup.
- **During play, tap the team that WON the rally.**
  - Serving team taps its panel: +1 point, the server swaps courts.
  - Receiving team taps its panel: the server faulted. Doubles: Server 1 → Server 2 → side out. Singles: immediate side out.
- Center shows the score call (`4 – 3 – 2`), who serves, which court (RIGHT/LEFT) and who receives.
- Court diagram: Team 1 left half, Team 2 right half, seen from above, net in the middle.
- **Undo** reverts the last tap. The game is saved if the app closes.

## Rules built in (USA Pickleball side-out scoring)
- Only the serving side scores; first to the target with a 2-point lead wins.
- Doubles starts at 0-0-2 (first serving team gets one server only).
- Players swap courts only when their team scores. After a side out, the player in the right court serves first.
- Singles: serve from the right court when your score is even, left when odd.
- Serve goes diagonally cross-court.
