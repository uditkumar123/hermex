<div align="center">

# Hermex

**Control your self-hosted [Hermes](https://github.com/nesquena/hermes-webui) agent from your phone.**

Your server. Your phone. No middleman.

[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)
[![Android APK](https://img.shields.io/badge/APK-free-brightgreen?logo=android)](https://github.com/uditkumar123/hermex/releases)
[![Google Play](https://img.shields.io/badge/Google_Play-$1.99-3DDC84?logo=googleplay)](https://play.google.com/store/apps/details?id=com.hermex.app)
[![iOS](https://img.shields.io/badge/iOS-18%2B-000000?logo=apple&logoColor=white)](https://apps.apple.com/app/hermex/id6767006319)

[Report a bug](https://github.com/uditkumar123/hermex/issues) · [Contributing](CONTRIBUTING.md)

</div>

Hermex is a native mobile client for driving a self-hosted [hermes-webui](https://github.com/nesquena/hermes-webui) server — a mobile cockpit for an AI agent that lives on a machine **you** control. The phone is the control plane, not the compute plane: the agent, its tools, and your data stay on your own hardware.

- **Private.** No analytics, no tracking, no third-party relay — the app talks only to your server.
- **Native.** Built with platform-native UI (SwiftUI for iOS, Jetpack Compose for Android).
- **Free APK on GitHub** — sideload for free. **$1.99 on Google Play** — supports development. No subscriptions, no ads.

## Platforms

| Platform | Channel | Price | Min Version |
|----------|---------|-------|-------------|
| iOS | App Store | Free | iOS 18+ |
| Android | Google Play | $1.99 | Android 8.0+ (API 26) |
| Android | GitHub (APK) | Free | Android 8.0+ (API 26) |

## Features

- **Chat with your agent** — send messages with model, workspace, and profile options; watch responses stream in real time with thinking and tool-call detail.
- **Steer or stop a run** mid-flight.
- **Sessions** — browse, search, and resume every conversation on your server; cached sessions stay readable offline.
- **Pick your models** — switch between any model or provider your server is configured for.
- **Profiles & projects** — switch agent profiles and organize sessions into projects.
- **Offline support** — view cached sessions when disconnected from your server.

## Getting started

Hermex is a client only — it does not ship with, host, or provision a backend. You bring your own [hermes-webui](https://github.com/nesquena/hermes-webui) server running on a machine you control.

### 1. Run the server

Install and start `hermes-webui` on macOS, Linux, or Windows/WSL2 (Python 3.11+). Set `HERMES_WEBUI_PASSWORD`.

### 2. Make it reachable from your phone

- **HTTPS via a tunnel or reverse proxy (recommended).** Expose the server through Cloudflare Tunnel or any reverse proxy that terminates real TLS at a hostname you own.
- **Tailscale.** Run the server bound to all interfaces with a password, install Tailscale on both the server and your phone, and connect via the Tailscale IP.
- **Local testing** can use `http://localhost:8787` when the server runs on the same machine.

### 3. Connect

**Android (free):** [Download the APK](https://github.com/uditkumar123/hermex/releases) from GitHub Releases.
**Android (paid):** [Get it on Google Play](https://play.google.com/store/apps/details?id=com.hermex.app) for $1.99 — supports development.
**iOS:** [Download from the App Store](https://apps.apple.com/app/hermex/id6767006319).

Enter your server URL (e.g. `https://hermes.yourdomain.com`) and password, and you're in.

## Building from source

### Android

Requirements:
- Android Studio (JDK 17)
- Android SDK 35

```bash
cd android
./gradlew assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`

To bump version and build:
```bash
.\scripts\build-apk.ps1              # patch bump (default)
.\scripts\build-apk.ps1 -BumpType minor  # minor bump
.\scripts\build-apk.ps1 -BumpType major  # major bump
```

### iOS

Requirements:
- Xcode 26+ (iOS 18 SDK)
- iPhone or simulator on iOS 18+

```bash
xcodebuild -project HermesMobile.xcodeproj -scheme HermesMobile -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Server compatibility

The app is developed and tested against the `hermes-webui` upstream. The app decodes tolerantly (unknown fields never crash it) and endpoint shapes are verified against upstream source.

## Automation

- **Pre-commit hook** — verifies Android build passes before commits
- **GitHub Actions CI** — builds and lints on every push/PR
- **Auto version bump** — patch version increments automatically on merge to master
- **Changelog generator** — generates `CHANGELOG.md` from conventional commits

## Contributing

Contributions are welcome — see [`CONTRIBUTING.md`](CONTRIBUTING.md) for how to pick up work and open a PR.

- Do not invent API endpoints or JSON shapes; verify against the upstream server source.
- Every model decodes tolerantly — never crash on unknown fields.
- Do not modify the upstream `hermes-webui` server from this repo.

## License

MIT — see [LICENSE](LICENSE).

Hermex is an independent client and is not affiliated with the upstream [hermes-webui](https://github.com/nesquena/hermes-webui) project.
