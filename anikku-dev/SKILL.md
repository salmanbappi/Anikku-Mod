---
name: anikku-dev
description: Automates git deployment and build monitoring. Use when the user wants to push changes, deploy updates, or check if the build passed.
---

# AniZen Dev Ops

## Instructions

1.  **Deploy & Watch**:
    When the user wants to push changes or "deploy", run the `deploy.sh` script.
    ```bash
    ./anikku-dev/scripts/deploy.sh "Your commit message here"
    ```

2.  **Handle Success**:
    - If the script exits with **0** (Success), inform the user that the build passed and the APK is ready (instruct them where to find it on GitHub).

3.  **Handle Failure**:
    - If the script exits with **1** (Failure), it will automatically save the error logs to `build_error.log` in the current directory.
    - **IMMEDIATELY** read `build_error.log`.
    - Analyze the error.
    - **Plan a Fix**: Propose or implement a fix based on the error.
    - **Retry**: After fixing, ask the user if they want to try deploying again.

## Maintenance & Mod Notes

### 1. Download Engine
- **Multi-threaded Logic**: Located in `core:common` module (`NetworkHelper.kt`). Uses `RandomAccessFile` and `Range` headers.
- **Import Constraints**: In `Downloader.kt`, always use fully qualified names for `java.io.File` and `java.io.InputStream` to avoid collision with `com.hippo.unifile.UniFile` and internal project symbols.

### 2. Build Stability
- **String Resources**: Release builds use strict AAPT2 validation. All strings with multiple placeholders (e.g., `%s %d`) **MUST** use positional markers (e.g., `%1$s %2$d`). If build fails with "Multiple substitutions", check `i18n` module XMLs.
- **Keystore**: The release keystore `app/anikku-mod.jks` is tracked in Git. Password is `salman2005`.

### 3. Performance Optimizations
- **Batch Status**: `AnimeScreenModel.kt` uses a batch check for downloaded directory names to avoid UI stutters when scrolling large lists.

## Scripts
- `deploy.sh`: Commits, pushes, watches the build, and downloads logs on failure.