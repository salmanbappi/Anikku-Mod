# 🧠 AniZen Project Mind (Auto-Generated Map)

## 📂 Module Structure
- **anikku-dev**: Support Module
- **anikku-tracker**: Support Module
- **app**: Main UI & ViewModels
- **buildSrc**: Support Module
- **conductor**: Support Module
- **core**: Cross-module Utilities
- **core-metadata**: Support Module
- **data**: Repositories & Data Persistence
- **docs**: Support Module
- **domain**: Business Logic (Use Cases)
- **fastlane**: Support Module
- **flagkit**: Support Module
- **gradle**: Support Module
- **i18n**: Support Module
- **i18n-ank**: Support Module
- **i18n-kmk**: Support Module
- **i18n-sy**: Support Module
- **macrobenchmark**: Support Module
- **presentation-core**: Support Module
- **presentation-widget**: Support Module
- **scripts**: Support Module
- **source-api**: Support Module
- **source-local**: Support Module
- **telemetry**: Support Module

## 💎 Core Components
- **Player**: `app/src/main/java/eu/kanade/tachiyomi/ui/player`
  - Key Files: AniyomiMPVView.kt, CastManager.kt, ExternalIntents.kt, PipActions.kt, PlayerActivity.kt...
- **Downloader**: `app/src/main/java/eu/kanade/tachiyomi/data/download`
  - Key Files: DownloadCache.kt, DownloadJob.kt, DownloadManager.kt, DownloadNotifier.kt, DownloadPendingDeleter.kt...
- **Trackers**: `app/src/main/java/eu/kanade/tachiyomi/data/track`
  - Key Files: AnimeTracker.kt, BaseTracker.kt, DeletableTracker.kt, EnhancedTracker.kt, TrackStatus.kt...
- **DI (Injekt)**: `app/src/main/java/eu/kanade/tachiyomi/di`
  - Key Files: AppModule.kt, PreferenceModule.kt, SYPreferenceModule.kt...

## 🧪 Recent DI Activity (Injekt Discovery)
Detected 24 registered services in AppModule.
- `AndroidStorageFolderProvider`
- `Anime4KManager`
- `ChapterCache`
- `ConnectionsManager`
- `CoverCache`
- `Database`
- `DelayedTrackingStore`
- `DownloadCache`
- `DownloadManager`
- `DownloadProvider`
