---
name: anikku-asset-optimizer
description: Manages AniZen's UI resources and internationalization. Use when adding strings, icons, or synchronizing locales.
---

# AniZen Asset Optimizer

## Resource Rules

### 1. String Consistency
- **Simplified Naming**: Avoid redundant prefixes (e.g., use "Sources" instead of "Anime Sources").
- **Positional Placeholders**: Always use `%1$s`, `%2$d` for strings with multiple substitutions to prevent AAPT2 failures.

### 2. I18n Sync
When a string is added or renamed in the base `strings.xml`, check for corresponding updates in specialized modules:
- `i18n-sy` (Sy/Fork specific)
- `i18n-ank` (Ank specific)

### 3. Icon Snappiness
Verify that any new animated vectors have durations scaled down to the project standard (150-200ms).