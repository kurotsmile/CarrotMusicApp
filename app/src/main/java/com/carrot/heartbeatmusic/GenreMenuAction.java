package com.carrot.heartbeatmusic;

public class GenreMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Genres";
    }

    @Override
    public String subtitle() {
        return "Explore every shade of music";
    }

    @Override
    public int iconRes() {
        return R.drawable.genre;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Genres", "genres", R.drawable.genre);
    }
}
