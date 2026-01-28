# 🌌 AniZen

AniZen is a high-performance, feature-rich modified version of AniZen. This mod focuses on providing a professional-grade viewing experience with advanced video processing tools, optimized downloading, and tactile feedback.

---

## 💎 Exclusive Mod Improvements

Compared to the original AniZen, this mod introduces massive upgrades to the player, trackers, and downloading system.

### 🎨 Ultimate Video Filter Suite (mpvEx Style)
The filtering system has been completely redesigned into a clean, expandable card-based UI.
*   **Filter Presets (Themes):** Instantly apply optimized looks like **Vivid Anime**, **Cinema**, **Night Mode**, **Warm/Cold**, and **Grayscale**.
*   **Advanced Controls:** Granular sliders for **Sharpness**, **Blur**, and **Film Grain**.
*   **Pro-Level Debanding:** Detailed control over Iterations, Threshold, and Range to eliminate color artifacts in anime gradients.
*   **Consolidated Chain:** Filters are applied in a single pass to prevent screen flickering/blinking.

### 🧠 Anime4K Real-time Neural Upscaling
Integrated the famous **Anime4K neural networks** directly into the player.
*   **Upscale Low-Res Content:** Make 480p/720p look like crisp 4K in real-time.
*   **Custom Modes:** Select between Mode A (Restore), Mode B (Soft), and Mode C (Denoise) with "Plus" versions for flagship devices.
*   **Quality Levels:** Fast (S), Balanced (M), and High (L) to match your phone's GPU power.

### ⏩ Customizable Long Press Speed
*   **Beyond 2x:** Go into the Playback Speed menu and set your preferred "Long press speed" (e.g., 3x, 4x, or even 0.5x).
*   **Visual Feedback:** The on-screen indicator dynamically updates to show your configured speed.
*   **Zero-Bug Reset:** Fixed the original bug where speed would reset to incorrect values after release.

### ⚙️ Pro Decoder Settings
Found in **Settings -> Player -> Decoder**:
*   **High Quality Scaling:** Enables `ewa_lanczossharp` for significantly cleaner edges.
*   **Interpolation (Smooth Motion):** Synchronizes video frames with your screen's refresh rate for buttery smooth 60fps-like motion on 24fps anime.
*   **Universal Filter Fix:** Forces `mediacodec-copy` automatically when filters are active, ensuring they work on **every video** regardless of format.

### 📥 1DM+ Style Downloader
*   **Parallel Power:** Increased concurrent episode limit to **30**.
*   **Multi-threaded Chunking:** Optimized downloader for direct files, significantly increasing speeds on stable connections.
*   **Internal Downloader UI:** Dedicated settings to toggle and configure the internal download engine.

### 🔗 Enhanced Tracker Stability
*   **MAL Fix:** Updated to the official Client ID for 100% compatibility.
*   **AniList Robustness:** Improved token parsing to prevent login failures on specific redirect URLs.
*   **Redirect Logic:** Optimized deep-link capturing for all trackers.

---

## 🔧 How to Use & Pro-Tips

### 🎭 Using Video Filters & Presets
1.  Open the **Video Filters** panel in the player.
2.  Tap on **Presets** to see the new layout.
3.  Choose a theme (e.g., **Vivid Anime**). You will see the description explaining what it does.
4.  The app will automatically match and highlight the preset if you move the sliders manually!

### 🧪 Understanding Anime4K Modes
*   **Mode A (Restoration):** Best for high-quality anime (1080p). Makes edges "pop."
*   **Mode B (Soft Fix):** Use this for blurry or older anime to regain focus.
*   **Mode C (Dirty Fix):** Best for low-quality or "noisy" videos (DVD rips). It cleans up the "blocks" and grain.
*   **The "+" versions:** Run the restoration twice. Only use these on **flagship devices** (Snapdragon 8 Gen 1+).
*   **If it lags:** Lower the quality from **High** to **Balanced** or **Fast**.

### 📳 Tactile Experience
This mod adds **Haptic Feedback** across the app for a premium feel:
*   Nav bar clicks, toggle switches, and library item interactions.
*   Logo header and playback control triggers.

---

## 📥 Downloads

Latest releases are available on the [Releases Page](https://github.com/salmanbappi/AniZen-Mod/releases).

### Development Builds
For the absolute latest bleeding-edge updates (debug builds), check the [Actions Tab](https://github.com/salmanbappi/AniZen-Mod/actions) and download the APK from the latest successful run.

---

## 🛠️ Development

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
-   **Original AniZen:** [komikku-app/anikku](https://github.com/komikku-app/anikku)
-   **Base Project:** [Aniyomi](https://github.com/aniyomiorg/aniyomi)
-   **Filter Inspiration:** [mpvEx](https://github.com/marlboro-advance/mpvEx)
-   **Shaders:** [Anime4K](https://github.com/bloc97/Anime4K)
