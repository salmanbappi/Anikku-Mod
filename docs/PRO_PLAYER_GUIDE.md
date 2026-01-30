# ⚙️ Pro Player Settings & Filters Guide

This mod unlocks the full potential of `mpv-android`. While the official version uses conservative settings, AniZen is tuned for maximum fidelity and responsiveness.

---

## ⚡ The "Zero-Lag" Optimization Suite

### 🔊 Performance-First Video Sync
*   **The Change:** Default `video-sync` has been switched to **`audio`**.
*   **Why?** The official app uses `display-resample`, which is extremely CPU-intensive on Android and often causes stuttering (lag) when skipping or playing high-bitrate files.
*   **AniZen Logic:** We use standard `audio` sync for smooth, lag-free performance. **`display-resample`** is automatically enabled **only** when you turn on **Interpolation**, as it is technically required for that feature.

### ⏱️ Smart Font Caching
*   **The Improvement:** Unlike the original app which copies fonts from storage on every launch, AniZen checks if fonts are already cached.
*   **Result:** Opening the player is now **near-instant**, even if you have hundreds of custom fonts.

### 📊 Low-Overhead Technical Stats (Page 6)
*   **The Improvement:** High-frequency properties (Real-time FPS, Mistime, Dropped Frames) now use an efficient **polling system**.
*   **Result:** Background CPU drain is eliminated when the stats page is closed. The app only fetches this data when you are actually looking at it.

---

## 🏗️ High Quality Scaling (`ewa_lanczossharp`)

*   **What is it?** Replaces standard bilinear scaling with a **Jinc-based scaler**.
*   **Why use it?** Standard scaling makes diagonals look "jagged." This preserves sharpness and removes jaggies from anime lines.
*   **⚠️ Compatibility:** Do **NOT** use `gpu-next` with Anime4K. AniZen will automatically switch you to the standard `gpu` renderer when Anime4K is enabled to prevent crashes.
*   **⚠️ The Cost:** Triple the GPU load compared to standard playback. Disable if your device gets hot.

---

## 🎞️ Interpolation (Smooth Motion)

*   **The Problem:** Anime is 24fps, screens are 60Hz+. This causes "judder" during camera pans.
*   **The Solution:** Generates intermediate frames to blend motion for a fluid 60fps+ look.
*   **⚠️ The Cost:**
    1.  **Input Lag:** Adds delay due to frame calculation.
    2.  **Battery:** Significant GPU/CPU impact.
    3.  **Sync:** Forces `display-resample` mode.

---

## 🎨 Video Filters & Copy Mode

### 🔓 Intelligent "Copy Mode" Switching
Normally, Android's hardware decoder is a "black box." To apply filters like **Sharpen** or **Saturation**, we must force the decoder to copy image data back to memory (`mediacodec-copy`).

*   **AniZen Smart Logic:**
    *   **Filters OFF:** Uses standard, ultra-efficient hardware decoding.
    *   **Filters ON:** Automatically enables `mediacodec-copy`.
*   **Pro-Tip:** For maximum battery life during long trips, ensure all Video Filter sliders are set to **0**.

### 🖌️ Presets Explained
*   **Vivid Anime:** (Contrast +5, Saturation +20, Sharpen +15). Makes colors pop and lines crisp.
*   **Cinema:** (Brightness -5, Contrast +15, Saturation -10). Moodier, darker look.
*   **Vintage:** (Saturation -30, Gamma -10, Hue -5). Faded, old-school aesthetic.