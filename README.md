# Hermex

Hermex is a native SwiftUI iPhone app for driving a self-hosted [hermes-webui](https://github.com/nesquena/hermes-webui) server from iOS — a mobile cockpit for an AI agent that lives on a machine you control. The Xcode target and scheme are `HermesMobile`; the app's display name is `Hermex`.

The phone is the control plane, not the compute plane: sign in to your server, browse and search sessions, send messages with model/reasoning/workspace options and attachments, watch responses stream in real time, steer or stop a run, browse workspace files, and open read-only Tasks, Skills, Memory, and Usage Analytics panels.

## You need your own server

Hermex is a client only. It does not ship with, host, or provision a backend. Every user runs their own [hermes-webui](https://github.com/nesquena/hermes-webui) server (a third-party, MIT-licensed open-source project) on a machine they control — self-hosting it, securing it, and keeping it reachable are your responsibility.

Common ways to make the server reachable from your phone:

- **HTTPS via a tunnel or reverse proxy (recommended).** Expose the server through Cloudflare Tunnel or any reverse proxy that terminates real TLS at a hostname you own. Real HTTPS keeps iOS App Transport Security happy with no exceptions. Set `HERMES_WEBUI_PASSWORD` on the server — on a publicly reachable hostname the password is your only app-level defense.
- **Tailscale.** Run the server bound to all interfaces with a password, install Tailscale on both the server and the iPhone, and connect to `http://<tailnet-ip>:8787`. The app allows plain HTTP only for Tailscale's `100.64.0.0/10` device range.
- **Simulator-only local testing** can use `http://localhost:8787` when the server runs on the same Mac.

Onboarding starts with an empty server field; enter your reachable URL, for example:

```text
https://hermes.yourdomain.com
```

If connection testing fails, check these first:

1. The machine hosting `hermes-webui` is awake.
2. `hermes-webui` is running and serving `/health` (`curl https://<your-server>/health`).
3. The tunnel, reverse proxy, or Tailscale route is connected.
4. The server URL and password are correct.

## Requirements

- Xcode 26 or newer (iOS 18 SDK), Swift 5.9+
- An iPhone or iOS simulator running iOS 18+
- A reachable `hermes-webui` server (see above)

## Building from source

Clone the repo, open `HermesMobile.xcodeproj`, and run the `HermesMobile` scheme on an iPhone simulator. Dependencies are resolved automatically via Swift Package Manager.

From the command line:

```zsh
xcodebuild -project HermesMobile.xcodeproj -scheme HermesMobile -destination 'platform=iOS Simulator,name=iPhone 17' build
```

```zsh
xcodebuild test -project HermesMobile.xcodeproj -scheme HermesMobile -destination 'platform=iOS Simulator,name=iPhone 17'
```

If that simulator is not installed, list available devices and choose a nearby iPhone simulator:

```zsh
xcrun simctl list devices available
```

Local validation defaults for XcodeBuildMCP users live in `.xcodebuildmcp/config.yaml`; the standard post-change flow is in [`DEVELOPMENT.md`](DEVELOPMENT.md).

## Server compatibility

The app is developed and tested against the `hermes-webui` commit pinned in [`UPSTREAM_TESTED_SHA`](UPSTREAM_TESTED_SHA). Upstream does not yet guarantee API stability (its README declares version skew unsupported pending their stable-API work), so newer or older server versions may break individual features — please include your server version in bug reports. The app decodes tolerantly (unknown fields never crash it) and endpoint shapes are verified against upstream source, never invented; see [`CONTRACT_TESTS.md`](CONTRACT_TESTS.md) for the contract-testing approach.

## Documentation map

- [`PROJECT_SPEC.md`](PROJECT_SPEC.md): source of truth for product scope, API behavior, dependencies, and architecture decisions.
- [`PROJECT_INTENT.md`](PROJECT_INTENT.md): short orientation; useful for product tradeoffs, not implementation details.
- [`DEVELOPMENT.md`](DEVELOPMENT.md): local development workflow, server setup notes, and the maintainer release runbook.
- [`TESTFLIGHT.md`](TESTFLIGHT.md): maintainer-only TestFlight/App Store Connect operations.
- [`CONTRACT_TESTS.md`](CONTRACT_TESTS.md): upstream contract-test readiness and the pin-advance policy.
- [`docs/agents/`](docs/agents): repo-local agent workflow conventions (issues, triage labels, domain notes).
- [GitHub Issues](https://github.com/uzairansaruzi/hermex/issues): source of truth for active bugs, polish notes, and feature requests.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for how to pick up work and open a PR, and [`AGENTS.md`](AGENTS.md) for the working agreement coding agents follow in this repo. The short version:

- Do not invent API endpoints or JSON shapes; verify against the upstream server source or a running server.
- Every `Codable` model decodes tolerantly — never crash on unknown fields.
- Add no third-party dependencies beyond the locked list in `PROJECT_SPEC.md` without explicit approval.
- Do not modify the upstream `hermes-webui` server from this repo.

## License

MIT — see [LICENSE](LICENSE).
