package com.carrot.heartbeatmusic;

import android.app.Activity;
import android.content.SharedPreferences;

final class MonetizationManager {
    MonetizationManager(Activity activity, SharedPreferences preferences) {
    }

    boolean isAvailable() {
        return false;
    }

    void start() {
    }

    void runAfterInterstitial(Runnable action) {
        action.run();
    }

    void runAfterRewarded(Runnable action) {
        action.run();
    }

    void purchaseRemoveAds() {
    }

    void destroy() {
    }
}
