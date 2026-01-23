# Anikku Mod

A modified version of Anikku (Aniyomi fork) with enhanced player features and optimized performance.

## 🚀 Features

-   **2x Speed on Long Press:** Long-press anywhere in the video player to instantly toggle 2x playback speed with haptic feedback.
-   **Optimized CI/CD:** Fast builds and automatic releases via GitHub Actions.
-   **Debug Builds:** Easy access to latest debug APKs for testing.

## 📥 Downloads

Latest releases are available on the [Releases Page](https://github.com/salmanbappi/Anikku-Mod/releases).

### Development Builds
For the absolute latest bleeding-edge updates (debug builds), check the [Actions Tab](https://github.com/salmanbappi/Anikku-Mod/actions) and click on the latest successful run.

## 🛠️ Development

### Prerequisites
-   JDK 17
-   Android SDK
-   Git

### Building Locally
```bash
./gradlew assembleDebug
```

### Bumping Version
To release a new version:
```bash
./scripts/bump_version.sh [patch|minor|major]
git push && git push --tags
```

## 🤝 Credits
-   Based on [Anikku](https://github.com/komikku-app/anikku)
-   Original project: [Aniyomi](https://github.com/aniyomiorg/aniyomi)