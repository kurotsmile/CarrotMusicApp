package com.carrot.heartbeatmusic;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int DARK = Color.rgb(16, 11, 9);
    private static final int BG = Color.rgb(255, 247, 242);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(21, 17, 15);
    private static final int MUTED = Color.rgb(117, 97, 91);
    private static final int ACCENT = Color.rgb(255, 106, 0);

    private final MusicRepository repository = new MusicRepository();
    private final ImageLoader imageLoader = new ImageLoader();
    private final List<Song> currentSongs = new ArrayList<>();
    private LinearLayout songList;
    private ProgressBar progressBar;
    private TextView titleView;
    private TextView nowPlayingView;
    private ImageButton playPauseButton;
    private MediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
        buildUi();
        connectController();
        loadHome();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(16));
        header.setBackgroundColor(DARK);
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        titleView = new TextView(this);
        titleView.setText("Heartbeat Music");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(28);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(titleView);

        TextView subtitle = new TextView(this);
        subtitle.setText("Heart Beat Play trong giao diện Android native");
        subtitle.setTextColor(Color.rgb(255, 207, 189));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        header.addView(subtitle);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Tìm bài hát, nghệ sĩ, album...");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.rgb(255, 215, 196));
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackgroundColor(Color.rgb(36, 17, 11));
        header.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
        search.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = view.getText().toString().trim();
                if (query.isEmpty()) {
                    loadHome();
                } else {
                    loadSearch(query);
                }
                return true;
            }
            return false;
        });

        progressBar = new ProgressBar(this);
        page.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        ScrollView scrollView = new ScrollView(this);
        songList = new LinearLayout(this);
        songList.setOrientation(LinearLayout.VERTICAL);
        songList.setPadding(dp(14), dp(14), dp(14), dp(96));
        scrollView.addView(songList);
        page.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout player = new LinearLayout(this);
        player.setGravity(Gravity.CENTER_VERTICAL);
        player.setPadding(dp(14), dp(10), dp(14), dp(10));
        player.setBackgroundColor(DARK);
        FrameLayout.LayoutParams playerParams = new FrameLayout.LayoutParams(-1, dp(76), Gravity.BOTTOM);
        root.addView(player, playerParams);

        playPauseButton = new ImageButton(this);
        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        playPauseButton.setColorFilter(Color.WHITE);
        playPauseButton.setBackgroundColor(ACCENT);
        player.addView(playPauseButton, new LinearLayout.LayoutParams(dp(52), dp(52)));
        playPauseButton.setOnClickListener(view -> {
            if (controller == null) {
                return;
            }
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                controller.play();
            }
            updatePlayerState();
        });

        nowPlayingView = new TextView(this);
        nowPlayingView.setText("Chọn một bài hát để phát");
        nowPlayingView.setTextColor(Color.WHITE);
        nowPlayingView.setTextSize(15);
        nowPlayingView.setPadding(dp(12), 0, 0, 0);
        nowPlayingView.setSingleLine(false);
        player.addView(nowPlayingView, new LinearLayout.LayoutParams(0, -2, 1));

        setContentView(root);
    }

    private void connectController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicPlaybackService.class));
        ListenableFuture<MediaController> controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                updatePlayerState();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, MoreExecutors.directExecutor());
    }

    private void loadHome() {
        progressBar.setVisibility(View.VISIBLE);
        repository.loadHome(new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> renderSongs("Bài hát mới", songs));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void loadSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        repository.search(query, new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> renderSongs("Kết quả: " + query, songs));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void renderSongs(String heading, List<Song> songs) {
        progressBar.setVisibility(View.GONE);
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();

        TextView headingView = new TextView(this);
        headingView.setText(heading);
        headingView.setTextSize(22);
        headingView.setTextColor(TEXT);
        headingView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        headingView.setPadding(dp(2), 0, 0, dp(10));
        songList.addView(headingView);

        if (songs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa tìm thấy bài hát có thể phát.");
            empty.setTextColor(MUTED);
            empty.setTextSize(16);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        for (int i = 0; i < songs.size(); i++) {
            songList.addView(songRow(songs.get(i), i));
        }
    }

    private View songRow(Song song, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundColor(CARD);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView cover = new ImageView(this);
        cover.setBackgroundColor(Color.rgb(255, 226, 209));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(cover, new LinearLayout.LayoutParams(dp(72), dp(72)));
        imageLoader.load(song.avatar, cover);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12), 0, dp(8), 0);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = new TextView(this);
        name.setText(song.name);
        name.setTextColor(TEXT);
        name.setTextSize(17);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        meta.addView(name);

        TextView artist = new TextView(this);
        artist.setText(song.artist.isEmpty() ? "Heart Beat Play" : song.artist);
        artist.setTextColor(MUTED);
        artist.setTextSize(13);
        artist.setPadding(0, dp(4), 0, 0);
        meta.addView(artist);

        ImageButton play = new ImageButton(this);
        play.setImageResource(android.R.drawable.ic_media_play);
        play.setColorFilter(Color.WHITE);
        play.setBackgroundColor(ACCENT);
        row.addView(play, new LinearLayout.LayoutParams(dp(46), dp(46)));
        play.setOnClickListener(view -> playAt(index));
        row.setOnClickListener(view -> playAt(index));
        return row;
    }

    private void playAt(int index) {
        if (controller == null || index < 0 || index >= currentSongs.size()) {
            return;
        }
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : currentSongs) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.name)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(Uri.parse(song.avatar))
                    .build();
            MediaItem item = new MediaItem.Builder()
                    .setUri(song.mp3)
                    .setMediaId(song.id)
                    .setMediaMetadata(metadata)
                    .build();
            mediaItems.add(item);
        }
        controller.setMediaItems(mediaItems, index, 0);
        controller.prepare();
        controller.play();
        Song song = currentSongs.get(index);
        nowPlayingView.setText(song.name + "\n" + (song.artist.isEmpty() ? "Heart Beat Play" : song.artist));
        updatePlayerState();
    }

    private void updatePlayerState() {
        if (controller != null && controller.isPlaying()) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.release();
            controller = null;
        }
        super.onDestroy();
    }
}
