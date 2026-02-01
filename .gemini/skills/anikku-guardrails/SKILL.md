---
name: anikku-guardrails
description: Enforces AniZen's Clean Architecture and DI integrity. Use when adding modules, use cases, or registering services in Injekt.
---

# AniZen Guardrails

## Core Principles

### 1. Clean Architecture Boundaries
- **App**: Depends on `domain` and `data`. No direct DB access.
- **Domain**: Pure Kotlin. No Android or Data dependencies.
- **Data**: Implements `domain` repositories. Depends on `domain`.

### 2. Injekt Registration
Every new repository or interactor **must** be registered in `app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt`.
- Use `addSingletonFactory` for repositories.
- Use `addFactory` for interactors (Use Cases).

### 3. Verification
Before finalizing a feature, check that dependencies flow inwards toward the `domain` module.