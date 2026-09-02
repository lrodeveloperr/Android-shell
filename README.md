# Android Shell

A native, reusable Jetpack Compose application shell for phones, foldables, and tablets. It deliberately contains no product-domain logic.

## What is included

- Material 3, edge-to-edge, dynamic color, light/dark mode, large text, RTL support
- Compact bottom navigation and expanded navigation rail using window width—not device names
- Onboarding, placeholder legal pages, Settings, help, paywall, restore seam, and component lab
- Populated, empty, loading, and error states
- Anchored adaptive AdMob test banner that never overlays navigation or content
- Google Play Billing 9 integration seam with a safe demo entitlement
- English and Spanish resources
- CI that tests, lints, and publishes a debug APK artifact

## Start a derived app

1. Change `applicationId`, package, display name, and launcher art.
2. Edit `ShellConfig.kt` for destinations, links, support, products, and monetization.
3. Replace the placeholder feature area—not the adaptive shell.
4. Replace Google’s demo AdMob identifiers and implement consent before requesting production ads.
5. Configure products in Play Console, query `ProductDetails`, verify purchases on a trusted service, and acknowledge them.
6. Replace the legal placeholders with reviewed, app-specific documents.

Open the repository in current Android Studio, or run `gradle assembleDebug` with JDK 17 and Android SDK 36.

## Shell rule

The shell removes repetitive UI decisions; it does not force every product into the same information architecture. Configure only the destinations and monetization a derived app needs. One top-level task should usually have no bottom navigation. Two to five destinations use the compact bar; wider windows promote them to a rail/sidebar.

All advertising identifiers committed here are Google’s test identifiers. Never ship them as a production configuration.
