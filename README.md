# Android Shell

A native, reusable Jetpack Compose application shell for phones, foldables, and tablets. It deliberately contains no product-domain logic.

## AI coding tools

Start with [`AGENTS.md`](AGENTS.md). It is the operational source map for the locked shell boundary, safe customization order, feature-canvas contract, advertising flavors, monetization enforcement, localization and release blockers.

## What is included

- Material 3, edge-to-edge, dynamic color, light/dark mode, large text, RTL support
- Compact bottom navigation and expanded navigation rail using window width—not device names
- Legal-only first launch by default, optional pager/single-page education, versioned legal re-consent, Settings, help, paywall, and component lab
- A centralized, searchable semantic icon catalog backed by the complete Compose Material icon dependency
- Populated, empty, loading, and error states
- Anchored adaptive AdMob test banner that never overlays navigation or content
- All seven monetization profiles enforced by one access policy, including persistent, deduplicated usage caps
- Google Play Billing 9 product queries, purchases, pending state, restore, acknowledgement, signature verification, revocation refresh, and bounded offline entitlement cache
- UMP-gated AdMob initialization plus a required privacy-options entry point
- Adaptive/round/themed launcher icons, system splash, in-app mark, and checked 512 px Play asset
- DataStore persistence and a locked 31-language shared-core bundle with native per-app language selection
- Injectable Compose `FeatureCanvas`; derived apps replace product functionality without forking shell mechanics
- Optional native backup provider seam, disabled and network-free by default
- Purchase-surface guard: paywalls, restore, win-back, and purchase promotions contain no app logo or brand-mark imagery
- Ad-free build switch that removes the advertising-ID manifest declaration and never initializes UMP/AdMob
- CI contract validation, tests, lint, and a debug APK artifact

## Start a derived app

1. Change `applicationId`, package, display name, and the complete asset family in `branding/README.md`.
2. Edit `ShellConfig.kt` for onboarding, legal version, destinations, links, support, products, ads, usage cap, offline grace, and monetization.
3. Select reusable icons from `ShellIconCatalog.kt`; use stable semantic IDs in configuration and add current Material Symbol vector drawables there when a product needs a newer symbol.
4. Pass your Compose implementation as `featureCanvas` to `ShellApp`. Call `reportSuccessfulAction(stableId)` only after a metered domain operation succeeds; retries with the same ID do not consume the cap twice.
5. Replace Google’s demo AdMob identifiers and implement consent before requesting production ads.
6. Configure matching products in Play Console and set the licensing public key for local signature verification, or inject a trusted asynchronous `PurchaseVerifier`. Blank or failed verification never grants access.
7. Replace the legal placeholders with reviewed, app-specific documents and increment `legal.version` when acceptance must be renewed.
8. Run `bash scripts/validate-shell.sh --strict ads` or `bash scripts/validate-shell.sh --strict noAds` for the artifact you will ship; a derived release must not contain template identity or incompatible identifiers.

The exact locked/customizable boundary and supporting Android sources are in [`docs/LOCKED_SHELL_SPEC.md`](docs/LOCKED_SHELL_SPEC.md).

The included GitHub workflow is manual-only so it does not consume hosted minutes unexpectedly. Run it explicitly when hosted verification is wanted; otherwise use the same commands locally.

Open the repository in current Android Studio, or run `gradle assembleDebug` with JDK 17 and Android SDK 36.

Build `assembleAdsRelease` for an ads-capable artifact. Build `assembleNoAdsRelease` for an artifact that cannot request advertising ID access and never initializes the ad stack. Both native flavors compile in CI.

## Monetization behavior

| Profile | Shell-enforced behavior |
|---|---|
| `Free` | Feature canvas is always available; no ads or purchase entry point. |
| `Ads` | Feature canvas is available; UMP-gated banner may appear. |
| `AdsWithRemovePurchase` | Feature canvas is available; a verified one-time product suppresses ads. |
| `OneTimeUnlock` | Feature canvas fails closed until a verified one-time product is active. |
| `Subscription` | Feature canvas fails closed until a verified subscription is active. |
| `UsageCapWithOneTimeUnlock` | Unique successful actions consume the durable free allowance, then require a one-time product. |
| `UsageCapWithSubscription` | Unique successful actions consume the durable free allowance, then require an active subscription. |

An authoritative successful Play refresh replaces the entitlement cache, so refunds and revocations close access. Failed/offline refreshes do not erase a valid cache. One-time purchases remain cached until an authoritative refresh; subscriptions use the configured offline grace window (maximum seven days).

## Shell rule

The shell removes repetitive UI decisions; it does not force every product into the same information architecture. Configure only the destinations and monetization a derived app needs. One top-level task should usually have no bottom navigation. Two to five destinations use the compact bar; wider windows promote them to a rail/sidebar.

All advertising identifiers committed here are Google’s test identifiers. Never ship them as a production configuration.

## Icon policy

The template exposes the full legacy Compose Material icon set for speed, while `ShellIconCatalog` provides the searchable, semantic subset used by shell screens. Google now recommends Material Symbols for new icons. Download only the required Android Vector Drawable from Google Fonts, add it to the catalog, and let R8 remove unused icon code from release builds. Decorative icons use a null content description; actionable icons require a localized description.


## Release control

The locked shell contract is **2.1.0**. Before shipping a derived app:

1. Complete [the app policy profile](docs/APP_POLICY_PROFILE.md); no `DECISION_REQUIRED` values may remain.
2. Work through [the store compliance checklist](docs/STORE_COMPLIANCE_CHECKLIST.md), including every conditional policy that the app activates.
3. Execute [the UI regression matrix](docs/UI_REGRESSION_MATRIX.md) on the actual release configuration.
4. Review [shell migrations](docs/SHELL_MIGRATIONS.md) when adopting a newer shell contract.
5. Run `scripts/validate-shell.sh --strict` locally. GitHub Actions remain manual-only.

A reusable shell can enforce a strong baseline, but store approval also depends on the derived app’s functionality, SDKs, metadata, audience, category, territories, accounts, and current policy. The checklist is a release gate, not an approval guarantee.
