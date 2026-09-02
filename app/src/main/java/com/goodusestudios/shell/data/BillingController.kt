package com.goodusestudios.shell.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.goodusestudios.shell.ui.PurchaseProduct
import com.goodusestudios.shell.ui.StoreProductKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BillingProduct(
    val id: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val kind: StoreProductKind,
    val available: Boolean,
)

enum class BillingStatus { Connecting, Ready, Unavailable }
data class BillingUiState(
    val status: BillingStatus = BillingStatus.Connecting,
    val products: List<BillingProduct> = emptyList(),
    val entitled: Boolean = false,
    val working: Boolean = false,
    val message: String? = null,
)

/** Replace this with trusted-server verification before production release. */
fun interface PurchaseVerifier { fun verify(purchase: Purchase): Boolean }

class BillingController(
    context: Context,
    private val configuredProducts: List<PurchaseProduct>,
    private val verifier: PurchaseVerifier = PurchaseVerifier { true },
) : PurchasesUpdatedListener {
    private val _state = MutableStateFlow(BillingUiState(products = fallbackProducts()))
    val state: StateFlow<BillingUiState> = _state.asStateFlow()
    private val detailsById = mutableMapOf<String, ProductDetails>()
    private val readyActions = mutableListOf<() -> Unit>()
    private var connecting = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billingClient.isReady) {
            refreshProducts()
            return
        }
        if (connecting) return
        connecting = true
        _state.value = _state.value.copy(status = BillingStatus.Connecting, message = null)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(status = BillingStatus.Ready)
                    val actions = readyActions.toList()
                    readyActions.clear()
                    refreshProducts()
                    queryOwnedPurchases(showRestoreMessage = false)
                    actions.forEach { it() }
                } else {
                    readyActions.clear()
                    _state.value = _state.value.copy(
                        status = BillingStatus.Unavailable,
                        message = result.debugMessage.ifBlank { "Google Play purchases are unavailable." },
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _state.value = _state.value.copy(status = BillingStatus.Connecting)
            }
        })
    }

    fun restore() = whenReady { queryOwnedPurchases(showRestoreMessage = true) }

    fun launchPurchase(activity: Activity, productId: String) = whenReady {
        val details = detailsById[productId]
        if (details == null) {
            _state.value = _state.value.copy(message = "This product is not active for the installed Play build.")
            return@whenReady
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (details.productType == BillingClient.ProductType.SUBS) {
            val token = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (token == null) {
                _state.value = _state.value.copy(message = "No eligible subscription offer is available.")
                return@whenReady
            }
            productParams.setOfferToken(token)
        }
        _state.value = _state.value.copy(working = true, message = null)
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams.build())).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(working = false, message = result.debugMessage)
        }
    }

    private fun whenReady(action: () -> Unit) {
        if (billingClient.isReady) action() else {
            readyActions += action
            connect()
        }
    }

    private fun refreshProducts() {
        configuredProducts.groupBy { it.kind }.forEach { (kind, products) ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.id)
                        .setProductType(kind.playType())
                        .build()
                })
                .build()
            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList.forEach { detailsById[it.productId] = it }
                    _state.value = _state.value.copy(products = configuredProducts.map(::toBillingProduct), message = null)
                } else {
                    _state.value = _state.value.copy(message = result.debugMessage)
                }
            }
        }
    }

    private fun queryOwnedPurchases(showRestoreMessage: Boolean) {
        _state.value = _state.value.copy(working = true, message = null)
        val types = configuredProducts.map { it.kind }.distinct()
        if (types.isEmpty()) {
            _state.value = _state.value.copy(working = false, message = "No products are configured.")
            return
        }
        val owned = mutableListOf<Purchase>()
        var remaining = types.size
        types.forEach { kind ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(kind.playType()).build(),
            ) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) owned += purchases
                remaining--
                if (remaining == 0) {
                    processPurchases(owned)
                    if (showRestoreMessage && owned.none { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                        _state.value = _state.value.copy(message = "No active purchases were found.")
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(working = false)
            else -> _state.value = _state.value.copy(working = false, message = result.debugMessage)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val verified = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && verifier.verify(it)
        }
        _state.value = _state.value.copy(entitled = verified.isNotEmpty(), working = false)
        verified.filterNot { it.isAcknowledged }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(message = result.debugMessage)
                }
            }
        }
    }

    private fun toBillingProduct(config: PurchaseProduct): BillingProduct {
        val details = detailsById[config.id]
        val price = when (config.kind) {
            StoreProductKind.OneTime -> details?.oneTimePurchaseOfferDetails?.formattedPrice
            StoreProductKind.Subscription -> details?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        }
        return BillingProduct(
            id = config.id,
            title = details?.title ?: config.fallbackTitle,
            description = details?.description.orEmpty(),
            formattedPrice = price ?: config.fallbackPrice,
            kind = config.kind,
            available = details != null,
        )
    }

    private fun fallbackProducts() = configuredProducts.map(::toBillingProduct)
    private fun StoreProductKind.playType() =
        if (this == StoreProductKind.Subscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP

    fun close() = billingClient.endConnection()
}
