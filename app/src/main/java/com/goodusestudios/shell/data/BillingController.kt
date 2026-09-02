package com.goodusestudios.shell.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Thin Play Billing 9 seam. Replace product IDs in ShellConfig, then add product querying/purchase UI. */
class BillingController(context: Context) : PurchasesUpdatedListener {
    private val _entitled = MutableStateFlow(false)
    val entitled: StateFlow<Boolean> = _entitled

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) = Unit
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun restore() {
        // Query purchases and verify them on a trusted server in a production app.
        connect()
    }

    fun launchPurchase(activity: Activity) {
        // Build BillingFlowParams after querying ProductDetails for the configured product.
        // This deliberate seam prevents a shell/demo SKU from ever charging a customer.
        connect()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _entitled.value = purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    }
}
