package com.carrot.heartbeatmusic;

public class ArtistMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Artists";
    }

    @Override
    public String subtitle() {
        return "Explore artists and related songs";
    }

    @Override
    public int iconRes() {
        return R.drawable.singer;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Artists", "artists", R.drawable.singer);
    }
}
