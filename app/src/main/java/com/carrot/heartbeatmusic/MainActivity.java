package com.carrot.heartbeatmusic;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.media3.common.Player;
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

    private MusicRepository repository;
    private final ImageLoader imageLoader = new ImageLoader();
    private final List<Song> currentSongs = new ArrayList<>();
    private final List<MusicLanguage> languages = new ArrayList<>();
    private LinearLayout songList;
    private ProgressBar progressBar;
    private ProgressBar playbackProgressBar;
    private TextView nowPlayingView;
    private LinearLayout languageButton;
    private ImageView languageIcon;
    private TextView languageText;
    private LinearLayout playerBar;
    private ImageButton playPauseButton;
    private MediaController controller;
    private String currentLang = "en";
    private String lastHeading = "Bài hát mới";
    private boolean showingDetail = false;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playerBar != null && playbackState != Player.STATE_IDLE) {
                playerBar.setVisibility(View.VISIBLE);
            }
            if (playbackProgressBar != null) {
                playbackProgressBar.setVisibility(playbackState == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
            }
            updatePlayerState();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updatePlayerState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
        repository = new MusicRepository(this);
        buildUi();
        connectController();
        loadLanguages();
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

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView titleView = new TextView(this);
        titleView.setText("Heartbeat Music");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(28);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));

        languageButton = new LinearLayout(this);
        languageButton.setGravity(Gravity.CENTER);
        languageButton.setOrientation(LinearLayout.HORIZONTAL);
        languageButton.setPadding(dp(12), 0, dp(12), 0);
        languageButton.setBackgroundColor(Color.rgb(36, 17, 11));
        languageIcon = new ImageView(this);
        languageIcon.setImageResource(R.drawable.country);
        languageButton.addView(languageIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        languageText = new TextView(this);
        languageText.setText("EN");
        languageText.setTextColor(Color.WHITE);
        languageText.setTextSize(13);
        languageText.setTypeface(Typeface.DEFAULT_BOLD);
        languageText.setPadding(dp(7), 0, 0, 0);
        languageButton.addView(languageText);
        titleRow.addView(languageButton, new LinearLayout.LayoutParams(-2, dp(42)));
        languageButton.setOnClickListener(view -> showLanguageChooser());

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
        songList.setPadding(dp(14), dp(14), dp(14), dp(104));
        scrollView.addView(songList);
        page.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        playerBar = new LinearLayout(this);
        playerBar.setOrientation(LinearLayout.VERTICAL);
        playerBar.setBackgroundColor(DARK);
        playerBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams playerParams = new FrameLayout.LayoutParams(-1, dp(84), Gravity.BOTTOM);
        root.addView(playerBar, playerParams);

        playbackProgressBar = new ProgressBar(this);
        playbackProgressBar.setIndeterminate(true);
        playbackProgressBar.setVisibility(View.GONE);
        playerBar.addView(playbackProgressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        LinearLayout playerContent = new LinearLayout(this);
        playerContent.setGravity(Gravity.CENTER_VERTICAL);
        playerContent.setPadding(dp(14), dp(10), dp(14), dp(10));
        playerBar.addView(playerContent, new LinearLayout.LayoutParams(-1, 0, 1));

        playPauseButton = new ImageButton(this);
        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        playPauseButton.setColorFilter(Color.WHITE);
        playPauseButton.setBackgroundColor(ACCENT);
        playerContent.addView(playPauseButton, new LinearLayout.LayoutParams(dp(52), dp(52)));
        playPauseButton.setOnClickListener(view -> {
            if (controller == null) {
                return;
            }
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                playbackProgressBar.setVisibility(View.VISIBLE);
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
        playerContent.addView(nowPlayingView, new LinearLayout.LayoutParams(0, -2, 1));

        setContentView(root);
    }

    private void connectController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicPlaybackService.class));
        ListenableFuture<MediaController> controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);
                updatePlayerState();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, MoreExecutors.directExecutor());
    }

    private void loadLanguages() {
        repository.loadLanguages(new MusicRepository.LanguageCallback() {
            @Override
            public void onLanguages(List<MusicLanguage> result) {
                runOnUiThread(() -> {
                    languages.clear();
                    languages.addAll(result);
                    updateSelectedLanguageUi(findLanguage(currentLang));
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showLanguageChooser() {
        Dialog dialog = new Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(12));
        box.setBackgroundColor(BG);

        TextView title = bodyText("Quốc gia + Ngôn ngữ", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        box.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(420)));

        for (MusicLanguage language : languages) {
            rows.addView(languageRow(language, dialog));
        }

        if (languages.isEmpty()) {
            TextView empty = bodyText("Đang tải danh sách ngôn ngữ...", 15, MUTED);
            empty.setPadding(dp(8), dp(22), dp(8), dp(22));
            rows.addView(empty);
        }

        dialog.setContentView(box);
        dialog.show();
    }

    private View languageRow(MusicLanguage language, Dialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setBackgroundColor(currentLang.equals(language.lang) ? Color.rgb(255, 240, 230) : CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setBackgroundColor(Color.rgb(255, 226, 209));
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        if (language.icon == null || language.icon.isEmpty()) {
            icon.setImageResource(R.drawable.country);
        } else {
            imageLoader.load(language.icon, icon);
        }

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, 0, 0);
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = bodyText(language.name.isEmpty() ? language.label() : language.name, 16, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(name);

        String secondary = language.country.isEmpty() ? language.lang.toUpperCase() : language.country.toUpperCase() + " · " + language.lang.toUpperCase();
        TextView code = bodyText(secondary, 12, MUTED);
        code.setPadding(0, dp(3), 0, 0);
        textBox.addView(code);

        row.setOnClickListener(view -> {
            currentLang = language.lang == null || language.lang.isEmpty() ? "en" : language.lang;
            updateSelectedLanguageUi(language);
            dialog.dismiss();
            loadHome();
        });
        return row;
    }

    private MusicLanguage findLanguage(String lang) {
        for (MusicLanguage language : languages) {
            if (language.lang.equals(lang)) {
                return language;
            }
        }
        return null;
    }

    private void updateSelectedLanguageUi(MusicLanguage language) {
        String lang = currentLang == null || currentLang.isEmpty() ? "en" : currentLang;
        languageText.setText(lang.toUpperCase());
        if (language == null || language.icon == null || language.icon.isEmpty()) {
            languageIcon.setImageResource(R.drawable.country);
        } else {
            imageLoader.load(language.icon, languageIcon);
        }
    }

    private void loadHome() {
        showingDetail = false;
        progressBar.setVisibility(View.VISIBLE);
        repository.loadHome(currentLang, new MusicRepository.Callback() {
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
        showingDetail = false;
        progressBar.setVisibility(View.VISIBLE);
        repository.search(query, currentLang, new MusicRepository.Callback() {
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
        showingDetail = false;
        lastHeading = heading;
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();

        songList.addView(heading(heading));

        if (songs.isEmpty()) {
            TextView empty = bodyText("Chưa tìm thấy bài hát có thể phát.", 16, MUTED);
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
        cover.setOnClickListener(view -> showSongDetail(song, index));

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12), 0, dp(8), 0);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = bodyText(song.name, 17, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        meta.addView(name);

        TextView artist = bodyText(song.artist.isEmpty() ? "Heart Beat Play" : song.artist, 13, MUTED);
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

    private void showSongDetail(Song song, int index) {
        showingDetail = true;
        progressBar.setVisibility(View.GONE);
        songList.removeAllViews();

        LinearLayout back = backButton(lastHeading);
        back.setOnClickListener(view -> renderSongs(lastHeading, new ArrayList<>(currentSongs)));
        songList.addView(back);

        ImageView cover = new ImageView(this);
        cover.setBackgroundColor(Color.rgb(255, 226, 209));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(-1, dp(320));
        coverParams.setMargins(0, 0, 0, dp(16));
        songList.addView(cover, coverParams);
        imageLoader.load(song.avatar, cover);

        songList.addView(heading(song.name));
        songList.addView(metaLine("Nghệ sĩ", song.artist.isEmpty() ? "Heart Beat Play" : song.artist));
        songList.addView(metaLine("Album", song.album));
        songList.addView(metaLine("Thể loại", song.genre));
        songList.addView(metaLine("Năm", song.year));
        songList.addView(metaLine("Ngôn ngữ", song.country.isEmpty() ? song.lang : song.country + " · " + song.lang.toUpperCase()));
        songList.addView(metaLine("Lượt nghe", song.viewCount > 0 ? String.valueOf(song.viewCount) : ""));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(12), 0, dp(16));
        songList.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        TextView play = actionButton("Phát bài hát");
        actions.addView(play, new LinearLayout.LayoutParams(0, dp(48), 1));
        play.setOnClickListener(view -> playAt(index));

        TextView open = actionButton("Mở web");
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        openParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(open, openParams);
        open.setOnClickListener(view -> {
            if (!song.url.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(song.url)));
            }
        });

        if (!song.description.isEmpty()) {
            songList.addView(sectionTitle("Mô tả"));
            songList.addView(bodyText(song.description, 15, MUTED));
        }
        if (!song.lyrics.trim().isEmpty()) {
            songList.addView(sectionTitle("Lyrics"));
            TextView lyrics = bodyText(song.lyrics.replace("\\n", "\n"), 15, TEXT);
            lyrics.setLineSpacing(dp(3), 1.0f);
            lyrics.setPadding(0, dp(8), 0, dp(24));
            songList.addView(lyrics);
        }
    }

    private View metaLine(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            TextView spacer = new TextView(this);
            spacer.setVisibility(View.GONE);
            return spacer;
        }
        TextView text = bodyText(label + ": " + value, 14, MUTED);
        text.setPadding(0, dp(5), 0, 0);
        return text;
    }

    private LinearLayout backButton(String label) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(12), 0, dp(14), 0);
        button.setBackgroundColor(Color.rgb(255, 240, 230));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44));
        params.setMargins(0, 0, 0, dp(12));
        button.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.back);
        icon.setColorFilter(ACCENT);
        button.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView text = bodyText(label, 15, ACCENT);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setPadding(dp(8), 0, 0, 0);
        button.addView(text);
        return button;
    }

    private TextView heading(String text) {
        TextView view = bodyText(text, 22, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(2), 0, 0, dp(10));
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = bodyText(text, 18, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(18), 0, dp(4));
        return view;
    }

    private TextView actionButton(String text) {
        TextView view = bodyText(text, 15, Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setBackgroundColor(ACCENT);
        return view;
    }

    private TextView bodyText(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void playAt(int index) {
        if (controller == null || index < 0 || index >= currentSongs.size()) {
            return;
        }
        playerBar.setVisibility(View.VISIBLE);
        playbackProgressBar.setVisibility(View.VISIBLE);
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
    public void onBackPressed() {
        if (showingDetail) {
            renderSongs(lastHeading, new ArrayList<>(currentSongs));
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (controller != null) {
            controller.removeListener(playerListener);
            controller.release();
            controller = null;
        }
        super.onDestroy();
    }
}
