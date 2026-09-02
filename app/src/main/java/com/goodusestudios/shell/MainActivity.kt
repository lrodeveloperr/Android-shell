package com.goodusestudios.shell

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodusestudios.shell.data.AdConsentController
import com.goodusestudios.shell.ui.ShellApp
import com.goodusestudios.shell.ui.ShellConfig
import com.goodusestudios.shell.ui.ShellTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val adConsent = AdConsentController(this, ShellConfig.definition.ads.tagForUnderAgeOfConsent)
        setContent {
            val consentState = adConsent.state.collectAsStateWithLifecycle().value
            ShellTheme {
                ShellApp(
                    canRequestAds = consentState.canRequestAds,
                    privacyOptionsRequired = consentState.privacyOptionsRequired,
                    onPrivacyOptions = adConsent::showPrivacyOptions,
                )
            }
        }
        adConsent.gather()
    }
}
