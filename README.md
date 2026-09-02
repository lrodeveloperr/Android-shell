# Android Shell

A native, reusable Jetpack Compose application shell for phones, foldables, and tablets. It deliberately contains no product-domain logic.

## What is included

- Material 3, edge-to-edge, dynamic color, light/dark mode, large text, RTL support
- Compact bottom navigation and expanded navigation rail using window width—not device names
- Configurable pager/single/disabled onboarding, versioned legal re-consent, Settings, help, paywall, and component lab
- A centralized, searchable semantic icon catalog backed by the complete Compose Material icon dependency
- Populated, empty, loading, and error states
- Anchored adaptive AdMob test banner that never overlays navigation or content
- Wired Google Play Billing 9 product queries, purchases, restore, acknowledgement, and verification seam
- UMP-gated AdMob initialization plus a required privacy-options entry point
- Adaptive/round/themed launcher icons, system splash, in-app mark, and checked 512 px Play asset
- DataStore persistence and system/English/Spanish per-app language selection
- CI contract validation, tests, lint, and a debug APK artifact

## Start a derived app

1. Change `applicationId`, package, display name, and the complete asset family in `branding/README.md`.
2. Edit `ShellConfig.kt` for onboarding, legal version, destinations, links, support, products, ads, and monetization.
3. Select reusable icons from `ShellIconCatalog.kt`; use stable semantic IDs in configuration and add current Material Symbol vector drawables there when a product needs a newer symbol.
4. Replace the placeholder feature area—not the adaptive shell.
5. Replace Google’s demo AdMob identifiers and implement consent before requesting production ads.
6. Configure matching products in Play Console and replace the development `PurchaseVerifier` with a trusted service.
7. Replace the legal placeholders with reviewed, app-specific documents and increment `legal.version` when acceptance must be renewed.
8. Run `bash scripts/validate-shell.sh --strict`; a derived release must not contain template identity or identifiers.

The exact locked/customizable boundary and supporting Android sources are in [`docs/LOCKED_SHELL_SPEC.md`](docs/LOCKED_SHELL_SPEC.md).

Every main-branch update is compiled from a clean runner before its APK artifact is used.

Open the repository in current Android Studio, or run `gradle assembleDebug` with JDK 17 and Android SDK 36.

## Shell rule

The shell removes repetitive UI decisions; it does not force every product into the same information architecture. Configure only the destinations and monetization a derived app needs. One top-level task should usually have no bottom navigation. Two to five destinations use the compact bar; wider windows promote them to a rail/sidebar.

All advertising identifiers committed here are Google’s test identifiers. Never ship them as a production configuration.

## Icon policy

The template exposes the full legacy Compose Material icon set for speed, while `ShellIconCatalog` provides the searchable, semantic subset used by shell screens. Google now recommends Material Symbols for new icons. Download only the required Android Vector Drawable from Google Fonts, add it to the catalog, and let R8 remove unused icon code from release builds. Decorative icons use a null content description; actionable icons require a localized description.
