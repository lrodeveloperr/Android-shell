package com.goodusestudios.shell.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onPrivacy: () -> Unit, onTerms: () -> Unit, onComplete: () -> Unit) {
    val pages = listOf(
        Triple("A faster starting point", "Adaptive navigation, settings, legal, monetization seams, and polished states are ready.", "01"),
        Triple("Native on every screen", "Compact phones use bottom navigation. Larger windows gain a navigation rail and more useful space.", "02"),
        Triple("Skin it, then build", "Change tokens and configuration first. Replace placeholder features only after your product logic is settled.", "03"),
    )
    var page by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val layoutMode = onboardingLayoutModeFor(maxHeight.value.toInt(), LocalDensity.current.fontScale)
        val contentModifier = Modifier
            .align(Alignment.Center)
            .fillMaxHeight()
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp)

        if (layoutMode == OnboardingLayoutMode.Scrollable) {
            Column(contentModifier.verticalScroll(rememberScrollState())) {
                OnboardingHeader()
                Spacer(Modifier.height(24.dp))
                OnboardingPage(pages[page])
                Spacer(Modifier.height(32.dp))
                OnboardingActions(page, pages.lastIndex, onPrivacy, onTerms, { page-- }) {
                    if (page < pages.lastIndex) page++ else onComplete()
                }
            }
        } else {
            Column(contentModifier) {
                OnboardingHeader()
                Spacer(Modifier.height(if (maxHeight >= 900.dp) 56.dp else 40.dp))
                OnboardingPage(pages[page])
                Spacer(Modifier.weight(1f))
                OnboardingActions(page, pages.lastIndex, onPrivacy, onTerms, { page-- }) {
                    if (page < pages.lastIndex) page++ else onComplete()
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader() {
    Text(ShellConfig.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun OnboardingPage(page: Triple<String, String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(page.third, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(page.first, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(page.second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingActions(
    page: Int,
    lastPage: Int,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (page == lastPage) "Get started" else "Continue") }
        if (page > 0) {
            OutlinedButton(onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onPrivacy) { Text("Privacy") }
            TextButton(onClick = onTerms) { Text("Terms") }
        }
    }
}

@Composable
fun FeatureScreen(destination: String, state: SampleContentState, expanded: Boolean) {
    when (state) {
        SampleContentState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        SampleContentState.Empty -> StateMessage("Nothing here yet", "Your app’s primary empty state belongs here.")
        SampleContentState.Error -> StateMessage("Couldn’t load content", "Keep the explanation human and offer one clear recovery action.", action = "Try again")
        SampleContentState.Populated -> {
            val items = (1..8).map { "${destination.replaceFirstChar(Char::uppercase)} item $it" }
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    FeatureList(items, Modifier.weight(0.42f))
                    Surface(Modifier.weight(0.58f).fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        StateMessage("Select an item", "A list-detail layout uses tablet space without merely stretching the phone UI.")
                    }
                }
            } else FeatureList(items, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun FeatureList(items: List<String>, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 8.dp)) {
        item {
            Column(
                Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("This is the replaceable feature area. Shell chrome stays untouched.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(items) { item ->
            ListItem(
                headlineContent = { Text(item) },
                supportingContent = { Text("Useful supporting information") },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun StateMessage(title: String, message: String, action: String? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = {}) { Text(action) }
        }
    }
}

@Composable
fun SettingsScreen(
    monetizationMode: MonetizationMode,
    onUpgrade: () -> Unit,
    onIcons: () -> Unit,
    onLab: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onSupport: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        if (monetizationMode != MonetizationMode.Free) {
            item { SettingsRow("Upgrade", "Manage purchases and restore access", Icons.Outlined.LockOpen, onUpgrade) }
        }
        item { SettingsRow("Icon library", "Search reusable Material icons", Icons.Outlined.Apps, onIcons) }
        item { SettingsRow("Language", "Follow system · English", Icons.Outlined.Language, {}) }
        item { SettingsRow("Help & support", ShellConfig.supportEmail, Icons.Outlined.SupportAgent, onSupport) }
        item { SettingsRow("Privacy policy", null, Icons.Outlined.Policy, onPrivacy) }
        item { SettingsRow("Terms of use", null, Icons.Outlined.CheckCircle, onTerms) }
        item { SettingsRow("Shell Lab", "Exercise every reusable state", Icons.Outlined.Science, onLab) }
        item {
            Text(
                "Shell 1.0 · Replace example URLs, product IDs, and ad IDs before shipping.",
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
fun LabScreen(
    mode: MonetizationMode,
    state: SampleContentState,
    onMode: (MonetizationMode) -> Unit,
    onState: (SampleContentState) -> Unit,
    onRemoveAds: () -> Unit,
    onResetOnboarding: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Monetization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MonetizationMode.values().forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == value, onClick = { onMode(value) })
                    Text(value.readableName())
                }
            }
        }
        item {
            Text("Feature state", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SampleContentState.values().forEach { value ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state == value, onClick = { onState(value) })
                    Text(value.name)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRemoveAds) { Text("Toggle entitlement") }
                OutlinedButton(onClick = onResetOnboarding) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset onboarding")
                }
            }
        }
    }
}

@Composable
fun PaywallScreen(onRestore: () -> Unit, onPurchase: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Make the useful thing unlimited.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("One focused value statement, transparent pricing, restore access, and legal links—without dark patterns.")
        }
        items(listOf("Unlimited core actions", "No advertising", "Supports continued development")) { value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(value)
            }
        }
        item { Button(onClick = onPurchase, modifier = Modifier.fillMaxWidth()) { Text("Continue · configured price") } }
        item { TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Restore purchases") } }
    }
}

@Composable
fun LegalScreen(title: String, body: String) {
    LazyColumn(contentPadding = PaddingValues(24.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Spacer(Modifier.height(16.dp)) }
        item { Text(body, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
fun LegalDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(title) },
        text = { Text(body) },
    )
}

const val privacyPlaceholder = "This shell does not collect personal data. Replace this placeholder with the derived app’s reviewed privacy policy, data inventory, retention rules, advertising disclosures, and contact details before distribution."
const val termsPlaceholder = "This shell is a reusable development template. Replace these terms with the derived app’s reviewed terms, purchase conditions, subscription renewal language, and jurisdiction-specific clauses before distribution."

private fun MonetizationMode.readableName() = when (this) {
    MonetizationMode.Free -> "Free"
    MonetizationMode.Ads -> "Ads"
    MonetizationMode.AdsWithRemovePurchase -> "Ads + remove purchase"
    MonetizationMode.OneTimeUnlock -> "One-time unlock"
    MonetizationMode.Subscription -> "Subscription"
    MonetizationMode.UsageCapWithOneTimeUnlock -> "Usage cap + one-time unlock"
    MonetizationMode.UsageCapWithSubscription -> "Usage cap + subscription"
}
