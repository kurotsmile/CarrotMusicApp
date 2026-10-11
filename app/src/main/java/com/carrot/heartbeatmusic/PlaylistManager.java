package com.carrot.heartbeatmusic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistManager {
    private final List<Song> songs = new ArrayList<>();
    private int currentIndex = -1;
    private Song singleSong = null;
    private boolean playlistActive = false;

    public void setSingle(Song song) {
        singleSong = song;
        playlistActive = false;
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
            playlistActive = true;
        }
    }

    public Song current() {
        if (!playlistActive) {
            return singleSong;
        }
        if (currentIndex < 0 || currentIndex >= songs.size()) {
            return null;
        }
        return songs.get(currentIndex);
    }

    public Song next() {
        if (songs.isEmpty()) {
            return null;
        }
        playlistActive = true;
        currentIndex = (currentIndex + 1) % songs.size();
        return current();
    }

    public Song previous() {
        if (songs.isEmpty()) {
            return null;
        }
        playlistActive = true;
        currentIndex = (currentIndex - 1 + songs.size()) % songs.size();
        return current();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public boolean hasMultiple() {
        return songs.size() > 1;
    }

    public boolean isPlaylistActive() {
        return playlistActive;
    }

    public List<Song> items() {
        if (!playlistActive) {
            List<Song> queue = new ArrayList<>();
            if (singleSong != null) {
                queue.add(singleSong);
            }
            return Collections.unmodifiableList(queue);
        }
        return Collections.unmodifiableList(songs);
    }

    public List<Song> playlistItems() {
        return Collections.unmodifiableList(songs);
    }

    public boolean moveUp(int index) {
        if (index <= 0 || index >= songs.size()) {
            return false;
        }
        Collections.swap(songs, index, index - 1);
        if (currentIndex == index) {
            currentIndex = index - 1;
        } else if (currentIndex == index - 1) {
            currentIndex = index;
        }
        return true;
    }

    public boolean moveDown(int index) {
        if (index < 0 || index >= songs.size() - 1) {
            return false;
        }
        Collections.swap(songs, index, index + 1);
        if (currentIndex == index) {
            currentIndex = index + 1;
        } else if (currentIndex == index + 1) {
            currentIndex = index;
        }
        return true;
    }

    public boolean removeAt(int index) {
        if (index < 0 || index >= songs.size()) {
            return false;
        }
        Song removed = songs.remove(index);
        if (singleSong != null && singleSong.id.equals(removed.id)) {
            singleSong = null;
        }
        if (songs.isEmpty()) {
            currentIndex = -1;
            playlistActive = false;
            return true;
        }
        if (currentIndex >= songs.size()) {
            currentIndex = songs.size() - 1;
        } else if (index < currentIndex) {
            currentIndex--;
        }
        return true;
    }

    public void clear() {
        songs.clear();
        currentIndex = -1;
        singleSong = null;
        playlistActive = false;
    }
}
