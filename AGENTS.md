# AGENTS.md — Pastiera

Pastiera is an Android input method (IME) for devices with a physical keyboard (profiled on the Unihertz Titan 2; layouts are JSON-adaptable). This repository is Seongheon's fork; the Korean (두벌식 QWERTY) input and dictionary work lives here. Package namespace: `it.palsoftware.pastiera`. `minSdk 29`, `targetSdk 36`, JVM target 11.

## Layout
- `app/` — the single Gradle module (Kotlin). Product flavors: `stable` and `nightly`. Build types include `release` and `nightly`.
- `scripts/` — build, signing, dictionary, and emoji helpers (see `scripts/README.md`).
- `maestro/titan2elite/` — Maestro UI flows for the Titan 2 Elite.
- `tools/prediction_bench/` — suggestion and prediction benchmarks.
- `docs/`, `design/`, `fastlane/metadata/`, `fdroiddata/`, `third_party/` — docs, assets, store metadata, and vendored code.

## Verification
Match the checks to the change, and run the CI set before you call a change done:

```sh
./gradlew :app:testStableDebugUnitTest
./gradlew :app:testStableDebugUnitTest -PPASTIERA_FDROID_BUILD=true
./gradlew :app:testNightlyDebugUnitTest
```

Targeted suites (from README "Development / Tests"):
- Modifier state, routing, and service: `--tests it.palsoftware.pastiera.core.ModifierStateControllerTest --tests it.palsoftware.pastiera.inputmethod.InputEventRouterModifierE2ETest --tests it.palsoftware.pastiera.inputmethod.PhysicalKeyboardInputMethodServiceDeviceBehaviorTest`
- Flavor and update logic: `--tests it.palsoftware.pastiera.FlavorBuildConfigTest --tests it.palsoftware.pastiera.update.UpdateCheckerFlavorLogicTest` (run on both `Stable` and `Nightly` debug unit-test tasks)
- Nightly debug APK: `./scripts/build-nightly-debug.sh <version> [--install] [--device <adb-serial>]`

## Rules
- Never commit signing material. `release/keystore.properties`, keystores, and `*_KEYSTORE_B64` values stay local or in CI secrets, and they must not appear in logs or diffs.
- Release, nightly, and F-Droid publishing (`build-release.sh`, `publish-private-fdroid-nightly.sh`, GitHub release workflows) change external state. Run them only when the user explicitly asks.
- Keep `stable` and `nightly` flavor behavior separate. Update-checker logic is flavor-specific and has tests that cover it.
- Regenerate dictionaries and emoji assets with the scripts in `scripts/`; don't hand-edit generated assets.
- Keep diffs scoped, and keep upstream-facing code style consistent with the surrounding Kotlin.
