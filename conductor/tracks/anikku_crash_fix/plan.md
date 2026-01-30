# Track: AniZen Crash Fixes
**Goal:** Resolve `NoSuchElementException` in player and `InjektionException` for `StorageManager`.

### Status: In Progress

### Tasks
- [x] **Fix Decoder Enum Crash:**
    - [x] Identify root cause: `getDecoderFromValue` failing on combined `hwdec` strings.
    - [x] Implement robust `getDecoderFromValue` with `firstOrNull` and fallback to `Decoder.Auto`.
    - [x] Commit and push fix. (Committed in a72eabe)
- [ ] **Investigate StorageManager DI Failure:**
    - [ ] Analyze `InjektionException: No registered instance for StorageManager`.
    - [ ] Note: Log date (Jan 28) differs from current build (Jan 30). Might be a stale issue.
    - [ ] Action: Monitor if this persists in the new build.
- [ ] **Verification:**
    - [ ] Monitor GitHub Actions build. (Currently running)
    - [ ] Verify fix on device. (Pending user test)
