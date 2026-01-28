# ⚙️ Pro Player Settings & Filters Guide

This mod unlocks the full potential of `mpv-android`. However, with great power comes great battery drain. Here is everything you need to know.

---

## 🏗️ High Quality Scaling (`ewa_lanczossharp`)

*   **What is it?** This replaces the standard bilinear video scaler with a **Jinc-based scaler** (Lanczos radius 3).
*   **Why use it?** Standard scaling makes diagonals look "jagged." This scaler calculates pixels using a complex math function that preserves sharpness while smoothing jaggies.
*   **⚠️ The Cost:** It is extremely mathematically intensive. Enabling this on a 1080p video can **triple the GPU load** compared to standard playback.
*   **When to DISABLE:** If your phone gets hot, the battery drains fast, or you see frame drops.

---

## 🎞️ Interpolation (Smooth Motion)

*   **The Problem:** Most anime is produced at **24 frames per second (fps)**. Most phone screens update at **60Hz, 90Hz, or 120Hz**.
    *   60 is not divisible by 24 (60 / 24 = 2.5). This means some frames are shown for 2 screen refreshes, and others for 3. This causes **judder** (uneven motion) in panning shots.
*   **The Solution:** Interpolation generates **fake intermediate frames** to blend the motion.
*   **⚠️ The Cost:**
    1.  **Input Lag:** The player must buffer frames to calculate the blend, adding delay.
    2.  **Soap Opera Effect:** Some people find smooth motion looks "unnatural" for anime.
    3.  **Battery:** Your GPU is working overtime to generate these new frames.

---

## 🎨 Video Filters & Copy Mode

### 🔓 The "Universal Filter Fix" (Copy Mode)
Normally, Android's hardware decoder is a "black box"—apps can't touch the video. To apply filters like **Grayscale** or **Saturation**, we must force the decoder to **copy** the image data back to the app's memory.

*   **Auto-Smart Mode:** This mod monitors your filters.
    *   **Filters OFF:** Uses standard, efficient hardware decoding.
    *   **Filters ON (Saturation, Sharpen, etc.):** Automatically switches to `mediacodec-copy`.
*   **Why does this matter?** `mediacodec-copy` uses more battery/CPU bandwidth than standard mode.
*   **Pro-Tip:** If you care about maximum battery life, ensure all "Video Filter" sliders are set to **0**.

### 🖌️ Presets Explained
*   **Vivid Anime:** (Contrast +5, Saturation +20, Sharpen +15). A heavy preset. Makes colors pop and lines crisp.
*   **Cinema:** (Brightness -5, Contrast +15, Saturation -10). Moodier, darker look.
*   **Vintage:** (Saturation -30, Gamma -10, Hue -5). Faded, old-school aesthetic.
*   **Grayscale:** (Saturation -100). Classic black and white.
