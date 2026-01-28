# 🧠 Anime4K: The Ultimate Guide

**Anime4K** is a set of open-source, high-performance real-time upscaling/denoising algorithms for anime. This mod integrates them directly into the player via GLSL shaders.

---

## 🧪 Understanding the Modes

Anime4K isn't a "one size fits all" filter. It offers specialized modes for different content types.

### 🌟 Mode A: Faithful Upscaling (Best for 1080p/720p)
*   **Goal:** Restore the original lines as accurately as possible.
*   **Best For:** Modern anime (2010+) that is already decent quality (720p or 1080p) but looks slightly soft on high-res phone screens.
*   **Effect:** Sharpens edges and reconstructs missing details without altering the art style significantly.

### 🌫️ Mode B: Perceptual Deblur (Best for 720p/Older)
*   **Goal:** Treat the image as "blurred" and try to reverse it.
*   **Best For:** Older anime (90s/00s) or content that feels "out of focus."
*   **Effect:** Aggressively sharpens lines. Can sometimes create "thin" lines but makes the image pop significantly more than Mode A.

### 🧹 Mode C: Denoise & Restore (Best for 480p/Compressed)
*   **Goal:** Clean up the image before upscaling.
*   **Best For:** Low-quality streams, DVD rips, or videos with visible "blocky" compression artifacts.
*   **Effect:** Applies a "de-blocking" pass to smooth out JPEG/MPEG artifacts, then upscales. Prevents the "upscaling noise" effect.

### ➕ The "Plus" Variants (A+, B+, C+)
These modes run the primary reconstruction shader **twice**.
*   **Pros:** Even sharper image, higher perceived resolution.
*   **Cons:** **Doubles the GPU load.**
*   **Recommendation:** Only use on flagship devices (Snapdragon 8 Gen 1/2/3, Dimensity 9000+).

---

## ⚡ Performance & Quality Profiles

The "Quality" setting (Fast, Balanced, High) determines the complexity of the neural network used.

| Profile | CNN Size | GPU Load | Recommended For |
| :--- | :--- | :--- | :--- |
| **Fast (S)** | S (Small) | Low | Mid-range phones (Snapdragon 7xx, Exynos 1xxx) |
| **Balanced (M)** | M (Medium) | Medium | High-end older phones (Snapdragon 865/870/888) |
| **High (L)** | L (Large) | High | Current Flagships (Snapdragon 8 Gen 1+) |

### 🐢 Troubleshooting Lag
If your video stutters or audio desyncs:
1.  **Lower the Quality first:** Switch from High (L) to Balanced (M).
2.  **Avoid "Plus" modes:** Stick to standard Mode A/B/C.
3.  **Check Resolution:** Upscaling 1080p to 4K is much harder than 480p to 1080p. Disable shaders for 4K content.
