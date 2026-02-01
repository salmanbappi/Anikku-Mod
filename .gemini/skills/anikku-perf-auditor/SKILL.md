---
name: anikku-perf-auditor
description: Ensures AniZen remains snappy and responsive. Use when optimizing Compose UI, animations, or database queries.
---

# AniZen Performance Auditor

## Standards

### 1. Snappy Animations
- **Tab Transitions**: 100ms.
- **Icon Animations**: 150ms - 200ms.
- **Visibility Transitions**: 100ms.

### 2. Compose Optimization
- **Stable Keys**: Always provide a unique `key` in `LazyColumn`/`LazyGrid` items to minimize recomposition.
- **DerivedStateOf**: Use for expensive calculations based on other states.
- **IO Safety**: Ensure all heavy operations (Disk/Network) are wrapped in `withIOContext` or `launchIO`.

### 3. Tactile Feedback
Ensure haptic feedback is integrated into primary interaction points (Tabs, Toggles, Clicks) to maintain the "Premium" feel.