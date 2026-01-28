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
Integrated the famous **Anime4K neural networks** directly into the player using high-performance GLSL shaders.
*   **Neural Upscaling:** Go beyond basic bicubic scaling. Use Convolutional Neural Networks (CNN) to reconstruct missing details.
*   **Real-time Restoration:** Remove compression artifacts, ringing, and blurriness without losing the "hand-drawn" look.
*   **Hardware Accelerated:** Runs entirely on your device's GPU for zero-lag playback (on supported hardware).
*   **Dynamic Selection:** Choose between specialized modes (Restore, Upscale, Denoise) and quality profiles (Fast to Ultra).

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

Anime4K isn't just one filter; it's a suite of shaders designed for different types of content:

*   **Mode A (Optimized):** The default for most modern anime. It focuses on sharpening and upscaling. Best for 720p/1080p content that looks a bit soft.
*   **Mode B (De-Blur):** Specifically tuned for content that has been upscaled poorly or looks "out of focus." It aggressively recovers line art.
*   **Mode C (De-Noise):** The "heavy lifter" for low-quality videos (480p, old DVD rips, or heavily compressed streams). It uses a de-blocking pass before upscaling to prevent "ugly" blocks from becoming larger.
*   **Mode A+B / B+C:** Combined chains for specific scenarios where you need both restoration and sharpening.
*   **"Plus" Versions:** These modes run the neural pass multiple times for even crisper lines. **Warning:** These require significantly more GPU power.

#### ⚡ Performance Guide
*   **Fast (S):** Uses lightweight kernels. Perfect for mid-range phones or saving battery.
*   **Balanced (M):** The recommended setting for most modern devices (Snapdragon 865+).
*   **High (L):** Uses complex 64-128 channel networks. Best for flagship GPUs (Snapdragon 8 Gen 1/2/3).
*   **Pro-Tip:** If you experience frame drops, lower the Quality (e.g., from L to M) before changing the Mode. Mode A is generally the lightest.

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
