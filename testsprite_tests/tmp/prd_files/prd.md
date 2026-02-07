# Anikku (AniZen) Product Requirements Document

## 1. Executive Summary
Anikku is a high-performance, open-source anime tracking and streaming application for Android. It focuses on providing a tactile, fast, and feature-rich experience for anime enthusiasts.

## 2. Target Audience
Anime viewers who want a centralized library, seamless streaming from multiple sources, and deep integration with tracking services like MyAnimeList and AniList.

## 3. Core Features
### 3.1 Streaming & Search
- **Multi-source Support:** Scrape and stream anime from various providers via an extension system.
- **Global Search:** Search across all installed extensions simultaneously.
- **Video Player:** Advanced MPV-based player with support for gestures, haptics, and custom playback speeds.

### 3.2 Library Management
- **Favorites:** Save anime to a local library.
- **Categories:** Organize favorites into custom categories.
- **Tracking:** Auto-sync watch progress with external trackers.

### 3.3 Performance & UX
- **Haptic Feedback:** Premium tactile response across the UI.
- **Fast Loading:** Optimized list rendering and parallelized network requests.
- **Downloads:** Internal downloader with multi-threading support.

## 4. Technical Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Voyager for navigation.
- **Data:** SQLDelight for local storage, Injekt for DI.
- **Networking:** OkHttp, Jsoup for scraping.

## 5. Success Metrics
- Zero duplicate key crashes in Lazy lists.
- Frame timing below 16ms for 60fps smoothness.
- Successful tracker synchronization.
