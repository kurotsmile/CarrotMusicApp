package com.carrot.heartbeatmusic;

public class SleepTimerMenuAction implements MusicMenuAction {
    @Override
    public String title() {
        return "Sleep Timer";
    }

    @Override
    public String subtitle() {
        return "Stop music or close the app later";
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
