# The Happy Wheel - Implementation Spec (Prototype v0.1)

## Scope
Single-screen Android prototype focused on troll puzzle mechanics before wheel spin.

## Objective
Enable the player to spin the wheel only after resolving setup puzzles. Failures should be humorous and fast to recover.

## Core Loop
1. Player plugs the power cable → room lights up → `Start` becomes enabled.
2. Player taps `Start`:
   - If anchor or stand unresolved → **Dramatic Failure** sequence plays.
   - If all conditions met → wheel spins and shows a random result.
3. After **Dramatic Failure**: wheel respawns in place, anchor/stand reset to broken, power retained, humorous message shown.
4. Player fixes puzzles → taps `Start` again → spin succeeds.

## Mandatory Puzzle Rules

### 1) Disabled Start Button (Power Cable)
- Initial: `isPowered = false`, `Start` disabled.
- Interaction: tap cable control.
- Success: `isPowered = true`, `Start` enabled.

### 2) Unstable Anchor
- Initial: `isAnchorStable = false`.
- Interaction: tap anchor `REQUIRED_ANCHOR_TAPS` times.
- Success: `isAnchorStable = true`.
- Failure: pressing `Start` toggles wobble feedback and blocks spin.

### 3) Unstable Stand
- Initial: `isStandFixed = false`.
- Interaction: tap stand to tighten.
- Success: `isStandFixed = true`.
- Failure: pressing `Start` triggers collapse animation and spin is denied.

### 4) Spam Clicking Consequence
- Track rapid `Start` presses in `SPAM_WINDOW_MS`.
- Rapid taps increase `overheat`.
- Spin speed multiplier is derived from `overheat`.
- If `overheat >= FIRE_THRESHOLD`: wheel catches fire, vanishes, hard round reset.

## Runtime State Model
- `isSpinning`: prevents re-entry while animation runs.
- `isFailing`: true during dramatic failure sequence; blocks all input.
- `isPowered`: gate for button activation and room lighting.
- `isAnchorStable`: gate for spin readiness; reset to false after dramatic failure.
- `isStandFixed`: gate for spin readiness; reset to false after dramatic failure.
- `wheelExists`: false only after spam-fire fail, until full reset.
- `anchorTapCount`: progress toward anchor stability; reset after dramatic failure.
- `overheat`, `lastStartTapMs`: spam/heat system.

## Gate Condition

### Start Button Enabled
`isPowered && wheelExists && !isFailing && !isSpinning`

### Spin Succeeds
`isAnchorStable && isStandFixed && overheat < FIRE_THRESHOLD`

### Dramatic Failure Triggered
`isPowered && (!isAnchorStable || !isStandFixed)` on `Start` press

**Dramatic Failure sequence (non-destructive — no full game reset):**
1. **(0ms)** Random humorous failure message shown. Wheel wobbles violently.
2. **(380ms)** Wheel falls off-screen (translationY, rotation, scale down). Stand collapses (rotationBy).
3. **(820ms)** Wheel tints red (fire effect via `backgroundTintList`).
4. **(1050ms)** Wheel becomes invisible.
5. **(2000ms)** `respawnAfterFailure()` — wheel pops back in with bounce animation, anchor/stand reset to broken, `isPowered` retained, random respawn joke shown, `Start` re-enabled.

### Spam Fire Triggered
`overheat >= FIRE_THRESHOLD` (during or after spin) → `triggerFireFailure()` → full round reset.

## UI Contract (Current IDs)
- `statusTV`: one-line humor + hint. Positioned top of screen, always above dim overlay (elevation=4dp).
- `wheelTV`: spin target + disappear on fire. Centered on screen (vertical bias 0.36).
- `anchorTV`: tap to stabilize anchor. **Fixed at top-center of wheel** (`constraintBottom_toTopOf="@id/wheelTV"`, centered on wheel horizontally).
- `standTV`: tap to fix stand. **Fixed at bottom-center of wheel** (`constraintTop_toBottomOf="@id/wheelTV"`, centered on wheel horizontally).
- `cableTV`: tap to connect power. **Fixed at bottom-left corner of room**, above Start button.
- `luckyNumberTV`: final spin result. Below `standTV`, elevated above dim overlay (elevation=4dp).
- `playTV`: Start button. Fixed at bottom center, glows amber (`#FFC107`) when powered.
- `dimOverlay`: full-screen black overlay (alpha=0.72), fades to 0 on power connect, non-clickable.

## Event Flow
- `onCreate` → bind views → register click handlers → `resetRound(fullReset = true)`.
- `play()` → guard `isFailing` → `updateSpamOverheat()` → if spinning check fire → `tryStartSpin()`.
- `tryStartSpin()` branches:
  - `!isPowered` → status hint,
  - `!isAnchorStable || !isStandFixed` → `triggerDramaticFailure()`,
  - `overheat >= FIRE_THRESHOLD` → `triggerFireFailure()` (full reset),
  - else → `spinWheel()`.
- `triggerDramaticFailure()` → 5-phase chained animation → `respawnAfterFailure()`.
- `respawnAfterFailure()` → partial state reset (power retained) → wheel pop-in → re-enable Start.
- `spinWheel()` → animates wheel → writes result → resets `overheat`.

## Timing Defaults
- Spam tap window: `350ms`.
- Fire threshold: `6` rapid taps.
- Wheel wobble (fail phase 1): `380ms`.
- Wheel fall animation (fail phase 2): `480ms`.
- Fire tint (fail phase 3): at `820ms`.
- Wheel invisible (fail phase 4): at `1050ms`.
- Wheel respawn (fail phase 5): at `2000ms`.
- Respawn pop-in: `260ms` expand + `160ms` settle.
- Spin duration: `900ms` (prototype; tune later).

## Future Extensions
- Fake Start buttons (one valid).
- Reverse controls.
- Hold-to-spin hidden condition.
- Deceptive timer windows.
- Wheel drift correction taps.

## Validation Plan
- Manual smoke test:
  1. Verify Start disabled before cable connect.
  2. Verify anchor requires 5 taps.
  3. Verify stand collapse if unfixed.
  4. Verify rapid taps trigger fire reset.
  5. Verify successful spin after all fixes.
- Build check: compile debug variant with Gradle.
