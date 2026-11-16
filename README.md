<p align="center"><strong>Colay AI Android SDK</strong></p>
<p align="center">Install attribution, event tracking and conversion measurement for your mobile app</p>

<p align="center">
  <a href="https://github.com/gidysoft/colay-sdk-android/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/gidysoft/colay-sdk-android/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://jitpack.io/#gidysoft/colay-sdk-android"><img alt="JitPack" src="https://jitpack.io/v/gidysoft/colay-sdk-android.svg"></a>
  <img alt="minSdk 21" src="https://img.shields.io/badge/minSdk-21-3ddc84">
  <a href="LICENSE"><img alt="MIT" src="https://img.shields.io/badge/license-MIT-blue"></a>
</p>

[Colay AI](https://colayai.com) brings your ads, website, app and AI Agent conversations together in one dashboard. This SDK reports **which ad or link led to each install of your Android app**, which screens users visit, and key steps such as sign-up, add to cart and purchase. In the dashboard you see users coming from ads, funnels and sales broken down by campaign.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [API](#api)
- [Configuration](#configuration)
- [Privacy and consent](#privacy-and-consent)
- [Architecture](#architecture)
- [Example app](#example-app)
- [Development and testing](#development-and-testing)
- [Troubleshooting](#troubleshooting)

## Features

- **Install attribution:** exact install source via the Google Play Install Referrer; UTM parameters and Meta / Google / TikTok click IDs (`fbclid`, `gclid`, `ttclid`, `ctwa_clid`).
- **Events:** screens, standard and custom events, purchases (amount and currency), user identity.
- **Automatic events:** `app_install`, `app_open`, `app_background`, `session_start`, `deeplink_open`.
- **Reliable delivery:** persistent on-device queue, batched uploads, exponential backoff on network errors.
- **Privacy:** nothing is sent without consent (GDPR / KVKK); the advertising ID is not read by default.
- **Lightweight:** a single dependency (`com.android.installreferrer`); usable from Kotlin and Java.

## Requirements

| | |
|---|---|
| Android | 5.0 (API 21) or later |
| Language | Kotlin 1.9+ or Java 8+ |
| Colay AI | Dashboard → Ads → **Tracking setup** → Android source (app ID = your `applicationId`) |

## Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.gidysoft:colay-sdk-android:0.1.2")
}
```

## Quick start

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Colay.init(this, "colay_pk_xxx", ColayConfig(defaultConsent = Consent.DENIED))
    }
}

// Once the user grants consent
Colay.setConsent(Consent.GRANTED)

// Screens, events, users, purchases
Colay.screen("Product detail", mapOf("productId" to "thermos-500"))
Colay.track("add_to_cart", mapOf("productId" to "thermos-500"))
Colay.identify("u_123", mapOf("email" to "jane@example.com"))
Colay.purchase(149.90, "USD", mapOf("orderId" to "A-1001"))

// Deep links received while the app is running
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.data?.let(Colay::handleDeepLink)
}
```

The publishable key (`colay_pk_…`) is not a secret: it can only send events, never read data.

## API

| Call | What it does |
|---|---|
| `Colay.init(context, key, config)` | Initializes the SDK (once, in `Application.onCreate`) |
| `Colay.setConsent(consent)` | Sets consent and persists it on the device |
| `Colay.screen(name, properties)` | Sends a `screen_view` event |
| `Colay.track(event, properties)` | Sends a standard or custom event (name up to 64 characters) |
| `Colay.identify(userId, traits)` | Identifies the user; earlier anonymous events are linked to them |
| `Colay.purchase(amount, currency, properties)` | Sends a `purchase` event, used in revenue reports |
| `Colay.handleDeepLink(uri)` | Reads campaign data from the link that opened the app |
| `Colay.reset()` | Clears the user and session on sign-out |
| `Colay.flush()` | Sends the queue right away |

**Standard event names:** `sign_up`, `login`, `logout`, `lead`, `add_to_cart`, `begin_checkout`, `purchase`, `subscription_start`, `subscription_cancel`, `tutorial_complete`, `level_complete`. Full contract: [PROTOCOL.md](PROTOCOL.md).

## Configuration

| `ColayConfig` field | Default | Description |
|---|---|---|
| `baseUrl` | `https://colayai.com` | Server |
| `defaultConsent` | `Consent.GRANTED` | `Consent.DENIED` is recommended for users covered by GDPR / KVKK |
| `flushAt` / `flushIntervalSeconds` | 20 / 15 | Batch size threshold and upload interval |
| `maxQueueSize` | 1000 | Maximum number of events kept on the device |
| `sessionTimeoutSeconds` | 1800 | Inactivity before a new session starts |
| `trackLifecycle` | `true` | Automatic app lifecycle events |
| `trackActivities` | `false` | `screen_view` whenever an Activity resumes |
| `trackDeepLinks` | `true` | Campaign data from the launch link |
| `collectAdvertisingId` | `false` | Google advertising ID (requires `play-services-ads-identifier` and a Play Console declaration) |
| `debug` | `false` | Verbose logging in Logcat with the `ColaySDK` tag |

## Privacy and consent

| Consent | Behaviour |
|---|---|
| `Consent.DENIED` | No events are sent; the queue is cleared |
| `Consent.ANALYTICS_ONLY` | Events are sent without the advertising ID and click IDs; the server does not forward them to ad platforms |
| `Consent.GRANTED` | Everything |

Email addresses and phone numbers travel over TLS; the server stores them hashed with SHA-256. For the Play Console "Data safety" form: app interactions, device IDs (anonymous), and optionally email / phone (if you use `identify`).

## Architecture

```
Colay (public API, thread-safe singleton)
  └─ ColayClient ── session, consent, lifecycle, deep links
       ├─ EventQueue ── persistent on-device queue (file), limits and trimming
       ├─ Transport ─── HTTPS upload; handles 200 / 400 / 401 / 413 / 429 / 5xx responses
       ├─ Attribution ─ parses UTM parameters and click IDs
       └─ Platform ──── device info, Install Referrer, advertising ID
```

Every call returns immediately; work runs in order on a single background thread. The public surface is only `Colay`, `ColayConfig` and `Consent`; everything else is `internal`.

The example app follows **MVVM**:

```
ui/main/MainActivity ─▶ MainRoute (Compose) ─▶ MainViewModel ─▶ AnalyticsTracker (interface)
                                              StateFlow<MainUiState>        └─ ColayAnalyticsTracker ─▶ Colay
```

The screen only renders state; business logic lives in `MainViewModel`; analytics calls sit behind the `AnalyticsTracker` interface. This lets the ViewModel be tested with a fake tracker (`example/src/test`), and you can use the same structure in your own app.

## Example app

```properties
# local.properties (not committed)
colay.key=colay_pk_xxx
colay.baseUrl=https://colayai.com
```

```bash
./gradlew :example:installDebug
adb shell am start -a android.intent.action.VIEW -d "colayexample://campaign?utm_source=instagram&utm_campaign=test" com.colayai.example
```

Enter `com.colayai.example` as the app ID of your tracking source. Events appear in the dashboard under Tracking setup → **Test** and User journey → **Events**. To use a local server from the emulator, set `colay.baseUrl=http://10.0.2.2:5050`.

## Development and testing

```bash
./gradlew :colay:testDebugUnitTest :example:testDebugUnitTest   # unit tests
./gradlew :colay:assembleRelease                                 # AAR
./gradlew :colay:publishReleasePublicationToMavenLocal           # the publication JitPack runs
```

To release: bump `version` and `SDK_VERSION` in `colay/build.gradle.kts`, add a `## [x.y.z]` section to [CHANGELOG.md](CHANGELOG.md), merge to `main`, then `git tag x.y.z && git push origin x.y.z`. The Release workflow checks that the tag matches the code version, runs the tests, publishes a GitHub Release with the notes from the CHANGELOG and the AAR attached, and triggers the JitPack build.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| No events arrive | Consent was not granted (`Colay.setConsent`) or `Colay.init` was not called |
| 401 / 403 | Wrong key, disabled source, or the package name does not match the source |
| Events show up late | Events are batched; call `Colay.flush()` to send immediately |
| Install source is empty | The app was installed outside the Play Store (e.g. `adb install`); store installs fill it in |

---

[Changelog](CHANGELOG.md) · [Releases](https://github.com/gidysoft/colay-sdk-android/releases) · [Security](SECURITY.md) · [Contributing](CONTRIBUTING.md) · [MIT License](LICENSE) © Gidysoft
