---
name: anikku-dev
description: Automates git deployment and build monitoring. Use when the user wants to push changes, deploy updates, or check if the build passed.
---

# Anikku Dev Ops

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

## Scripts
- `deploy.sh`: Commits, pushes, watches the build, and downloads logs on failure.