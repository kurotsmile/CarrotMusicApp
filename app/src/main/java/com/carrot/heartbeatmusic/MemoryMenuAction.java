package com.carrot.heartbeatmusic;

public class MemoryMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Ký ức âm nhạc";
    }

    @Override
    public String subtitle() {
        return "Nghe nhạc theo dòng thời gian";
    }

    @Override
    public int iconRes() {
        return R.drawable.memory;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Ký ức âm nhạc", "memories", R.drawable.memory);
    }
}
