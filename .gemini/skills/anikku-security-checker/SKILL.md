---
name: anikku-security-checker
description: Audits AniZen for enterprise-level security and data privacy. Use when updating R8 rules or handling sensitive user data.
---

# AniZen Security Checker

## Audit Checklist

### 1. R8 Full Mode
Ensure `android.enableR8.fullMode=true` is present in `gradle.properties`. Review `proguard-rules.pro` if classes are missing due to aggressive stripping.

### 2. Data Privacy
- **Encrypted Storage**: Use `SQLCipher` for DB and `EncryptedSharedPreferences` for tokens.
- **No Leaks**: Ensure no API keys, tokens, or PII (Personally Identifiable Information) are logged in Release builds.

### 3. Obfuscation
Verify that core logic (especially the multi-threaded downloader) is properly obfuscated to prevent reverse engineering.