package com.goodusestudios.shell.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class Route { Main, Settings, Lab, Paywall, Privacy, Terms }

@Composable
fun ShellApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("shell", 0) }
    var onboardingComplete by remember { mutableStateOf(preferences.getBoolean("onboarding", false)) }
    var route by remember { mutableStateOf(Route.Main) }
    var destinationId by remember { mutableStateOf(ShellConfig.destinations.first().id) }
    var monetizationMode by remember { mutableStateOf(MonetizationMode.AdsWithRemovePurchase) }
    var adRemoved by remember { mutableStateOf(false) }
    var contentState by remember { mutableStateOf(SampleContentState.Populated) }

    if (!onboardingComplete) {
        OnboardingScreen(
            onPrivacy = { route = Route.Privacy },
            onTerms = { route = Route.Terms },
            onComplete = {
                preferences.edit().putBoolean("onboarding", true).apply()
                onboardingComplete = true
            },
        )
        if (route == Route.Privacy || route == Route.Terms) {
            LegalDialog(
                title = if (route == Route.Privacy) "Privacy Policy" else "Terms of Use",
                body = if (route == Route.Privacy) privacyPlaceholder else termsPlaceholder,
                onDismiss = { route = Route.Main },
            )
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp
        val showAd = monetizationMode in setOf(
            MonetizationMode.Ads,
            MonetizationMode.AdsWithRemovePurchase,
        ) && !adRemoved

        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                ShellRail(destinationId, onDestination = { destinationId = it; route = Route.Main })
                ShellScaffold(
                    route = route,
                    destinationId = destinationId,
                    expanded = true,
                    showBottomBar = false,
                    showAd = showAd,
                    monetizationMode = monetizationMode,
                    contentState = contentState,
                    onNavigate = { route = it },
                    onDestination = { destinationId = it; route = Route.Main },
                    onMonetizationMode = { monetizationMode = it },
                    onContentState = { contentState = it },
                    onRemoveAds = { adRemoved = !adRemoved },
                    onResetOnboarding = {
                        preferences.edit().putBoolean("onboarding", false).apply()
                        onboardingComplete = false
                    },
                )
            }
        } else {
            ShellScaffold(
                route = route,
                destinationId = destinationId,
                expanded = false,
                showBottomBar = route == Route.Main,
                showAd = showAd,
                monetizationMode = monetizationMode,
                contentState = contentState,
                onNavigate = { route = it },
                onDestination = { destinationId = it; route = Route.Main },
                onMonetizationMode = { monetizationMode = it },
                onContentState = { contentState = it },
                onRemoveAds = { adRemoved = !adRemoved },
                onResetOnboarding = {
                    preferences.edit().putBoolean("onboarding", false).apply()
                    onboardingComplete = false
                },
            )
        }
    }
}

@Composable
private fun ShellRail(selected: String, onDestination: (String) -> Unit) {
    NavigationRail {
        ShellConfig.destinations.forEach { item ->
            NavigationRailItem(
                selected = selected == item.id,
                onClick = { onDestination(item.id) },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellScaffold(
    route: Route,
    destinationId: String,
    expanded: Boolean,
    showBottomBar: Boolean,
    showAd: Boolean,
    monetizationMode: MonetizationMode,
    contentState: SampleContentState,
    onNavigate: (Route) -> Unit,
    onDestination: (String) -> Unit,
    onMonetizationMode: (MonetizationMode) -> Unit,
    onContentState: (SampleContentState) -> Unit,
    onRemoveAds: () -> Unit,
    onResetOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    val title = when (route) {
        Route.Main -> ShellConfig.destinations.first { it.id == destinationId }.label
        Route.Settings -> "Settings"
        Route.Lab -> "Shell Lab"
        Route.Paywall -> "Upgrade"
        Route.Privacy -> "Privacy Policy"
        Route.Terms -> "Terms of Use"
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (route != Route.Main) {
                        IconButton(onClick = { onNavigate(Route.Main) }) { Text("‹") }
                    }
                },
                actions = {
                    if (route == Route.Main) {
                        IconButton(onClick = { onNavigate(Route.Settings) }) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column {
                if (showAd && route == Route.Main) AdaptiveAdBanner()
                if (showBottomBar) {
                    NavigationBar {
                        ShellConfig.destinations.forEach { item ->
                            NavigationBarItem(
                                selected = destinationId == item.id,
                                onClick = { onDestination(item.id) },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (route) {
                Route.Main -> FeatureScreen(destinationId, contentState, expanded)
                Route.Settings -> SettingsScreen(
                    monetizationMode = monetizationMode,
                    onUpgrade = { onNavigate(Route.Paywall) },
                    onLab = { onNavigate(Route.Lab) },
                    onPrivacy = { onNavigate(Route.Privacy) },
                    onTerms = { onNavigate(Route.Terms) },
                    onSupport = {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${ShellConfig.supportEmail}")))
                    },
                )
                Route.Lab -> LabScreen(
                    monetizationMode,
                    contentState,
                    onMonetizationMode,
                    onContentState,
                    onRemoveAds,
                    onResetOnboarding,
                )
                Route.Paywall -> PaywallScreen(onRestore = {}, onPurchase = {})
                Route.Privacy -> LegalScreen("Privacy Policy", privacyPlaceholder)
                Route.Terms -> LegalScreen("Terms of Use", termsPlaceholder)
            }
        }
    }
}
