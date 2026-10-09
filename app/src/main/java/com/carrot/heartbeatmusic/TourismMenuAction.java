package com.carrot.heartbeatmusic;

public class TourismMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Du lịch";
    }

    @Override
    public String subtitle() {
        return "Âm nhạc theo quốc gia";
    }

    @Override
    public int iconRes() {
        return R.drawable.tourism;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Du lịch", "tourism", R.drawable.tourism);
    }
}
