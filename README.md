# BodyQuest — Physical AI Coach

> **Your body is the controller. Your phone is the coach.**

BodyQuest is a real-time physical AI fitness coach for Android. It watches a squat through
the camera, tracks body pose on-device, scores the rep against real biomechanics, speaks a
single actionable correction, and then checks whether the *next* rep actually improved.

```
SEE → UNDERSTAND → ANALYZE → CORRECT → VERIFY → LEARN → ADAPT
```

Most fitness apps count reps. BodyQuest checks *how* you moved, tells you the one thing to
fix, and confirms whether you fixed it — closing the loop between coach and athlete.

## Status

Early build, squat-only. The vertical slice below is real and tested; everything past it
(push-up/curl, persistence, adaptive difficulty, voice commands, haptics, the laptop
companion) is scaffolded as empty packages/directories and not yet implemented.

| Layer | What's built |
|---|---|
| Camera | CameraX preview + frame analysis, live on the phone's camera |
| Pose | MediaPipe Pose Landmarker on-device, GPU delegate with CPU fallback |
| Movement | Squat rep-counting state machine (smoothed, hysteresis, debounced) |
| Form | Deterministic depth / knee-alignment / torso / tempo / symmetry scoring |
| Coaching | Single-issue correction engine with before/after verification; phrasing pluggable with an on-device Gemma 3 model, falls back to a fixed phrase table when no model is loaded |
| Voice | On-device TTS speaks corrections and positioning guidance |
| UI | Splash → Onboarding → Home → Workout/Exercise Selection → Calibration → Live Coach → Set Summary |

## Built with

- Kotlin + Jetpack Compose, single Gradle Android app module
- CameraX — camera preview and frame analysis
- MediaPipe Tasks Vision — Pose Landmarker (GPU delegate, CPU fallback)
- MediaPipe Tasks GenAI — optional on-device Gemma 3 model for coaching phrasing
- Android TextToSpeech — on-device coaching voice
- Room — local persistence
- Kotlin coroutines

## Requirements

- Android Studio / JDK 17
- Android SDK: compileSdk 36, minSdk 26
- A device or emulator with a camera (an emulator can use a webcam as its virtual back camera)

## Getting started

```bash
./gradlew :app:installDebug   # build and install onto a connected device/emulator
./gradlew :app:testDebugUnitTest   # run the engine test suite (no device needed)
```

Launch the app, grant camera access, and follow Calibration → Live Coach to run a squat set.

Coaching phrases work out of the box from the fixed phrase table. To try Gemma-generated
phrasing instead, push a converted Gemma 3 `.task` model (e.g. `gemma3-1b-it-int4.task`) onto
the device at the app's `filesDir` — the model isn't bundled in the APK, so without it the app
silently uses the fixed phrases.

## Architecture

One Gradle app module, package-boundaried by layer:

```
app/src/main/java/com/bodyquest/
  core/      pose/landmark types, geometry helpers, the PoseEngine interface
  camera/    CameraX binding, frame→bitmap conversion, FPS tracking
  pose/      MediaPipe pose estimation + camera-thread glue
  movement/  per-exercise rep-counting state machines
  form/      deterministic biomechanics scoring
  coach/     correction selection + before/after verification, phrase table
  llm/       on-device Gemma coaching-phrase provider (optional, falls back to coach/'s table)
  workout/   orchestrates camera frame → rep → form → correction end-to-end
  voice/     on-device TTS wrapper
  ui/        Compose screens, navigation, theme
```

`PoseEngine` is the one seam between the ML runtime and everything else — swap the
MediaPipe implementation without touching movement, form, or coaching logic.

Scaffolded but empty (later phases): `adaptive/`, `data/`, `net/`, `sensors/` under `app/`,
plus top-level `server/`, `studio/`, `shared/`, `models/` for the planned WebSocket sync and
BodyQuest Studio desktop companion.

## Testing

22 JVM unit tests, no device required, covering:

- Rep counting against clean, partial, paused, jittery, and reversed movement sequences
- Each form metric independently, against synthetic-but-geometrically-real pose fixtures
- Correction priority selection and the before/after verification loop
- The full frame → rep → form → correction pipeline end-to-end

```bash
./gradlew :app:testDebugUnitTest
```

### Manual/on-device

Unit tests don't touch the camera, MediaPipe, TTS, or Gemma — verify those on a real device
(the pose and Gemma models need real hardware; an emulator's software renderer/CPU won't give
representative latency or may fail to load the GPU delegate):

```bash
./gradlew :app:installDebug
```

- **Golden path**: grant camera permission → Calibration → Live Coach → do a squat set with
  deliberately bad form (e.g. knees caving in) → confirm a correction is spoken promptly and
  the next rep's snapshot verifies whether it improved → Set Summary shows the set's reps/form/
  corrections/quality trend.
- **Gemma fallback**: with no model file on the device, corrections should still speak the
  fixed phrases in `coach/CorrectionTypes.kt` — coaching must never go silent. Push a converted
  `gemma3-1b-it-int4.task` to the app's `filesDir` (see Getting started), relaunch, and confirm
  phrasing is generated instead; kill/corrupt the model file mid-session to confirm it falls
  back cleanly rather than crashing.
- **Edge cases**: partial body out of frame (should show the "position yourself" state, not a
  bogus score), pausing mid-rep, and backgrounding the app during Live Coach (camera/TTS/model
  resources should release via `onDispose`, not leak).

## Design principles

- **Deterministic biomechanics, not hallucinated AI.** Form scores come from joint-angle
  geometry; the LLM's job (where used at all — an optional on-device Gemma 3 model) is
  phrasing, never the numbers or which issue to raise.
- **On-device by default.** Pose estimation and coaching run locally; no raw video leaves
  the device.
- **One correction at a time.** When multiple form issues fire on the same rep, only the
  highest-priority one is surfaced — never a list.
