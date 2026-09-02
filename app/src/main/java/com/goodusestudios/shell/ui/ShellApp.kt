package com.goodusestudios.shell.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodusestudios.shell.data.BillingController
import com.goodusestudios.shell.data.BillingUiState
import com.goodusestudios.shell.data.ShellGate
import com.goodusestudios.shell.data.ShellStateStore
import kotlinx.coroutines.launch

private enum class Route { Main, Settings, Icons, Lab, Paywall, Privacy, Terms }

@Composable
fun ShellApp(
    canRequestAds: Boolean,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val definition = ShellConfig.definition
    val stateStore = remember { ShellStateStore(context.applicationContext) }
    val billingController = remember { BillingController(context, definition.monetization.products) }
    val billing by billingController.state.collectAsStateWithLifecycle()
    var gate by remember { mutableStateOf<ShellGate?>(null) }
    var route by rememberSaveable { mutableStateOf(Route.Main) }
    var onboardingDialog by rememberSaveable { mutableStateOf<Route?>(null) }
    var destinationId by rememberSaveable { mutableStateOf(ShellConfig.destinations.first().id) }
    var monetizationMode by rememberSaveable { mutableStateOf(definition.monetization.initialMode) }
    var demoEntitlement by rememberSaveable { mutableStateOf(false) }
    var contentState by rememberSaveable { mutableStateOf(SampleContentState.Populated) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(billingController) { billingController.connect() }
    LaunchedEffect(stateStore, definition.legal.version) {
        stateStore.gate(definition.legal.version).collect { gate = it }
    }
    DisposableEffect(billingController) { onDispose { billingController.close() } }

    when (gate) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ShellGate.FullOnboarding -> {
            if (
                definition.onboarding.presentation == OnboardingPresentation.None &&
                definition.onboarding.requireLegalAcceptance
            ) {
                LegalUpdateScreen(
                    legal = definition.legal,
                    onPrivacy = { onboardingDialog = Route.Privacy },
                    onTerms = { onboardingDialog = Route.Terms },
                    onAccept = { scope.launch { stateStore.completeOnboarding(definition.legal.version) } },
                )
                OnboardingLegalDialog(onboardingDialog) { onboardingDialog = null }
            } else if (definition.onboarding.presentation == OnboardingPresentation.None) {
                LaunchedEffect(Unit) { stateStore.completeOnboarding(definition.legal.version) }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                OnboardingScreen(
                    config = definition.onboarding,
                    legal = definition.legal,
                    onPrivacy = { onboardingDialog = Route.Privacy },
                    onTerms = { onboardingDialog = Route.Terms },
                    onComplete = { scope.launch { stateStore.completeOnboarding(definition.legal.version) } },
                )
                OnboardingLegalDialog(onboardingDialog) { onboardingDialog = null }
            }
        }
        ShellGate.LegalUpdate -> {
            LegalUpdateScreen(
                legal = definition.legal,
                onPrivacy = { onboardingDialog = Route.Privacy },
                onTerms = { onboardingDialog = Route.Terms },
                onAccept = { scope.launch { stateStore.acceptLegalUpdate(definition.legal.version) } },
            )
            OnboardingLegalDialog(onboardingDialog) { onboardingDialog = null }
        }
        ShellGate.Ready -> {
            BackHandler(enabled = route != Route.Main) { route = Route.Main }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val expanded = maxWidth >= 600.dp
                val showAd = canRequestAds && monetizationMode in setOf(
                    MonetizationMode.Ads,
                    MonetizationMode.AdsWithRemovePurchase,
                ) && !demoEntitlement && !billing.entitled

                if (expanded) {
                    Row(Modifier.fillMaxSize()) {
                        ShellRail(destinationId) { destinationId = it; route = Route.Main }
                        ShellScaffold(
                            route, destinationId, true, false, showAd, monetizationMode, contentState,
                            privacyOptionsRequired, billing,
                            onNavigate = { route = it },
                            onDestination = { destinationId = it; route = Route.Main },
                            onMonetizationMode = { monetizationMode = it },
                            onContentState = { contentState = it },
                            onRemoveAds = { demoEntitlement = !demoEntitlement },
                            onResetOnboarding = { scope.launch { stateStore.resetOnboarding() } },
                            onLanguage = { showLanguageDialog = true },
                            onPrivacyOptions = onPrivacyOptions,
                            billingController = billingController,
                        )
                    }
                } else {
                    ShellScaffold(
                        route, destinationId, false, route == Route.Main, showAd, monetizationMode, contentState,
                        privacyOptionsRequired, billing,
                        onNavigate = { route = it },
                        onDestination = { destinationId = it; route = Route.Main },
                        onMonetizationMode = { monetizationMode = it },
                        onContentState = { contentState = it },
                        onRemoveAds = { demoEntitlement = !demoEntitlement },
                        onResetOnboarding = { scope.launch { stateStore.resetOnboarding() } },
                        onLanguage = { showLanguageDialog = true },
                        onPrivacyOptions = onPrivacyOptions,
                        billingController = billingController,
                    )
                }
            }
            if (showLanguageDialog) LanguageDialog { showLanguageDialog = false }
        }
    }
}

@Composable
private fun OnboardingLegalDialog(route: Route?, onDismiss: () -> Unit) {
    if (route == Route.Privacy || route == Route.Terms) {
        val legal = ShellConfig.definition.legal
        LegalDialog(
            title = if (route == Route.Privacy) "Privacy Policy" else "Terms of Use",
            body = if (route == Route.Privacy) legal.privacyBody else legal.termsBody,
            onDismiss = onDismiss,
        )
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
    privacyOptionsRequired: Boolean,
    billing: BillingUiState,
    onNavigate: (Route) -> Unit,
    onDestination: (String) -> Unit,
    onMonetizationMode: (MonetizationMode) -> Unit,
    onContentState: (SampleContentState) -> Unit,
    onRemoveAds: () -> Unit,
    onResetOnboarding: () -> Unit,
    onLanguage: () -> Unit,
    onPrivacyOptions: () -> Unit,
    billingController: BillingController,
) {
    val context = LocalContext.current
    val legal = ShellConfig.definition.legal
    val title = when (route) {
        Route.Main -> ShellConfig.destinations.first { it.id == destinationId }.label
        Route.Settings -> "Settings"
        Route.Icons -> "Icon library"
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
                        IconButton(onClick = { onNavigate(Route.Main) }) {
                            Icon(Icons.Outlined.ArrowBack, "Back")
                        }
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
                Route.Main -> FeatureScreen(destinationId, contentState, expanded) {
                    onContentState(SampleContentState.Populated)
                }
                Route.Settings -> SettingsScreen(
                    monetizationMode = monetizationMode,
                    privacyOptionsRequired = privacyOptionsRequired,
                    onUpgrade = { onNavigate(Route.Paywall) },
                    onIcons = { onNavigate(Route.Icons) },
                    onLanguage = onLanguage,
                    onPrivacyOptions = onPrivacyOptions,
                    onLab = { onNavigate(Route.Lab) },
                    onPrivacy = { onNavigate(Route.Privacy) },
                    onTerms = { onNavigate(Route.Terms) },
                    onSupport = { openUri(context, "mailto:${ShellConfig.supportEmail}") },
                )
                Route.Icons -> IconLibraryScreen()
                Route.Lab -> LabScreen(monetizationMode, contentState, onMonetizationMode, onContentState, onRemoveAds, onResetOnboarding)
                Route.Paywall -> PaywallScreen(
                    billing = billing,
                    benefits = ShellConfig.definition.monetization.benefits,
                    onRetry = billingController::connect,
                    onRestore = billingController::restore,
                    onPurchase = { id -> context.findActivity()?.let { billingController.launchPurchase(it, id) } },
                    onPrivacy = { onNavigate(Route.Privacy) },
                    onTerms = { onNavigate(Route.Terms) },
                )
                Route.Privacy -> LegalScreen("Privacy Policy", legal.privacyBody, legal.effectiveDate) { openUri(context, legal.privacyUrl) }
                Route.Terms -> LegalScreen("Terms of Use", legal.termsBody, legal.effectiveDate) { openUri(context, legal.termsUrl) }
            }
        }
    }
}

private fun openUri(context: Context, uri: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
