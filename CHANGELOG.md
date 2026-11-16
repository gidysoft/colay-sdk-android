# Changelog

All notable changes to this project are documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: [SemVer](https://semver.org/).

## [0.1.2] - 2026-11-17

Initial public release.

### Added
- Public API: `init`, `track`, `screen`, `identify`, `purchase`, `setConsent`, `handleDeepLink`, `reset`, `flush`.
- Install attribution via the Google Play Install Referrer; UTM parameters and click IDs (`fbclid`, `gclid`, `ttclid`, `ctwa_clid` and more).
- Automatic `app_install`, `app_open`, `app_background`, `session_start` and `deeplink_open` events.
- Persistent on-device queue, batched uploads and exponential backoff.
- Consent management (analytics / ads); the advertising ID is opt-in.
- Example app built with MVVM (Jetpack Compose, `ViewModel` + `StateFlow`, `AnalyticsTracker` interface) and ViewModel unit tests.
- GitHub Actions: build and unit tests on every change; automatic GitHub Releases from version tags.
- `README.md`, `PROTOCOL.md`, `SECURITY.md`, `CONTRIBUTING.md`.
