<div align="center">

# Hermes Companion

**A native mobile client for your self-hosted Hermes WebUI server.**

Your phone becomes the control plane for your AI agent — the agent, its tools, and your data stay on your own hardware.

[![License: MIT](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)
[![Android APK](https://img.shields.io/badge/APK-free-brightgreen?logo=android)](https://github.com/uditkumar123/hermex/releases)
[![Google Play](https://img.shields.io/badge/Google_Play-$1.99-3DDC84?logo=googleplay)](https://play.google.com/store/apps/details?id=com.hermex.app)

[Report a bug](https://github.com/uditkumar123/hermex/issues) · [Contributing](CONTRIBUTING.md)

</div>

## Features

- **Chat with your agent** — send messages with model, workspace, and profile controls; watch responses stream in real time with thinking and tool-call detail.
- **Sessions** — browse, search, and resume every conversation on your server. Cached sessions stay readable offline.
- **Steer or stop runs** mid-flight.
- **Switch models, profiles, and projects** on the fly.
- **Private by design** — no accounts, no tracking, no analytics. The app talks only to your server.

## Requirements

- A running [Hermes WebUI](https://github.com/nesquena/hermes-webui) server (self-hosted on your own machine or VPS).
- Your server must be reachable from your phone via HTTPS, Tailscale, or local network.

## Platforms

| Platform | Channel | Price | Min Version |
|----------|---------|-------|-------------|
| Android | Google Play | $1.99 | Android 8.0+ (API 26) |
| Android | GitHub (APK) | Free | Android 8.0+ (API 26) |

## Getting started

### 1. Run the server

Install and start `hermes-webui` on macOS, Linux, or Windows/WSL2 (Python 3.11+). Set `HERMES_WEBUI_PASSWORD`.

### 2. Make it reachable from your phone

- **HTTPS via a tunnel or reverse proxy (recommended).** Expose the server through Cloudflare Tunnel or any reverse proxy that terminates real TLS at a hostname you own.
- **Tailscale.** Run the server bound to all interfaces with a password, install Tailscale on both the server and your phone, and connect via the Tailscale IP.
- **Local network.** Use `http://` for devices on the same LAN or Tailscale network.

### 3. Connect

**Android (free):** [Download the APK](https://github.com/uditkumar123/hermex/releases) from GitHub Releases.
**Android (paid):** [Get it on Google Play](https://play.google.com/store/apps/details?id=com.hermex.app) for $1.99 — supports development.

Enter your server URL and password, and you're in.

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

Hermes Companion is an independent client and is not affiliated with the upstream [hermes-webui](https://github.com/nesquena/hermes-webui) project.
