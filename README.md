# 🌌 AniZen

AniZen is a high-performance, feature-rich modified version of AniZen. This mod focuses on providing a professional-grade viewing experience with advanced video processing tools, optimized downloading, and tactile feedback.

---

## 💎 Exclusive Mod Improvements

### 🧠 Anime4K Real-time Neural Upscaling
Integrated the famous **Anime4K neural networks** directly into the player. Upscale 720p to 4K clarity in real-time.
*   👉 **[Read the Anime4K Guide](docs/ANIME4K_GUIDE.md)** for details on Modes (A, B, C) and Performance profiles.

### ⚙️ Pro Player Engine
Unlocked advanced `mpv` features for audiophiles and videophiles.
*   **High Quality Scaling:** `ewa_lanczossharp` for pristine edges.
*   **Interpolation:** Smooth 60fps motion for 24fps anime.
*   **Universal Filter Fix:** Smart auto-switching for filter compatibility.
*   👉 **[Read the Pro Player Guide](docs/PRO_PLAYER_GUIDE.md)** for deep dives on these settings and performance warnings.

### 🎨 Ultimate Video Filter Suite
*   **Presets:** Vivid Anime, Cinema, Vintage, and more.
*   **Granular Control:** Adjust Sharpen, Blur, Grain, and Debanding.
*   **Clean UI:** Expandable, card-based interface.

### 📥 1DM+ Style Downloader
*   **Parallel Power:** Increased concurrent episode limit to **30**.
*   **Multi-threaded Chunking:** Optimized for maximum speed.

### 🔗 Enhanced Tracker Stability
*   **Official Compatibility:** Uses official Aniyomi Client IDs for MyAnimeList and AniList.
*   **Seamless Login:** Supports `tachiyomi://` redirects for a "just works" experience.

### ⏩ Customizable Long Press Speed
*   Set any speed (e.g., 3x, 0.5x) for the long-press action.
*   Visual feedback and bug-free resetting.

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