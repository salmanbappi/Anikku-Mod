#!/bin/bash

# Anikku Performance Benchmark Suite
# Rationale: Automated performance tracking to eliminate UI jank and monitor system regression.

echo "🚀 Starting Anikku Benchmark Suite..."

# 1. Generate Baseline Profile
echo "📊 Generating Baseline Profiles (Optimization)..."
./gradlew :macrobenchmark:connectedBenchmark -P android.testInstrumentationRunnerArguments.class=tachiyomi.macrobenchmark.BaselineProfileGenerator

# 2. Run Frame Timing Benchmarks (Jank detection)
echo "📈 Measuring Frame Smoothness (Library Scrolling)..."
./gradlew :macrobenchmark:connectedBenchmark -P android.testInstrumentationRunnerArguments.class=tachiyomi.macrobenchmark.LibraryScrollingBenchmark

# 3. Run Startup Benchmarks
echo "⚡ Measuring App Startup Speed..."
./gradlew :macrobenchmark:connectedBenchmark -P android.testInstrumentationRunnerArguments.class=tachiyomi.macrobenchmark.ColdStartupBenchmark

echo "✅ Benchmarks complete. Results are available in macrobenchmark/build/outputs/connected_android_test_additional_output/"
