package com.goodusestudios.shell.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The only file most derived apps need to edit before replacing placeholder features.
 */
object ShellConfig {
    const val appName = "Shell"
    const val supportEmail = "support@example.com"
    const val privacyUrl = "https://example.com/privacy"
    const val termsUrl = "https://example.com/terms"
    const val subscriptionProductId = "shell.pro.monthly"
    const val lifetimeProductId = "shell.pro.lifetime"
    const val demoBannerUnitId = "ca-app-pub-3940256099942544/9214589741"

    val destinations = listOf(
        ShellDestination("home", "Home", Icons.Outlined.Home),
        ShellDestination("library", "Library", Icons.Outlined.Inbox),
        ShellDestination("activity", "Activity", Icons.Outlined.Timeline),
    )
}

data class ShellDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

enum class MonetizationMode {
    Free,
    Ads,
    AdsWithRemovePurchase,
    OneTimeUnlock,
    Subscription,
    UsageCapWithOneTimeUnlock,
    UsageCapWithSubscription,
}

enum class SampleContentState { Populated, Empty, Loading, Error }

enum class NavigationMode { BottomBar, Rail, Sidebar }

enum class OnboardingLayoutMode { Anchored, Scrollable }

fun navigationModeForWidth(widthDp: Int): NavigationMode = when {
    widthDp >= 840 -> NavigationMode.Sidebar
    widthDp >= 600 -> NavigationMode.Rail
    else -> NavigationMode.BottomBar
}

/**
 * Content-specific breakpoint. The persistent onboarding actions need more
 * vertical room than a normal scrolling screen, especially with large text.
 */
fun onboardingLayoutModeFor(heightDp: Int, fontScale: Float): OnboardingLayoutMode =
    if (heightDp < 700 || fontScale > 1.3f) OnboardingLayoutMode.Scrollable
    else OnboardingLayoutMode.Anchored
