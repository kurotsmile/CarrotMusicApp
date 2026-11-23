package com.carrot.heartbeatmusic;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Collections;
import java.util.List;

final class MonetizationManager {
    private static final String TAG = "HeartbeatAds";
    private static final String PREF_ADS_REMOVED = "ads_removed";
    private static final int SONG_SELECTIONS_PER_AD = 5;
    private static final long MIN_AD_INTERVAL_MS = 5L * 60L * 1000L;

    private final Activity activity;
    private final SharedPreferences preferences;
    private final ConsentInformation consentInformation;
    private BillingClient billingClient;
    private boolean adsRemoved;
    private boolean billingChecked;
    private boolean adsInitialized;
    private boolean destroyed;
    private int songSelections;
    private long lastAdTime = SystemClock.elapsedRealtime();

    MonetizationManager(Activity activity, SharedPreferences preferences) {
        this.activity = activity;
        this.preferences = preferences;
        adsRemoved = preferences.getBoolean(PREF_ADS_REMOVED, false);
        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
    }

    boolean isAvailable() {
        return true;
    }

    void start() {
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (destroyed || billingClient == null) {
                    return;
                }
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing unavailable; continuing with cached remove-ads state.");
                    billingChecked = true;
                    initializeAdsIfAllowed();
                    return;
                }
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                        (result, purchases) -> {
                            if (destroyed) {
                                return;
                            }
                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                updateEntitlement(purchases, false);
                            }
                            billingChecked = true;
                            Log.d(TAG, "Remove-ads purchase restore finished. owned=" + adsRemoved);
                            initializeAdsIfAllowed();
                        });
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });

        ConsentRequestParameters consentParams = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                activity,
                consentParams,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity,
                        formError -> {
                            Log.d(TAG, "Consent flow finished. canRequestAds=" + consentInformation.canRequestAds());
                            initializeAdsIfAllowed();
                        }),
                requestConsentError -> {
                    Log.d(TAG, "Consent update failed: " + requestConsentError.getMessage());
                    initializeAdsIfAllowed();
                });
    }

    void runAfterInterstitial(Runnable action) {
        songSelections++;
        if (adsRemoved
                || !adsInitialized
                || songSelections < SONG_SELECTIONS_PER_AD
                || SystemClock.elapsedRealtime() - lastAdTime < MIN_AD_INTERVAL_MS) {
            action.run();
            return;
        }

        InterstitialAd ad = InterstitialAdPreloader.pollAd(BuildConfig.INTERSTITIAL_AD_UNIT_ID);
        if (ad == null) {
            Log.d(TAG, "Interstitial not preloaded yet.");
            action.run();
            return;
        }
        ad.setAdEventCallback(new InterstitialAdEventCallback() {
            private boolean continued;

            private void continuePlayback() {
                if (!continued && !destroyed) {
                    continued = true;
                    action.run();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown.");
                songSelections = 0;
                lastAdTime = SystemClock.elapsedRealtime();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                continuePlayback();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                continuePlayback();
            }
        });
        ad.show(activity);
    }

    void runAfterRewarded(Runnable action) {
        if (adsRemoved) {
            action.run();
            return;
        }
        if (!adsInitialized) {
            Toast.makeText(activity, "Rewarded ad is not ready. Please try again shortly.", Toast.LENGTH_LONG).show();
            return;
        }
        RewardedAd ad = RewardedAdPreloader.pollAd(BuildConfig.REWARDED_AD_UNIT_ID);
        if (ad == null) {
            Toast.makeText(activity, "Rewarded ad is still loading. Please try again shortly.", Toast.LENGTH_LONG).show();
            return;
        }
        boolean[] rewardState = new boolean[3]; // earned, dismissed, continued
        Runnable continueAfterReward = () -> {
            if (rewardState[0] && rewardState[1] && !rewardState[2] && !destroyed) {
                rewardState[2] = true;
                action.run();
            }
        };
        ad.setAdEventCallback(new RewardedAdEventCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardState[1] = true;
                if (!rewardState[0]) {
                    Toast.makeText(activity, "Watch the full ad to export this MP3.", Toast.LENGTH_LONG).show();
                }
                continueAfterReward.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError error) {
                Toast.makeText(activity, "Rewarded ad could not be shown. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
        ad.show(activity, rewardItem -> {
            rewardState[0] = true;
            continueAfterReward.run();
        });
    }

    void purchaseRemoveAds() {
        if (adsRemoved) {
            Toast.makeText(activity, "Ads are already removed for this Google Play account.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (billingClient == null || !billingClient.isReady()) {
            Toast.makeText(activity, "Google Play billing is not ready. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BuildConfig.REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        billingClient.queryProductDetailsAsync(params, (result, detailsResult) -> {
            if (destroyed || billingClient == null) {
                return;
            }
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || detailsResult.getProductDetailsList().isEmpty()) {
                Toast.makeText(activity, "Remove ads is not available in Google Play yet.", Toast.LENGTH_LONG).show();
                return;
            }
            ProductDetails details = detailsResult.getProductDetailsList().get(0);
            List<ProductDetails.OneTimePurchaseOfferDetails> offers = details.getOneTimePurchaseOfferDetailsList();
            BillingFlowParams.ProductDetailsParams.Builder item = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details);
            if (offers != null && !offers.isEmpty()) {
                item.setOfferToken(offers.get(0).getOfferToken());
            }
            BillingResult launchResult = billingClient.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(Collections.singletonList(item.build()))
                            .build());
            if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Toast.makeText(activity, "Unable to open Google Play purchase.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (destroyed) {
            return;
        }
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            updateEntitlement(purchases, true);
        } else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(activity, "Google Play could not complete the purchase.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateEntitlement(List<Purchase> purchases, boolean notifyUser) {
        boolean ownsRemoveAds = false;
        boolean pending = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(BuildConfig.REMOVE_ADS_PRODUCT_ID)) {
                continue;
            }
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                ownsRemoveAds = true;
                if (!purchase.isAcknowledged()) {
                    billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build(),
                            billingResult -> {
                            });
                }
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                pending = true;
            }
        }

        adsRemoved = ownsRemoveAds;
        preferences.edit().putBoolean(PREF_ADS_REMOVED, adsRemoved).apply();
        if (adsRemoved && adsInitialized) {
            InterstitialAdPreloader.destroy(BuildConfig.INTERSTITIAL_AD_UNIT_ID);
            RewardedAdPreloader.destroy(BuildConfig.REWARDED_AD_UNIT_ID);
            adsInitialized = false;
        }
        if (notifyUser) {
            if (adsRemoved) {
                Toast.makeText(activity, "Purchase complete. Ads have been removed.", Toast.LENGTH_LONG).show();
            } else if (pending) {
                Toast.makeText(activity, "Purchase pending. Ads will be removed after payment completes.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeAdsIfAllowed() {
        if (destroyed || !billingChecked || adsRemoved || adsInitialized || !consentInformation.canRequestAds()) {
            Log.d(TAG, "Ads not initialized: billingChecked=" + billingChecked
                    + ", adsRemoved=" + adsRemoved
                    + ", canRequestAds=" + consentInformation.canRequestAds());
            return;
        }
        adsInitialized = true;
        new Thread(() -> MobileAds.initialize(
                activity,
                new InitializationConfig.Builder(activity.getString(R.string.admob_app_id)).build(),
                status -> activity.runOnUiThread(() -> {
                    if (destroyed) {
                        return;
                    }
                    AdRequest request = new AdRequest.Builder(BuildConfig.INTERSTITIAL_AD_UNIT_ID).build();
                    InterstitialAdPreloader.start(
                            BuildConfig.INTERSTITIAL_AD_UNIT_ID,
                            new PreloadConfiguration(request, 1),
                            new PreloadCallback() {
                                @Override
                                public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                                    Log.d(TAG, "Interstitial preloaded.");
                                }

                                @Override
                                public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError error) {
                                    Log.d(TAG, "Interstitial preload failed: " + error.getMessage());
                                }

                                @Override
                                public void onAdsExhausted(@NonNull String preloadId) {
                                    Log.d(TAG, "Interstitial preload cache exhausted.");
                                }
                            });
                    AdRequest rewardedRequest = new AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build();
                    RewardedAdPreloader.start(
                            BuildConfig.REWARDED_AD_UNIT_ID,
                            new PreloadConfiguration(rewardedRequest, 1),
                            new PreloadCallback() {
                                @Override
                                public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
                                    Log.d(TAG, "Rewarded ad preloaded.");
                                }

                                @Override
                                public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError error) {
                                    Log.d(TAG, "Rewarded preload failed: " + error.getMessage());
                                }

                                @Override
                                public void onAdsExhausted(@NonNull String preloadId) {
                                    Log.d(TAG, "Rewarded preload cache exhausted.");
                                }
                            });
                }))).start();
    }

    void destroy() {
        destroyed = true;
        if (adsInitialized) {
            InterstitialAdPreloader.destroy(BuildConfig.INTERSTITIAL_AD_UNIT_ID);
            RewardedAdPreloader.destroy(BuildConfig.REWARDED_AD_UNIT_ID);
            adsInitialized = false;
        }
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }
}
