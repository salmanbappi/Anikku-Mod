<<<<<<< HEAD
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

[**Download Latest Releases**](https://github.com/salmanbappi/Anikku-Mod/releases)

For experimental updates, check the [**Actions Tab**](https://github.com/salmanbappi/Anikku-Mod/actions) for the latest successful build.

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
=======
<div align="center">

<a href="https://anikku-app.github.io">
    <img src="./.github/assets/icon.png" alt="anikku logo" title="anikku logo" width="80"/>
</a>

# Anikku [App](#)

### Full-featured player, based on Aniyomi.
Discover and watch anime, cartoons, series, and more – easier than ever on your Android device.

| Releases | Preview |
|----------|---------|
| <div align="center"> [![GitHub downloads](https://img.shields.io/github/downloads/komikku-app/anikku/latest/total?label=Latest%20Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/komikku-app/anikku/releases/latest) [![GitHub downloads](https://img.shields.io/github/downloads/komikku-app/anikku/total?label=Total%20Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/komikku-app/anikku/releases) [![Stable build](https://img.shields.io/github/actions/workflow/status/komikku-app/anikku/build_release.yml?labelColor=27303D&label=Stable&labelColor=06599d&color=043b69)](https://github.com/komikku-app/anikku/actions/workflows/build_release.yml) | <div align="center"> [![GitHub downloads](https://img.shields.io/github/downloads/komikku-app/anikku-preview/latest/total?label=Latest%20Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/komikku-app/anikku-preview/releases/latest) [![GitHub downloads](https://img.shields.io/github/downloads/komikku-app/anikku-preview/total?label=Total%20Downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://github.com/komikku-app/anikku-preview/releases) [![Preview build](https://img.shields.io/github/actions/workflow/status/komikku-app/anikku-preview/build_app.yml?labelColor=27303D&label=Preview&labelColor=2c2c47&color=1c1c39)](https://github.com/komikku-app/anikku-preview/actions/workflows/build_app.yml) |

[![Discord](https://img.shields.io/discord/1242381704459452488.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/85jB7V5AJR)
[![CI](https://img.shields.io/github/actions/workflow/status/komikku-app/anikku/build_push.yml?labelColor=27303D&label=CI)](https://github.com/komikku-app/anikku/actions/workflows/build_push.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/komikku-app/anikku?labelColor=27303D&color=0877d2)](/LICENSE)
[![Translation status](https://hosted.weblate.org/widget/komikku-app/anikku/svg-badge.svg)](https://hosted.weblate.org/projects/komikku-app/anikku/)

## Download

[![Stable](https://img.shields.io/github/release/komikku-app/anikku.svg?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://github.com/komikku-app/anikku/releases/latest)
[![Preview](https://img.shields.io/github/v/release/komikku-app/anikku-preview.svg?maxAge=3600&label=Preview&labelColor=2c2c47&color=1c1c39)](https://github.com/komikku-app/anikku-preview/releases/latest)

*Requires Android 8.0 or higher.*

[![Sponsor me on GitHub](https://custom-icon-badges.demolab.com/badge/-Sponsor-ea4aaa?style=for-the-badge&logo=heart&logoColor=white)](https://github.com/sponsors/cuong-tran "Sponsor me on GitHub")

## Features

![screenshots of app](./.github/readme-images/screens.png)

<div align="left">

### Features include:

* **Anikku**:
  * `Anime Suggestions` automatically showing source-website's recommendations / suggestions / related to current entry for all sources.
  * `Auto theme color` based on each entry's cover for entry View & Reader.
  * `App custom theme` with `Color palettes` for endless color lover.
  * `Bulk-favorite` multiple entries all at once.
  * `Fast browsing` (for who with large library experiencing slow loading)
  * Auto `2-way sync` progress with trackers.
  * Support `Android TV`, `Fire TV`.
  * From SY:
    * `Anime Recommendations` showing community recommends from Anilist, MyAnimeList.
    * Edit `Anime Info` manually, or fill data from MyAnimeList, Kitsu, Shikimori, Bangumi, Simkl.
    * `Custom cover` with files or URL.
    * `Feed tab`, where you can easily view the latest entries or saved search from multiple sources at same time.
    * `Saving searches` & filters, can use them with `Feed-tab`
    * `Pin anime` to top of Library with `Tag` sort.
    * `Merge anime` allow merging separated anime/episodes into one entry.
    * `Lewd filter`, hide the lewd anime in your library when you want to.
    * `Tracking filter`, filter your tracked anime so you can see them or see non-tracked anime.
    * `Search tracking` status in library.
    * `Mass-migration` all your anime from one source to another at same time.
    * `Dynamic Categories`, view the library in multiple ways.
    * `Custom categories` for sources, liked the pinned sources, but you can make your own versions and put any sources in them.
    * Cross device `Library sync` with SyncYomi & Google Drive.
  * Anime `cover on Updates notification`.
  * `Panorama cover` showing wide cover in full.
  * `to-be-updated` screen: which entries are going to be checked with smart-update?
  * `Update Error` screen & migrating them away.
  * `Source & Language icon` on Library & various places. (Some language flags are not really accurate)
  * `Grouped updates` in Update tab (inspired by J2K).
  * Drag & Drop re-order `Categories`.
  * Ability to `enable/disable repo`, with icon.
  * `Search for sources` & Quick NSFW sources filter in Extensions, Browse & Migration screen.
  * In-app `progress banner` shows Library syncing / Backup restoring / Library updating progress.
  * Long-click to add/remove single entry to/from library, everywhere.
  * Docking Watch/Resume button to left/right.
  * Auto-install app update.
  * Configurable interval to refresh entries from downloaded storage.
  * And many more from same maintainer's app for Manga reader: [Komikku](https://github.com/komikku-app/komikku)
* Aniyomi:
  * Watching videos
  * Local watching of downloaded content
  * A configurable player built on mpv-android with multiple options and settings
  * Tracker support: [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/), [Kitsu](https://kitsu.app/), [Simkl](https://simkl.in/), [Shikimori](https://shikimori.one), and [Bangumi](https://bgm.tv/)
  * Categories to organize your library
  * Create backups locally to watch offline or to your desired cloud service
* Other forks' features:
  * Torrent support (Needs right extensions) (@Diegopyl1209)
  * Support for Cast functionality (Animetail)
  * Group by tags in library (Kuukiyomi)
  * Discord Rich Presence (Animiru, Kuukiyomi, Animetail)

# Issues, Feature Requests and Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

<details><summary>Issues</summary>

[Website](https://anikku-app.github.io/)

1. **Before reporting a new issue, take a look at the [FAQ](https://anikku-app.github.io/docs/faq/general), the [changelog](https://github.com/komikku-app/anikku/releases) and the already opened [issues](https://github.com/komikku-app/anikku/issues).**
2. If you are unsure, ask here: [![Discord](https://img.shields.io/discord/1242381704459452488.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/85jB7V5AJR)

</details>

<details><summary>Bugs</summary>

* Include version (More → About → Version)
 * If not latest, try updating, it may have already been solved
 * Preview version is equal to the number of commits as seen on the main page
* Include steps to reproduce (if not obvious from description)
* Include screenshot (if needed)
* If it could be device-dependent, try reproducing on another device (if possible)
* Don't group unrelated requests into one issue

Use the [issue forms](https://github.com/komikku-app/anikku/issues/new/choose) to submit a bug.

</details>

<details><summary>Feature Requests</summary>

* Write a detailed issue, explaining what it should do or how.
* Include screenshot (if needed).
</details>

<details><summary>Contributing</summary>

See [CONTRIBUTING.md](./CONTRIBUTING.md).
</details>

<details><summary>Code of Conduct</summary>

See [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md).
</details>

</div>

### Credits

Thank you to all the people who have contributed!

<a href="https://github.com/komikku-app/anikku/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=aniyomiorg/aniyomi" alt="Anikku app contributors" title="Anikku app contributors" width="800"/>
</a>

![Visitor Count](https://count.getloli.com/get/@komikku-app?theme=capoo-2)

### Disclaimer

The developer(s) of this application does not have any affiliation with the content providers available, and this application hosts zero content.

<div align="left">

## License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 The Mihon Open Source Project
Copyright © 2024 The Aniyomi Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>

</div>
>>>>>>> official/master
