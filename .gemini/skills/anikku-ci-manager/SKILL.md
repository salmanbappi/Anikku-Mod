---
name: anikku-ci-manager
description: Automates GitHub Actions workflows for AniZen. Use when triggering releases, watching build status, or handling build failures in the cloud.
---

# AniZen CI Manager

## Workflows

### 1. Trigger Official Release
When the user wants to "deploy" or "release", trigger the `release.yml` workflow.
```bash
gh workflow run release.yml
```

### 2. Watch Build Status
Always monitor the build after a push or manual trigger to ensure quality checks (Detekt) pass.
```bash
./scripts/watch_build.sh
```

### 3. Handle Cloud Failures
If a build fails:
1. Identify the failing step (e.g., `Run Code Analysis (Detekt)` or `Build Release APK`).
2. Download the logs using `gh run view --log`.
3. Propose a fix based on the specific lint or compiler error.