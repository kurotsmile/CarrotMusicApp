package com.carrot.heartbeatmusic;

public class ProfileMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Profile";
    }

    @Override
    public String subtitle() {
        return "Sign in and sync your playlists";
    }

    @Override
    public int iconRes() {
        return R.drawable.profile;
    }

    @Override
    public void open(MainActivity activity) {
        activity.showProfileDialog();
    }
}
