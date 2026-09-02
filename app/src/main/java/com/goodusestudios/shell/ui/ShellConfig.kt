package com.goodusestudios.shell.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The customization surface for every derived app. Shell mechanics read only this
 * definition; product code should not fork onboarding, legal, billing, or navigation UI.
 */
object ShellConfig {
    val definition = ShellDefinition(
        brand = BrandConfig(appName = "Shell", supportEmail = "support@example.com"),
        legal = LegalConfig(
            version = 1,
            effectiveDate = "2026-09-02",
            privacyUrl = "https://example.com/privacy",
            termsUrl = "https://example.com/terms",
            privacyBody = "This shell does not collect personal data. Replace this placeholder with the derived app’s reviewed privacy policy, data inventory, retention rules, advertising disclosures, and contact details before distribution.",
            termsBody = "This shell is a reusable development template. Replace these terms with the derived app’s reviewed terms, purchase conditions, subscription renewal language, and jurisdiction-specific clauses before distribution.",
        ),
        onboarding = OnboardingConfig(
            presentation = OnboardingPresentation.Pager,
            showBrandMark = true,
            requireLegalAcceptance = true,
            pages = listOf(
                OnboardingPage("01", "A faster starting point", "Adaptive navigation, settings, legal, monetization, and polished states are ready."),
                OnboardingPage("02", "Native on every screen", "Compact phones use bottom navigation. Larger windows gain a navigation rail and more useful space."),
                OnboardingPage("03", "Skin it, then build", "Change configuration and brand assets first. Replace only the product feature area."),
            ),
        ),
        monetization = MonetizationConfig(
            initialMode = MonetizationMode.AdsWithRemovePurchase,
            freeSuccessfulActions = 5,
            subscriptionOfflineGraceHours = 72,
            // Base64 Play Console licensing public key. Empty is deliberately fail-closed.
            playLicensePublicKey = "",
            products = listOf(
                PurchaseProduct("shell.pro.monthly", StoreProductKind.Subscription, "Shell Pro monthly", "Configured in Play Console"),
                PurchaseProduct("shell.pro.lifetime", StoreProductKind.OneTime, "Lifetime unlock", "Configured in Play Console"),
            ),
            benefits = listOf("Unlimited core actions", "No advertising", "Supports continued development"),
        ),
        ads = AdsConfig(
            applicationId = "ca-app-pub-3940256099942544~3347511713",
            bannerUnitId = "ca-app-pub-3940256099942544/9214589741",
        ),
        destinations = listOf(
            ShellDestination("home", "Home", Icons.Outlined.Home),
            ShellDestination("library", "Library", Icons.Outlined.Inbox),
            ShellDestination("activity", "Activity", Icons.Outlined.Timeline),
        ),
    )

    val appName get() = definition.brand.appName
    val supportEmail get() = definition.brand.supportEmail
    val privacyUrl get() = definition.legal.privacyUrl
    val termsUrl get() = definition.legal.termsUrl
    val demoBannerUnitId get() = definition.ads.bannerUnitId
    val destinations get() = definition.destinations

    fun validationErrors(): List<String> = buildList {
        if (definition.brand.appName.isBlank()) add("brand.appName must not be blank")
        if (!definition.brand.supportEmail.contains('@')) add("brand.supportEmail must be an email address")
        if (definition.legal.version < 1) add("legal.version must be positive")
        if (definition.onboarding.presentation != OnboardingPresentation.None && definition.onboarding.pages.isEmpty()) {
            add("enabled onboarding requires at least one page")
        }
        if (definition.destinations.map { it.id }.distinct().size != definition.destinations.size) add("destination IDs must be unique")
        if (definition.destinations.size !in 1..5) add("configure one to five top-level destinations")
        val monetization = definition.monetization
        if (monetization.products.map { it.id }.distinct().size != monetization.products.size) add("Play product IDs must be unique")
        if (monetization.products.any { it.id.isBlank() }) add("Play product IDs must not be blank")
        if (monetization.initialMode.usesUsageCap && monetization.freeSuccessfulActions !in 1..1000) {
            add("usage-cap modes require 1 to 1000 free successful actions")
        }
        if (monetization.subscriptionOfflineGraceHours !in 0..168) {
            add("subscription offline grace must be between 0 and 168 hours")
        }
        val requiredKind = monetization.initialMode.requiredProductKind
        if (requiredKind != null && monetization.products.none { it.kind == requiredKind }) {
            add("${monetization.initialMode} requires a $requiredKind Play product")
        }
    }
}

data class ShellDefinition(
    val brand: BrandConfig,
    val legal: LegalConfig,
    val onboarding: OnboardingConfig,
    val monetization: MonetizationConfig,
    val ads: AdsConfig,
    val destinations: List<ShellDestination>,
)

data class BrandConfig(val appName: String, val supportEmail: String)
data class LegalConfig(
    val version: Int,
    val effectiveDate: String,
    val privacyUrl: String,
    val termsUrl: String,
    val privacyBody: String,
    val termsBody: String,
)

enum class OnboardingPresentation { None, SinglePage, Pager }
data class OnboardingConfig(
    val presentation: OnboardingPresentation,
    val showBrandMark: Boolean,
    val requireLegalAcceptance: Boolean,
    val pages: List<OnboardingPage>,
)
data class OnboardingPage(val step: String, val title: String, val body: String)

data class MonetizationConfig(
    val initialMode: MonetizationMode,
    val freeSuccessfulActions: Int,
    val subscriptionOfflineGraceHours: Int,
    val playLicensePublicKey: String,
    val products: List<PurchaseProduct>,
    val benefits: List<String>,
)
data class PurchaseProduct(
    val id: String,
    val kind: StoreProductKind,
    val fallbackTitle: String,
    val fallbackPrice: String,
)
enum class StoreProductKind { OneTime, Subscription }
data class AdsConfig(
    val applicationId: String,
    val bannerUnitId: String,
    val tagForUnderAgeOfConsent: Boolean = false,
)
data class ShellDestination(val id: String, val label: String, val icon: ImageVector)

enum class MonetizationMode {
    Free, Ads, AdsWithRemovePurchase, OneTimeUnlock, Subscription,
    UsageCapWithOneTimeUnlock, UsageCapWithSubscription,
}

val MonetizationMode.usesAds: Boolean
    get() = this == MonetizationMode.Ads || this == MonetizationMode.AdsWithRemovePurchase

val MonetizationMode.usesUsageCap: Boolean
    get() = this == MonetizationMode.UsageCapWithOneTimeUnlock ||
        this == MonetizationMode.UsageCapWithSubscription

val MonetizationMode.requiredProductKind: StoreProductKind?
    get() = when (this) {
        MonetizationMode.AdsWithRemovePurchase,
        MonetizationMode.OneTimeUnlock,
        MonetizationMode.UsageCapWithOneTimeUnlock -> StoreProductKind.OneTime
        MonetizationMode.Subscription,
        MonetizationMode.UsageCapWithSubscription -> StoreProductKind.Subscription
        MonetizationMode.Free,
        MonetizationMode.Ads -> null
    }
enum class SampleContentState { Populated, Empty, Loading, Error }
enum class NavigationMode { BottomBar, Rail, Sidebar }
enum class OnboardingLayoutMode { Anchored, Scrollable }

fun navigationModeForWidth(widthDp: Int): NavigationMode = when {
    widthDp >= 840 -> NavigationMode.Sidebar
    widthDp >= 600 -> NavigationMode.Rail
    else -> NavigationMode.BottomBar
}

/** Persistent actions need extra height, especially after font scaling. */
fun onboardingLayoutModeFor(heightDp: Int, fontScale: Float): OnboardingLayoutMode =
    if (heightDp < 700 || fontScale > 1.3f) OnboardingLayoutMode.Scrollable else OnboardingLayoutMode.Anchored
