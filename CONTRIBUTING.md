# Contributing to Hermex

Thanks for your interest in contributing! This document covers local setup,
running tests, and the PR workflow. Please also
read the [Code of Conduct](CODE_OF_CONDUCT.md).

## Local setup

- **Android Studio** or command-line JDK 17+ with Android SDK 35.
- Clone the repo and open the `android/` directory in Android Studio.
- To actually use the app you need your own
  [hermes-webui](https://github.com/nesquena/hermes-webui) server — the app is
  a client only. See the [README](README.md) for
  reachable-server options (Cloudflare Tunnel, reverse proxy, Tailscale, or
  local network).

## Running tests

The full Gradle test suite must pass before any PR:

```bash
cd android && ./gradlew test
```

The same suite runs in CI on every pull request.

## Building the APK

```bash
cd android && ./gradlew assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`

Signed release builds require a keystore. See `android/app/build.gradle.kts` for
the signing configuration.

## What PRs we welcome (and what we don't)
