# The Happy Wheel - Implementation Spec (Prototype v0.1)

## Scope
Single-screen Android prototype focused on troll puzzle mechanics before wheel spin.

## Objective
Enable the player to spin the wheel only after resolving setup puzzles. Failures should be humorous and fast to recover.

## Core Loop
1. Player attempts `Start`.
2. Game validates puzzle prerequisites.
3. On failure, show a comic fail state + guidance.
4. Player resolves blockers (power, anchor, stand).
5. Wheel spins and shows a random result.
6. Round resets.

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
- `isPowered`: gate for initial button activation.
- `isAnchorStable`: gate for spin readiness.
- `isStandFixed`: gate for spin readiness.
- `wheelExists`: false after fire fail until reset.
- `anchorTapCount`: progress toward anchor stability.
- `overheat`, `lastStartTapMs`: spam/heat system.

## Gate Condition
Spin can start only when:
`isPowered && isAnchorStable && isStandFixed && wheelExists && !isSpinning && overheat < FIRE_THRESHOLD`

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
- `onCreate` -> bind views -> register click handlers -> `resetRound(fullReset = true)`.
- `play()` -> `updateSpamOverheat()` -> `tryStartSpin()`.
- `tryStartSpin()` branches:
  - gate failures (power/anchor/stand/fire), or
  - `spinWheel()` success path.
- `spinWheel()` animates wheel, writes result, resets `overheat`.

## Timing Defaults
- Spam tap window: `350ms`.
- Fire threshold: `6` rapid taps.
- Stand collapse recovery: `900ms`.
- Fire reset delay: `1500ms`.
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
