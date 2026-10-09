package com.carrot.heartbeatmusic;

public class GenreMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Thể loại";
    }

    @Override
    public String subtitle() {
        return "Khám phá màu sắc âm nhạc";
    }

    @Override
    public int iconRes() {
        return R.drawable.genre;
    }

    @Override
    public void open(MainActivity activity) {
        activity.loadMenuItems("Thể loại", "genres", R.drawable.genre);
    }
}
