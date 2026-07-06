# Development

This app is a native Android app developed against a self-hosted `hermes-webui` server exposed over real HTTPS. See [`PROJECT_SPEC.md`](PROJECT_SPEC.md) for the full product and API plan.

## Primary Test Target

Point this at your own `hermes-webui` server exposed through an HTTPS tunnel or reverse proxy (e.g. Cloudflare Tunnel). If the server sets `HERMES_WEBUI_PASSWORD`, you need that password to sign in.

Before debugging the app, verify the server is reachable:

```bash
curl https://<your-server>/health
```

## Building

Requirements:
- Android Studio (JDK 17) or command-line JDK 17
- Android SDK 35

```bash
cd android
./gradlew assembleDebug
```

Debug APK output: `android/app/build/outputs/apk/debug/app-debug.apk`

Public GitHub Releases are signed release APKs produced by `.github/workflows/version-bump.yml`.

Run tests:

```bash
cd android
./gradlew test
```

## Upstream Contract Pin

The app is tested against `hermes-webui` tag `v0.51.85`, peeled commit `f1d399b437c1ca7fe4b6d2093aebe334c32f34a3`. The root [`UPSTREAM_TESTED_SHA`](UPSTREAM_TESTED_SHA) file is the machine-readable pin for future drift checks and contract tests.

## Full-App Manual Regression Checklist

### Onboarding/Auth
- Fresh install shows connect screen.
- Valid server URL + password logs in.
- Wrong password shows clear error.
- Server/tunnel down shows useful error.
- Sign out and reconfigure returns to connect screen.

### Sessions
- Load sessions online.
- Pull to refresh.
- Search sessions.
- Create new session.
- Pin/unpin.
- Archive/restore.
- Delete disposable session only.

### Chat/Streaming
- Open existing session at latest message.
- Send normal message.
- Watch response stream.
- Stop response.
- Background/foreground during active stream.
- Long response over 2 minutes.
- Network interruption recovery.

### Composer
- Model picker and favorites/recents.
- Reasoning picker.
- Workspace picker.
- Profile switch.
- Attach file.
- Voice input.
- Slash commands: `/help`, `/new`, `/model`, `/workspace`, `/title`, `/skills`, `/steer`, `/interrupt`, `/undo`, `/retry`, `/compress`.

### Server Panels
- Files list/search/preview.
- Tasks list/detail/output.
- Skills list/search/detail.
- Memory notes/profile.
- Usage analytics.

### Build/Release
- Light and dark mode.
- App icon visible.
- Build passes lint.
- Tests pass.
