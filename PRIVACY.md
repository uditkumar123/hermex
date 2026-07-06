# Privacy Policy

Last updated: July 3, 2026

Hermex does not collect, sell, or relay personal data. The app connects directly to your self-hosted Hermes WebUI server over a URL you provide; communication is between your device and your server.

## Data handling

- **No accounts.** Hermex has no sign-up, no registration, and no cloud backend.
- **No analytics.** No tracking SDKs, no crash reporters, no telemetry.
- **No app telemetry.** The app does not send analytics, crash reports, or usage telemetry to the Hermex developer.
- **No third-party relay.** The app talks exclusively to your server.

## What the app stores locally

- Your server URL and optional custom headers, stored locally in the app sandbox.
- Session cookies from your server, stored with Android encrypted preferences when available in release builds.
- Cached session and message data so conversations are readable offline.

Your password is sent to your configured server during login and is not intentionally stored by the app. Local cached data never leaves your device except when you choose to communicate with your configured server.

## Contact

If you have questions, open an issue at https://github.com/uzairansaruzi/hermex/issues.
