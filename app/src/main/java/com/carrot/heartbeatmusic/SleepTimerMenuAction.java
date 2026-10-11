package com.carrot.heartbeatmusic;

public class SleepTimerMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Hẹn tắt nhạc";
    }

    @Override
    public String subtitle() {
        return "Tắt nhạc hoặc tắt ứng dụng theo thời gian";
    }

    @Override
    public int iconRes() {
        return R.drawable.cancel_alarm;
    }

    @Override
    public void open(MainActivity activity) {
        activity.showSleepTimerDialog();
    }
}
