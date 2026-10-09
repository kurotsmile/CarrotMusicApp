package com.carrot.heartbeatmusic;

public class MusicListMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Danh sách nhạc";
    }

    @Override
    public String subtitle() {
        return "Bài hát theo ngôn ngữ đang chọn";
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
