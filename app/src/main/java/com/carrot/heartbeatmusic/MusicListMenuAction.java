package com.carrot.heartbeatmusic;

public class MusicListMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Music List";
    }

    @Override
    public String subtitle() {
        return "Songs for your selected language";
    }

    @Override
    public int iconRes() {
        return R.drawable.list_music;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadHomeFromMenu();
    }
}
