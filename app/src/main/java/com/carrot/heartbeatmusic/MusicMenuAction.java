package com.carrot.heartbeatmusic;

public interface MusicMenuAction {
    String title();
    String subtitle();
    int iconRes();
    void open(MainActivity activity);
}
