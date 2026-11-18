package com.carrot.heartbeatmusic;

public class RemoveAdsMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Remove ads";
    }

    @Override
    public String subtitle() {
        return "One-time purchase for uninterrupted listening";
    }

    @Override
    public int iconRes() {
        return R.drawable.remove_ads;
    }

    @Override
    public void open(MainActivity activity) {
        activity.purchaseRemoveAds();
    }
}
