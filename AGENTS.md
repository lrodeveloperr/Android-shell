# AI implementation guide

This repository is a reusable **native Android shell**. It is Kotlin and Jetpack Compose only. The shell owns repetitive application infrastructure; a derived app supplies its product logic through the feature-canvas boundary.

Read this file before changing code. The locked shell contract is **2.1.0**; read `docs/SHELL_MIGRATIONS.md` before upgrading a derived app.

## Non-negotiable architecture

- Keep the application 100% native Kotlin and Jetpack Compose.
- Do not introduce Flutter, React Native, WebView application shells, XML screen layouts, or another UI framework.
- Preserve Material 3, edge-to-edge layout, adaptive navigation, accessibility, RTL and large-text behavior.
- Keep private user data local unless the derived product specification explicitly authorizes a service.
- Do not fork or duplicate shell screens inside product code.
- Do not expose the feature canvas until `resolveFeatureAccess` returns an allowed result.
- Never grant paid access from a UI toggle, cached boolean, purchase callback alone, or unverified Play response.
- Never place the app logo, launcher icon, brand-mark image, or decorative app identity artwork on a paywall, purchase, subscription, restore, win-back, or promotional-purchase surface.
- Complete `docs/APP_POLICY_PROFILE.md` and `docs/STORE_COMPLIANCE_CHECKLIST.md` for every derived app. Strict validation must remain blocked while `DECISION_REQUIRED` exists.
- GitHub Actions are manual-only. Never trigger a workflow unless the user explicitly requests it.

## Five-minute source map

| Concern | Authoritative source |
|---|---|
| App identity and dependencies | `app/build.gradle.kts`, manifests and `app/src/main/res` |
| Per-app shell configuration | `app/src/main/java/com/goodusestudios/shell/ui/ShellConfig.kt` |
| Root navigation and access gate | `app/src/main/java/com/goodusestudios/shell/ui/ShellApp.kt` |
| Replaceable product boundary | `FeatureCanvas.kt` |
| Shell screens | `ShellScreens.kt` |
| Theme | `ShellTheme.kt` |
| Semantic icons | `ShellIconCatalog.kt` |
| Billing and verification | `data/BillingController.kt`, `data/AccessPolicy.kt` |
| Durable shell state | `data/ShellStateStore.kt` |
| Optional backup seam (disabled by default) | `data/BackupProvider.kt` |
| Store policy router and release profile | `docs/STORE_COMPLIANCE_CHECKLIST.md`, `docs/APP_POLICY_PROFILE.md` |
| UI regression coverage | `docs/UI_REGRESSION_MATRIX.md`, `PaywallComplianceUiTest.kt` |
| Contract migrations | `docs/SHELL_MIGRATIONS.md` |
| Ads and consent | `ui/AdBanner.kt`, `data/AdConsentController.kt` |
| Shared localization | `localization/GoodUseLocalization.kt`, `app/src/main/assets/gooduse-common-localization-v1.json` |
| Locked/custom boundary | `docs/LOCKED_SHELL_SPEC.md` |
| Branding assets | `branding/README.md` |
| Release checks | `scripts/validate-shell.sh` |

## What a derived app may replace

A derived app normally changes only:

1. Identity: package, application ID, display name, version and store identifiers.
2. Brand: complete launcher-icon family, in-app mark, colors and product-specific copy.
3. `ShellConfig.definition`: legal data, onboarding profile, destinations, monetization, products, ads, cap and support address.
4. Product feature code supplied as the `FeatureCanvas`.
5. Product-specific localization, Room database/repositories and Android capabilities that the product truly needs.
6. Reviewed privacy, terms and store metadata.

Everything else is shell infrastructure. Modify it only to fix a platform-wide defect that should benefit every future app.

## Required implementation order

1. Write the product flow and identify exactly which operations count as successful billable actions.
2. Select one monetization mode in `ShellConfig.kt`.
3. Decide whether the shipped artifact is `ads` or `noAds`.
4. Configure identity, legal version, HTTPS links, support, destinations and onboarding.
5. Replace the full brand/icon family.
6. Implement the feature canvas and its local data layer.
7. Call `reportSuccessfulAction(stableId)` only after the operation commits successfully.
8. Configure Play products and production advertising identifiers, when applicable.
9. Replace every template placeholder and finish localization.
10. Perform the release validation authorized for the task.

Do not redesign settled shell UI while implementing the product canvas.

## Feature-canvas contract

- Inject product UI through `ShellApp(featureCanvas = ...)`.
- The canvas receives destination, adaptive-width, sample-state and access context.
- A stable action ID represents one completed domain operation.
- Report success after persistence succeeds, never on button press, form opening, validation failure or retry.
- Reusing the same ID must be safe; the shell deduplicates it.
- When the shell requests the paywall, route through the existing shell paywall.
- Product code must not read or mutate entitlement storage directly.

## Monetization modes

| Mode | Access | Advertising | Required product |
|---|---|---|---|
| `Free` | Always | No | None |
| `Ads` | Always | Yes | None |
| `AdsWithRemovePurchase` | Always | Until verified unlock | One-time |
| `OneTimeUnlock` | Verified purchase only | No | One-time |
| `Subscription` | Active verified subscription only | No | Subscription |
| `UsageCapWithOneTimeUnlock` | Free successful actions, then verified unlock | No | One-time |
| `UsageCapWithSubscription` | Free successful actions, then active subscription | No | Subscription |

The current template default is `AdsWithRemovePurchase`. Change it deliberately for each app.

### Lower ad banner

The anchored adaptive banner is already implemented. It renders above the bottom navigation, never over content, only when all conditions are true:

- the selected monetization mode uses ads;
- the `ads` product flavor is built;
- UMP says ads may be requested; and
- no verified remove-ads entitlement is active.

Use `assembleAdsRelease` only for an ads-capable app. Use `assembleNoAdsRelease` for a physically ad-free artifact; that flavor removes the advertising-ID manifest declaration and does not initialize UMP or AdMob. Never place production ad IDs in the reusable template.

## Revenue integrity

- Access is resolved centrally before the product canvas is composed.
- Product IDs and product kinds must match Play Console.
- The Play licensing public key must be configured, or a trusted `PurchaseVerifier` must be injected.
- Blank or failed verification is fail-closed.
- Pending purchases do not grant access.
- Billing connect, product query, purchase and restore operations are single-flight. Keep at most one queued ready action and disable incompatible controls while billing work is active.
- An empty product-details response is an unavailable-catalog result, not success. Show an explicit localized error and one full-size retry control; another empty result must remain visible instead of leaving an inert button or endless spinner.
- A successful authoritative refresh replaces cached entitlements, so refunds and revocations close access.
- Subscription offline grace is bounded to 0–168 hours.
- Usage-cap records are durable, bounded and deduplicated.
- Debug entitlement controls must remain debug-only.

## Onboarding and legal

The shell supports `None`, `SinglePage` and `Pager`. Legal acceptance remains explicit and versioned. When `legal.version` increases, existing users must re-consent. Privacy and terms must remain readable before acceptance. Do not add account creation unless the derived app requires it.

## Navigation and adaptive layout

- Configure one to five top-level destinations.
- One primary task should usually use one destination and no bottom navigation.
- Compact windows use the bottom navigation bar.
- Widths from 600 dp use a rail; widths from 840 dp use the sidebar treatment.
- Do not branch on phone or tablet model names.
- Preserve the scrollable onboarding fallback for short screens and large font scales.

## Interaction reliability and touchscreen gate

- Treat every visible control as one full-surface target of at least 48×48 dp. A row that looks tappable must own its complete click or selection semantics.
- Build composite rows with one parent click action. Nested icons, labels, checkboxes and `RadioButton` controls use `onClick = null` when the parent owns selection so taps are not split into competing regions.
- Give the parent semantics node the role, label, test tag and enabled state. Compose UI tests must act through that node—not a child text or icon that can hide a partial target.
- Purchase, restore, retry, save, destructive confirmation and navigation actions are single-flight. Repeated taps must not queue duplicate mutations, billing calls or navigation events.
- A label appearing is not evidence that its control works. Regression tests must cover edge taps, minimum bounds, exactly-once callbacks, disabled/loading states, rapid repeated taps, compact height, adaptive widths, TalkBack, RTL and large fonts.
- Keyboard, dialogs, sheets, scrolling containers and edge-to-edge overlays must not intercept enabled controls. Diagnose gesture competition and state ownership before changing settled layout.

## Localization and accessibility

- Keep the shared 31-language common-string contract intact.
- Put only product-specific strings in the derived app layer.
- Localize all user-visible strings and content descriptions.
- Actionable icons require localized descriptions; decorative icons use null.
- Verify long text, RTL, large fonts and compact height without clipping.
- Avoid hard-coded English inside product Composables.
- Changing the in-app language must immediately update every visible header, navigation label and Settings row. Resolve display text from the active locale state; do not cache English titles outside recomposition.
- Display locale-appropriate currency symbols in customer-facing fields while persisting ISO 4217 codes as the data identity. Do not silently combine or convert currencies.
- Currency selectors contain current circulating regional currencies only. Exclude withdrawn codes, funds/accounting units, precious-metal codes, testing codes and other non-consumer entries; document and test the allow/exclude policy.

## Release blockers

A derived release must not ship with template identity, example URLs, support email, demo product IDs, Google test ad IDs or placeholder legal text. Ad-enabled releases also require reviewed UMP messages, privacy disclosures and production AdMob configuration.

When execution is authorized, validate the exact artifact:

- Ads-capable: `bash scripts/validate-shell.sh --strict ads`
- Ad-free: `bash scripts/validate-shell.sh --strict noAds`

- Validation must run on a stock hosted runner. Optional tools such as `rg` require a built-in fallback, and their absence must never silently skip or weaken a gate.

Do not start GitHub Actions as a substitute for an unrequested local run.


## Store review evidence and subscription metadata

- Attach the latest production build to the store release; never submit a screenshot/demo fixture as production.
- Reconcile product ID, base-plan duration, base price, geographic prices, localized metadata, free limit, paid entitlement, restore path, lapse behavior and legal links across code, Play Console and reviewer instructions before submission.
- Keep private reviewer evidence and instructions separate from public store graphics. Public promotional media must be purpose-built and must not reuse the launcher icon as an ordinary promotional asset.
- Reviewer instructions identify the exact build and product, give a reproducible paywall path, state the free and paid behavior, and disclose material account, data, advertising and physical-goods boundaries.
- Preparing a release or subscription is not submission. Do not send it to review or production without explicit authorization.

## Compliance baseline added in contract 2.1.0

- First launch defaults to legal-only acceptance. Product tours are an explicit per-app choice.
- Purchase surfaces are text-and-controls only: no app logo, launcher icon, or brand imagery.
- Backup is a native interface with `DisabledBackupProvider` as the default; supplying a cloud provider requires new privacy, account, retention, deletion, and Data safety decisions.
- The policy profile is intentionally incomplete in the template. Replace every `DECISION_REQUIRED` before a release candidate.
