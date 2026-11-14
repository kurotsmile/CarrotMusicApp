package com.carrot.heartbeatmusic;

public class TourismMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Travel";
    }

    @Override
    public String subtitle() {
        return "Music by country";
    }

    @Override
    public int iconRes() {
        return R.drawable.tourism;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Travel", "tourism", R.drawable.tourism);
    }
}
