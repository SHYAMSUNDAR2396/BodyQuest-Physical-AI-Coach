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
| Coaching | Single-issue correction engine with before/after verification |
| Voice | On-device TTS speaks corrections and positioning guidance |
| UI | Splash → Onboarding → Home → Workout/Exercise Selection → Calibration → Live Coach → Set Summary |

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

## Design principles

- **Deterministic biomechanics, not hallucinated AI.** Form scores come from joint-angle
  geometry; the LLM's job (where used at all) is phrasing, never the numbers.
- **On-device by default.** Pose estimation and coaching run locally; no raw video leaves
  the device.
- **One correction at a time.** When multiple form issues fire on the same rep, only the
  highest-priority one is surfaced — never a list.
