package com.carrot.heartbeatmusic;

public class MemoryMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Music Memories";
    }

    @Override
    public String subtitle() {
        return "Listen across the timeline";
    }

    @Override
    public int iconRes() {
        return R.drawable.memory;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Music Memories", "memories", R.drawable.memory);
    }
}
