package com.carrot.heartbeatmusic;

public class ArtistMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Nghệ sĩ";
    }

    @Override
    public String subtitle() {
        return "Khám phá nghệ sĩ và bài hát liên quan";
    }

    @Override
    public int iconRes() {
        return R.drawable.singer;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Nghệ sĩ", "artists", R.drawable.singer);
    }
}
