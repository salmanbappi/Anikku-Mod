# 🌌 AniZen (Pro Mod)

**AniZen** is a high-performance, ultra-refined modification of Anikku. While the official app provides a baseline, AniZen transforms it into a professional-grade media center with a focus on **Extreme Performance**, **Audiophile-grade Video**, and **Premium Tactile Feedback**.

---

## ⚡ Why AniZen? (Comparison with Official)

| Feature | Official Anikku | **AniZen Pro Mod** |
| :--- | :--- | :--- |
| **Video Engine** | Standard MPV | **Optimized "Zero-Lag" Engine** |
| **Upscaling** | Basic Bilinear | **Anime4K Real-time Neural Networks** |
| **Sync Logic** | Heavy `display-resample` | **Performance-first `audio` sync** |
| **Startup Speed** | Slow (Font copying lag) | **Instant-open (Smart Caching)** |
| **Downloader** | Standard Single-thread | **1DM+ Style (30 Threads + Chunking)** |
| **UI Interaction** | Static / No feedback | **Full Haptic Integration (Premium feel)** |
| **Stability** | Standard | **Universal Filter Compatibility Fix** |

---

## 💎 Exclusive Pro Features

### 🧠 Anime4K Neural Upscaling
Experience your library in true 4K. Integrated **Anime4K neural networks** allow real-time upscaling of 720p/1080p content to 4K with stunning clarity.
*   👉 **[Read the Anime4K Guide](docs/ANIME4K_GUIDE.md)** for Mode & Performance details.

### ⚙️ The "Zero-Lag" Engine
We’ve rewritten the internal MPV configuration to eliminate mobile-specific bottlenecks:
*   **Audio-First Sync:** Default sync logic swapped to eliminate the "leg" (stutter) found in official builds.
*   **Intelligent HWDEC:** Automatic `mediacodec-copy` switching only when required by filters, preserving battery and performance.
*   **Low-Overhead Monitoring:** Technical stats (FPS/Mistime) now use an efficient polling system instead of background-draining push events.
*   **High Quality Scaling:** Edge-shaping via `ewa_lanczossharp` for the sharpest anime lines.
*   👉 **[Read the Pro Player Guide](docs/PRO_PLAYER_GUIDE.md)** for technical deep dives.

### 📥 1DM+ Style Downloader
Engineered for speed. We've increased the concurrent episode limit to **30** and implemented multi-threaded chunking to saturate your bandwidth.

### 🖐️ Premium Tactile Experience
AniZen feels "alive." We’ve integrated subtle, professional-grade haptic feedback into:
*   Volume and Brightness sliders.
*   Seek bar scrubbing.
*   Settings toggles and Switches.
*   Library and Search item interactions.
*   Navigation and Header branding.

### 🎨 Ultimate Filter Suite
A granular, card-based UI allows you to adjust **Sharpen, Blur, Grain, and Debanding** on the fly, with high-performance presets like *Vivid Anime* and *Cinema*.

---

## 📥 Downloads

[**Download Latest Releases**](https://github.com/salmanbappi/AniZen/releases)

For experimental updates, check the [**Actions Tab**](https://github.com/salmanbappi/AniZen/actions) for the latest successful build.

---

## 🛠️ Development

### Building Locally
```bash
./gradlew assembleRelease
```

### Side-by-Side Install
AniZen uses the unique package ID `app.anizen`, allowing you to keep the official Anikku installed while using the Pro version.

---

## 🤝 Credits
-   **Original Anikku:** [komikku-app/anikku](https://github.com/komikku-app/anikku)
-   **Base Project:** [Aniyomi](https://github.com/aniyomiorg/aniyomi)
-   **Filter Inspiration:** [mpvEx](https://github.com/marlboro-advance/mpvEx)
-   **Shaders:** [Anime4K](https://github.com/bloc97/Anime4K)