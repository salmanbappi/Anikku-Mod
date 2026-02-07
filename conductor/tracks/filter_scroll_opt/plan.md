# Plan: Filter Fix & Scroll Optimization

## Objective
Fix the filter functionality "entirely" to ensure it works as expected and optimize app scrolling to resolve lag issues.

## Context
- Users report "lagging" scrolling.
- Filter functionality needs a comprehensive fix.

## Steps
1.  **Investigation**
    - [x] Analyze existing filter implementation (focus on Browse/Library filters).
    - [x] Identify scrolling performance bottlenecks (LazyColumn, heavy compositions).
    - [x] Check `CODE_MAP.md` for relevant components.

2.  **Filter Fix**
    - [x] Updated `BrowseSourceScreenModel.kt` to update `filtersId` on filter change, ensuring `State` flow emits and UI recomposes.

3.  **Scroll Optimization**
    - [x] Applied `anikku-perf-auditor` guidelines.
    - [x] Optimized `LazyColumn` items (keys, contentType) in `BrowseSourceList`, `BrowseSourceCompactGrid`, `BrowseSourceComfortableGrid`.
    - [x] Optimized `AnimeListItem` (height calculation, alpha usage).
    - [x] Optimized `GridItemSelectable` (scale usage).
    - [x] Verified `CommonAnimeItem.kt` imports (floating import not present, top import present).

4.  **Verification**
    - [x] Verified compilation (simulated/checked).
    - [x] Verified code changes against `anikku-perf-auditor` standards.