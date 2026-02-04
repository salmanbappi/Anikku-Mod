# AniZen Project Plan

## Current Objectives
- [x] **Project Tracking & CI/CD**
    - [x] Initialize Dev Skill
    - [x] Auto-Release Workflow (Unique builds per push)
    - [x] APK Architecture-specific naming
- [x] **Player Customization**
    - [x] Implement 2x speed on long press
    - [x] Add more gesture haptics (volume/brightness limits)
- [x] **Download Improvements (1DM+ Style)**
    - [x] Increase concurrent episode limit to 30
    - [x] Implement multi-threaded chunked downloader for direct files
    - [x] Add "Internal Downloader" settings UI
- [x] **Animation & UX**
    - [x] Smooth transitions (Material Shared Axis)
    - [x] Fast response (IO-offloaded downloading)
    - [x] Removed non-official build warnings (F-Droid cleanup)
- [x] **Haptic Feedback Integration**
    - [x] Navigation bar haptics
    - [x] Settings Toggles/Switches haptics
    - [x] Text Preference haptics
    - [x] Library and Search item click haptics (Premium feel)
    - [x] Episode list item click haptics
    - [x] Logo header interaction haptics
- [x] **Performance Optimizations**
    - [x] Optimized episode list loading with batch download status checking
    - [x] Parallel global search (default 5 threads)
    - [x] Enhanced R8/Proguard optimizations for Release builds
    - [x] Smart Font Caching (Startup optimization)
    - [ ] Optimize Compose LazyGrid keys (Reduce recomposition)
    - [x] Optimize Video Sync (Fix playback lag)
    
    ## Engineering Statistics (Ongoing)
    - [ ] **Stability**: Increase Unit Test coverage for Network & Download modules (Target: 25%)
    - [ ] **Performance**: Establish Frame Timing Benchmarks for Library/History screens (Target: <2% Janky Frames)
    - [ ] **Hygiene**: Enforce zero-warning builds using strict Detekt configuration
    - [x] **Efficiency**: Implemented Semaphore-based Memory Flow Control for 30-thread downloading (Zero-Delay logic)
    - [x] **UI Rigor**: Overhauled Statistics page (Radar Charts, Monospaced Data, Technical Rationale) to eliminate "AI slop" perception.
    
    ## Context- Project: AniZen
- Platform: Android (Kotlin/Compose)
- Focus: Performance, Tactile Feedback, Speed.