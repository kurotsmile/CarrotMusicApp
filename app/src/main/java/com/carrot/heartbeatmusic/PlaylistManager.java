package com.carrot.heartbeatmusic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistManager {
    private final List<Song> songs = new ArrayList<>();
    private int currentIndex = -1;

    public void setSingle(Song song) {
        songs.clear();
        if (song != null) {
            songs.add(song);
            currentIndex = 0;
        } else {
            currentIndex = -1;
        }
    }

    public boolean add(Song song) {
        if (song == null || song.id == null || song.id.isEmpty() || indexOf(song.id) >= 0) {
            return false;
        }
        songs.add(song);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        return true;
    }

    public int indexOf(String songId) {
        if (songId == null) {
            return -1;
        }
        for (int i = 0; i < songs.size(); i++) {
            if (songId.equals(songs.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < songs.size()) {
            currentIndex = index;
        }
    }

    public Song current() {
        if (currentIndex < 0 || currentIndex >= songs.size()) {
            return null;
        }
        return songs.get(currentIndex);
    }

    public Song next() {
        if (songs.isEmpty()) {
            return null;
        }
        currentIndex = (currentIndex + 1) % songs.size();
        return current();
    }

    public Song previous() {
        if (songs.isEmpty()) {
            return null;
        }
        currentIndex = (currentIndex - 1 + songs.size()) % songs.size();
        return current();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public boolean hasMultiple() {
        return songs.size() > 1;
    }

    public List<Song> items() {
        return Collections.unmodifiableList(songs);
    }

    public void clear() {
        songs.clear();
        currentIndex = -1;
    }
}
