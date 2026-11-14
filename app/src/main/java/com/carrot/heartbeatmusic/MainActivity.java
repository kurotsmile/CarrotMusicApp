package com.carrot.heartbeatmusic;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int VOICE_SEARCH_REQUEST = 42;
    private static final int[] SLEEP_TIMER_MINUTES = new int[]{5, 15, 30, 60, 120, 180};
    private static final int DARK = Color.rgb(16, 11, 9);
    private static final int BG = Color.rgb(255, 247, 242);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(21, 17, 15);
    private static final int MUTED = Color.rgb(117, 97, 91);
    private static final int ACCENT = Color.rgb(255, 106, 0);

    private MusicRepository repository;
    private final ImageLoader imageLoader = new ImageLoader();
    private final PlaylistManager playlistManager = new PlaylistManager();
    private final List<Song> currentSongs = new ArrayList<>();
    private final List<Song> homeSongs = new ArrayList<>();
    private final List<Song> monthlyRankSongs = new ArrayList<>();
    private final Map<String, List<ImageView>> songLoadingViews = new HashMap<>();
    private final List<MusicLanguage> languages = new ArrayList<>();
    private final List<MusicMenuAction> menuActions = new ArrayList<>();
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private LinearLayout songList;
    private ScrollView mainScrollView;
    private ProgressBar progressBar;
    private ProgressBar playbackProgressBar;
    private TextView nowPlayingTitleView;
    private TextView nowPlayingArtistView;
    private LinearLayout languageButton;
    private ImageView languageIcon;
    private TextView languageText;
    private EditText searchInput;
    private ImageView searchLoadingIcon;
    private ImageButton searchClearButton;
    private ImageButton searchMicButton;
    private LinearLayout playerBar;
    private ImageButton menuButton;
    private ImageButton playlistButton;
    private ImageButton prevButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton infoButton;
    private ImageButton stopButton;
    private ImageView detailPlayIcon;
    private AudioWaveView detailWaveView;
    private String detailSongId = "";
    private Song currentPlayingSong = null;
    private String loadingSongId = "";
    private SharedPreferences appPrefs;
    private MediaController controller;
    private String currentLang = "en";
    private String lastHeading = "New songs";
    private String currentHomeTab = "new";
    private boolean showingDetail = false;
    private boolean showingHomeTabs = false;
    private boolean detailBackToPlaylist = false;
    private boolean currentListHasHomeBack = false;
    private boolean searchLoading = false;
    private boolean accountLoggedIn = false;
    private String accountName = "";
    private String accountEmail = "";
    private boolean sleepTimerActive = false;
    private boolean sleepTimerExitApp = false;
    private int sleepTimerIndex = 2;
    private Runnable sleepTimerRunnable = null;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playerBar != null && playbackState != Player.STATE_IDLE) {
                playerBar.setVisibility(View.VISIBLE);
            }
            if (playbackProgressBar != null) {
                playbackProgressBar.setVisibility(playbackState == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
            }
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                setLoadingSong("");
            }
            updatePlayerState();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updatePlayerState();
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            if (mediaItem == null) {
                return;
            }
            int playlistIndex = playlistManager.indexOf(mediaItem.mediaId);
            if (playlistIndex >= 0) {
                playlistManager.setCurrentIndex(playlistIndex);
                updateNowPlaying(playlistManager.current());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        }
        repository = new MusicRepository(this);
        appPrefs = getSharedPreferences("heartbeat_music_app", MODE_PRIVATE);
        currentLang = appPrefs.getString("selected_lang", "en");
        accountLoggedIn = appPrefs.getBoolean("account_logged_in", false);
        accountName = appPrefs.getString("account_name", "");
        accountEmail = appPrefs.getString("account_email", "");
        setupMenuActions();
        buildUi();
        connectController();
        loadLanguages();
        loadHome();
    }

    private void setupMenuActions() {
        menuActions.clear();
        menuActions.add(new ProfileMenuAction());
        menuActions.add(new MusicListMenuAction());
        menuActions.add(new ArtistMenuAction());
        menuActions.add(new GenreMenuAction());
        menuActions.add(new MemoryMenuAction());
        menuActions.add(new TourismMenuAction());
        menuActions.add(new SleepTimerMenuAction());
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

        playlistButton = new ImageButton(this);
        playlistButton.setImageResource(R.drawable.playlist);
        playlistButton.setColorFilter(Color.WHITE);
        playlistButton.setBackgroundColor(Color.rgb(36, 17, 11));
        playlistButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        playlistButton.setPadding(dp(9), dp(9), dp(9), dp(9));
        LinearLayout.LayoutParams playlistParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        playlistParams.setMargins(dp(8), 0, 0, 0);
        titleRow.addView(playlistButton, playlistParams);
        playlistButton.setOnClickListener(view -> renderPlaylistScreen());

        languageButton = new LinearLayout(this);
        languageButton.setGravity(Gravity.CENTER);
        languageButton.setOrientation(LinearLayout.HORIZONTAL);
        languageButton.setPadding(dp(12), 0, dp(12), 0);
        languageButton.setBackgroundColor(Color.rgb(36, 17, 11));
        languageIcon = new ImageView(this);
        languageIcon.setImageResource(R.drawable.country);
        languageButton.addView(languageIcon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        languageText = new TextView(this);
        languageText.setText((currentLang == null || currentLang.isEmpty() ? "en" : currentLang).toUpperCase());
        languageText.setTextColor(Color.WHITE);
        languageText.setTextSize(13);
        languageText.setTypeface(Typeface.DEFAULT_BOLD);
        languageText.setPadding(dp(7), 0, 0, 0);
        languageButton.addView(languageText);
        titleRow.addView(languageButton, new LinearLayout.LayoutParams(-2, dp(42)));
        languageButton.setOnClickListener(view -> showLanguageChooser());

        menuButton = new ImageButton(this);
        menuButton.setImageResource(R.drawable.menu);
        menuButton.setColorFilter(Color.WHITE);
        menuButton.setBackgroundColor(Color.rgb(36, 17, 11));
        menuButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        menuButton.setPadding(dp(9), dp(9), dp(9), dp(9));
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        menuParams.setMargins(dp(8), 0, 0, 0);
        titleRow.addView(menuButton, menuParams);
        menuButton.setOnClickListener(view -> showMainMenuPanel());

        TextView subtitle = new TextView(this);
        subtitle.setText("A warm place for songs, memories, and music from around the world.");
        subtitle.setTextColor(Color.rgb(255, 207, 189));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        header.addView(subtitle);

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setPadding(dp(12), 0, dp(8), 0);
        searchBar.setBackgroundColor(Color.rgb(36, 17, 11));
        header.addView(searchBar, new LinearLayout.LayoutParams(-1, dp(48)));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search songs, artists, albums...");
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(Color.rgb(255, 215, 196));
        searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setPadding(0, 0, dp(8), 0);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchBar.addView(searchInput, new LinearLayout.LayoutParams(0, -1, 1));
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
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
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                updateSearchActions();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        searchLoadingIcon = new ImageView(this);
        searchLoadingIcon.setImageResource(R.drawable.waiting);
        searchLoadingIcon.setColorFilter(Color.WHITE);
        searchLoadingIcon.setVisibility(View.GONE);
        searchBar.addView(searchLoadingIcon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        searchClearButton = new ImageButton(this);
        searchClearButton.setImageResource(R.drawable.delete_search);
        searchClearButton.setColorFilter(Color.WHITE);
        searchClearButton.setBackgroundColor(Color.TRANSPARENT);
        searchClearButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        searchClearButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        searchClearButton.setVisibility(View.GONE);
        searchBar.addView(searchClearButton, new LinearLayout.LayoutParams(dp(38), dp(38)));
        searchClearButton.setOnClickListener(view -> {
            searchInput.setText("");
            loadHome();
        });

        searchMicButton = new ImageButton(this);
        searchMicButton.setImageResource(R.drawable.microphone);
        searchMicButton.setColorFilter(Color.WHITE);
        searchMicButton.setBackgroundColor(Color.TRANSPARENT);
        searchMicButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        searchMicButton.setPadding(dp(7), dp(7), dp(7), dp(7));
        searchBar.addView(searchMicButton, new LinearLayout.LayoutParams(dp(38), dp(38)));
        searchMicButton.setOnClickListener(view -> startVoiceSearch());

        progressBar = new ProgressBar(this);
        page.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        mainScrollView = new ScrollView(this);
        songList = new LinearLayout(this);
        songList.setOrientation(LinearLayout.VERTICAL);
        songList.setPadding(dp(14), dp(14), dp(14), dp(104));
        mainScrollView.addView(songList);
        page.addView(mainScrollView, new LinearLayout.LayoutParams(-1, 0, 1));

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

        prevButton = new ImageButton(this);
        prevButton.setImageResource(R.drawable.prev);
        prevButton.setColorFilter(Color.WHITE);
        prevButton.setBackgroundColor(Color.rgb(36, 17, 11));
        prevButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        prevButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        prevButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams prevParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        prevParams.setMargins(0, 0, dp(8), 0);
        playerContent.addView(prevButton, prevParams);
        prevButton.setOnClickListener(view -> playPreviousFromPlaylist());

        playPauseButton = new ImageButton(this);
        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        playPauseButton.setColorFilter(Color.WHITE);
        playPauseButton.setBackgroundColor(ACCENT);
        playPauseButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        playPauseButton.setPadding(dp(10), dp(10), dp(10), dp(10));
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

        nextButton = new ImageButton(this);
        nextButton.setImageResource(R.drawable.next);
        nextButton.setColorFilter(Color.WHITE);
        nextButton.setBackgroundColor(Color.rgb(36, 17, 11));
        nextButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        nextButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        nextButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        nextParams.setMargins(dp(8), 0, 0, 0);
        playerContent.addView(nextButton, nextParams);
        nextButton.setOnClickListener(view -> playNextFromPlaylist());

        LinearLayout nowPlayingBox = new LinearLayout(this);
        nowPlayingBox.setOrientation(LinearLayout.VERTICAL);
        nowPlayingBox.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingBox.setPadding(dp(12), 0, 0, 0);
        playerContent.addView(nowPlayingBox, new LinearLayout.LayoutParams(0, -2, 1));

        nowPlayingTitleView = new TextView(this);
        nowPlayingTitleView.setText("Choose a song to play");
        nowPlayingTitleView.setTextColor(Color.WHITE);
        nowPlayingTitleView.setTextSize(15);
        nowPlayingTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        nowPlayingTitleView.setSingleLine(true);
        nowPlayingBox.addView(nowPlayingTitleView);

        nowPlayingArtistView = new TextView(this);
        nowPlayingArtistView.setText("Heart Beat Play");
        nowPlayingArtistView.setTextColor(ACCENT);
        nowPlayingArtistView.setTextSize(13);
        nowPlayingArtistView.setSingleLine(true);
        nowPlayingBox.addView(nowPlayingArtistView);

        infoButton = new ImageButton(this);
        infoButton.setImageResource(R.drawable.info);
        infoButton.setColorFilter(Color.WHITE);
        infoButton.setBackgroundColor(Color.rgb(36, 17, 11));
        infoButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        infoButton.setPadding(dp(11), dp(11), dp(11), dp(11));
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        infoParams.setMargins(dp(10), 0, 0, 0);
        playerContent.addView(infoButton, infoParams);
        infoButton.setOnClickListener(view -> showCurrentPlayingDetail());

        stopButton = new ImageButton(this);
        stopButton.setImageResource(R.drawable.stop);
        stopButton.setColorFilter(Color.WHITE);
        stopButton.setBackgroundColor(Color.rgb(36, 17, 11));
        stopButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        stopButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        stopParams.setMargins(dp(10), 0, 0, 0);
        playerContent.addView(stopButton, stopParams);
        stopButton.setOnClickListener(view -> stopAndHidePlayer());

        setContentView(root);
    }

    private void showMainMenuPanel() {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(24), dp(18), dp(18));
        panel.setBackgroundColor(Color.WHITE);

        TextView title = bodyText("Heartbeat Music", 21, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(14));
        panel.addView(title);

        for (MusicMenuAction action : menuActions) {
            panel.addView(menuRow(action, dialog));
        }

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        dialog.setOnShowListener(shownDialog -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                shownWindow.setGravity(Gravity.END);
                shownWindow.setLayout(dp(304), WindowManager.LayoutParams.MATCH_PARENT);
            }
        });
        if (window != null) {
            window.setGravity(Gravity.END);
        }
        dialog.show();
    }

    private View menuRow(MusicMenuAction action, Dialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundColor(Color.rgb(255, 247, 242));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        boolean profileAction = action instanceof ProfileMenuAction;
        iconBox.setBackgroundColor(profileAction && accountLoggedIn ? ACCENT : Color.rgb(16, 11, 9));
        row.addView(iconBox, new LinearLayout.LayoutParams(dp(38), dp(38)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(profileAction && accountLoggedIn ? R.drawable.user_login : action.iconRes());
        icon.setColorFilter(action instanceof SleepTimerMenuAction && sleepTimerActive ? ACCENT : Color.WHITE);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconBox.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, 0, 0);
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView titleView = bodyText(action.title(), 16, TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(titleView);

        TextView subtitleView = bodyText(action.subtitle(), 12, MUTED);
        subtitleView.setPadding(0, dp(4), 0, 0);
        textBox.addView(subtitleView);
        row.setOnClickListener(view -> {
            dialog.dismiss();
            action.open(this);
        });
        return row;
    }

    void showProfileDialog() {
        showProfileDialog(false);
    }

    private void showProfileDialog(boolean registerMode) {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(24), dp(18), dp(18));
        panel.setBackgroundColor(Color.WHITE);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.carrot_28);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(-1, dp(72));
        logoParams.setMargins(0, 0, 0, dp(10));
        panel.addView(logo, logoParams);

        TextView title = bodyText(accountLoggedIn ? "Profile" : (registerMode ? "Create account" : "Sign in"), 21, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(8));
        panel.addView(title);

        if (accountLoggedIn) {
            TextView name = bodyText(accountName.isEmpty() ? accountEmail : accountName, 17, TEXT);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setGravity(Gravity.CENTER);
            panel.addView(name);
            TextView email = bodyText(accountEmail, 13, MUTED);
            email.setGravity(Gravity.CENTER);
            email.setPadding(0, dp(4), 0, dp(16));
            panel.addView(email);

            TextView cloudTitle = bodyText("Cloud playlists", 16, ACCENT);
            cloudTitle.setTypeface(Typeface.DEFAULT_BOLD);
            cloudTitle.setPadding(0, 0, 0, dp(8));
            panel.addView(cloudTitle);

            LinearLayout cloudList = new LinearLayout(this);
            cloudList.setOrientation(LinearLayout.VERTICAL);
            panel.addView(cloudList, new LinearLayout.LayoutParams(-1, -2));
            cloudList.addView(loadingRow("Loading cloud playlists..."));
            repository.loadCloudPlaylists(new MusicRepository.CloudPlaylistCallback() {
                @Override
                public void onPlaylists(List<CloudPlaylist> playlists) {
                    runOnUiThread(() -> {
                        cloudList.removeAllViews();
                        if (playlists.isEmpty()) {
                            cloudList.addView(bodyText("No cloud playlists yet.", 15, MUTED));
                            return;
                        }
                        for (CloudPlaylist playlist : playlists) {
                            cloudList.addView(cloudPlaylistRow(playlist));
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        cloudList.removeAllViews();
                        cloudList.addView(bodyText(message, 15, MUTED));
                    });
                }
            });

            TextView logout = profileActionButton("Sign out", Color.rgb(36, 17, 11), Color.WHITE);
            LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(-1, dp(46));
            logoutParams.setMargins(0, dp(16), 0, 0);
            panel.addView(logout, logoutParams);
            logout.setOnClickListener(view -> {
                accountLoggedIn = false;
                accountName = "";
                accountEmail = "";
                appPrefs.edit()
                        .putBoolean("account_logged_in", false)
                        .remove("account_name")
                        .remove("account_email")
                        .apply();
                repository.clearSession();
                dialog.dismiss();
                Toast.makeText(this, "Signed out.", Toast.LENGTH_SHORT).show();
            });
        } else {
            LinearLayout tabs = new LinearLayout(this);
            tabs.setGravity(Gravity.CENTER);
            tabs.setPadding(0, 0, 0, dp(12));
            panel.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
            TextView loginTab = profileActionButton("Login", registerMode ? Color.rgb(255, 240, 230) : ACCENT, registerMode ? ACCENT : Color.WHITE);
            TextView registerTab = profileActionButton("Register", registerMode ? ACCENT : Color.rgb(255, 240, 230), registerMode ? Color.WHITE : ACCENT);
            tabs.addView(loginTab, new LinearLayout.LayoutParams(0, dp(40), 1));
            LinearLayout.LayoutParams registerTabParams = new LinearLayout.LayoutParams(0, dp(40), 1);
            registerTabParams.setMargins(dp(8), 0, 0, 0);
            tabs.addView(registerTab, registerTabParams);
            loginTab.setOnClickListener(view -> {
                dialog.dismiss();
                showProfileDialog(false);
            });
            registerTab.setOnClickListener(view -> {
                dialog.dismiss();
                showProfileDialog(true);
            });

            EditText nameInput = null;
            if (registerMode) {
                nameInput = profileInput("Name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                panel.addView(nameInput, new LinearLayout.LayoutParams(-1, dp(48)));
            }
            EditText emailInput = profileInput("Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(-1, dp(48));
            emailParams.setMargins(0, registerMode ? dp(10) : 0, 0, 0);
            panel.addView(emailInput, emailParams);
            EditText passwordInput = profileInput("Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            LinearLayout.LayoutParams passwordParams = new LinearLayout.LayoutParams(-1, dp(48));
            passwordParams.setMargins(0, dp(10), 0, 0);
            panel.addView(passwordInput, passwordParams);

            EditText confirmInput = null;
            if (registerMode) {
                confirmInput = profileInput("Confirm password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(-1, dp(48));
                confirmParams.setMargins(0, dp(10), 0, 0);
                panel.addView(confirmInput, confirmParams);
            }

            TextView submit = profileActionButton(registerMode ? "Create account" : "Sign in", ACCENT, Color.WHITE);
            LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(-1, dp(46));
            submitParams.setMargins(0, dp(14), 0, 0);
            panel.addView(submit, submitParams);
            EditText finalNameInput = nameInput;
            EditText finalConfirmInput = confirmInput;
            submit.setOnClickListener(view -> {
                String name = finalNameInput == null || finalNameInput.getText() == null ? "" : finalNameInput.getText().toString().trim();
                String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
                String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
                String confirm = finalConfirmInput == null || finalConfirmInput.getText() == null ? password : finalConfirmInput.getText().toString();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (registerMode && name.isEmpty()) {
                    Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                    return;
                }
                submit.setEnabled(false);
                submit.setText(registerMode ? "Creating account..." : "Signing in...");
                MusicRepository.AccountCallback callback = new MusicRepository.AccountCallback() {
                    @Override
                    public void onAccount(String name, String email) {
                        runOnUiThread(() -> {
                            accountLoggedIn = true;
                            accountName = name == null ? "" : name;
                            accountEmail = email == null ? "" : email;
                            appPrefs.edit()
                                    .putBoolean("account_logged_in", true)
                                    .putString("account_name", accountName)
                                    .putString("account_email", accountEmail)
                                    .apply();
                            dialog.dismiss();
                            showProfileDialog();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            submit.setEnabled(true);
                            submit.setText(registerMode ? "Create account" : "Sign in");
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                };
                if (registerMode) {
                    repository.register(name, email, password, confirm, callback);
                } else {
                    repository.login(email, password, callback);
                }
            });
        }

        dialog.setContentView(panel);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.END);
            window.setLayout(dp(330), WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private EditText profileInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackgroundColor(Color.rgb(255, 247, 242));
        return input;
    }

    private TextView profileActionButton(String text, int background, int color) {
        TextView button = bodyText(text, 15, color);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackgroundColor(background);
        return button;
    }

    private View cloudPlaylistRow(CloudPlaylist playlist) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundColor(Color.rgb(255, 247, 242));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.playlist);
        icon.setColorFilter(Color.WHITE);
        icon.setBackgroundColor(ACCENT);
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, 0, 0);
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = bodyText(playlist.name.isEmpty() ? "Playlist" : playlist.name, 15, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(name);
        String subtitle = playlist.songCount + " songs";
        if (!playlist.description.isEmpty()) {
            subtitle += " · " + playlist.description;
        }
        textBox.addView(bodyText(subtitle, 12, MUTED));
        return row;
    }

    void showSleepTimerDialog() {
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(22), dp(18), dp(18));
        panel.setBackgroundColor(BG);

        TextView title = bodyText("Sleep Timer", 21, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(14));
        panel.addView(title);

        Switch activeSwitch = new Switch(this);
        activeSwitch.setText("Enable");
        activeSwitch.setTextColor(TEXT);
        activeSwitch.setTextSize(16);
        activeSwitch.setTypeface(Typeface.DEFAULT_BOLD);
        activeSwitch.setPadding(0, 0, 0, dp(14));
        activeSwitch.setChecked(sleepTimerActive);
        panel.addView(activeSwitch, new LinearLayout.LayoutParams(-1, -2));

        TextView timeLabel = bodyText(sleepTimerLabel(), 16, ACCENT);
        timeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        timeLabel.setPadding(0, 0, 0, dp(10));
        panel.addView(timeLabel);

        SeekBar timerSlider = new SeekBar(this);
        timerSlider.setMax(SLEEP_TIMER_MINUTES.length - 1);
        timerSlider.setProgress(sleepTimerIndex);
        panel.addView(timerSlider, new LinearLayout.LayoutParams(-1, -2));

        TextView actionLabel = bodyText("When time is up", 15, TEXT);
        actionLabel.setTypeface(Typeface.DEFAULT_BOLD);
        actionLabel.setPadding(0, dp(16), 0, dp(8));
        panel.addView(actionLabel);

        RadioGroup actionGroup = new RadioGroup(this);
        actionGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton stopMusic = new RadioButton(this);
        stopMusic.setId(1001);
        stopMusic.setText("Stop music");
        stopMusic.setTextColor(TEXT);
        stopMusic.setTextSize(15);
        actionGroup.addView(stopMusic);
        RadioButton closeApp = new RadioButton(this);
        closeApp.setId(1002);
        closeApp.setText("Close app");
        closeApp.setTextColor(TEXT);
        closeApp.setTextSize(15);
        actionGroup.addView(closeApp);
        actionGroup.check(sleepTimerExitApp ? 1002 : 1001);
        panel.addView(actionGroup, new LinearLayout.LayoutParams(-1, -2));

        activeSwitch.setOnCheckedChangeListener((button, checked) -> {
            sleepTimerActive = checked;
            if (checked) {
                scheduleSleepTimer();
                Toast.makeText(this, "Sleep timer enabled.", Toast.LENGTH_SHORT).show();
            } else {
                cancelSleepTimer();
                Toast.makeText(this, "Sleep timer disabled.", Toast.LENGTH_SHORT).show();
            }
        });
        timerSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sleepTimerIndex = progress;
                timeLabel.setText(sleepTimerLabel());
                if (sleepTimerActive && fromUser) {
                    scheduleSleepTimer();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        actionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            sleepTimerExitApp = checkedId == 1002;
            if (sleepTimerActive) {
                scheduleSleepTimer();
            }
        });

        dialog.setContentView(panel);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(dp(320), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private String sleepTimerLabel() {
        int minutes = SLEEP_TIMER_MINUTES[Math.max(0, Math.min(sleepTimerIndex, SLEEP_TIMER_MINUTES.length - 1))];
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " hr";
    }

    private void scheduleSleepTimer() {
        cancelSleepTimerRunnableOnly();
        sleepTimerActive = true;
        int minutes = SLEEP_TIMER_MINUTES[Math.max(0, Math.min(sleepTimerIndex, SLEEP_TIMER_MINUTES.length - 1))];
        sleepTimerRunnable = () -> {
            sleepTimerActive = false;
            stopAndHidePlayer();
            if (sleepTimerExitApp) {
                if (Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
            } else {
                Toast.makeText(this, "Music stopped by sleep timer.", Toast.LENGTH_SHORT).show();
            }
        };
        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60L * 1000L);
    }

    private void cancelSleepTimer() {
        sleepTimerActive = false;
        cancelSleepTimerRunnableOnly();
    }

    private void cancelSleepTimerRunnableOnly() {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
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
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(24), dp(18), dp(18));
        panel.setBackgroundColor(BG);

        TextView title = bodyText("Country + Language", 20, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(14));
        panel.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        for (MusicLanguage language : prioritizedLanguages()) {
            rows.addView(languageRow(language, dialog));
        }

        if (languages.isEmpty()) {
            TextView empty = bodyText("Loading languages...", 15, MUTED);
            empty.setPadding(dp(8), dp(22), dp(8), dp(22));
            rows.addView(empty);
        }

        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(dp(304), WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.END);
        }
        dialog.show();
        Window shownWindow = dialog.getWindow();
        if (shownWindow != null) {
            shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            shownWindow.setLayout(dp(304), WindowManager.LayoutParams.MATCH_PARENT);
            shownWindow.setGravity(Gravity.END);
        }
    }

    private List<MusicLanguage> prioritizedLanguages() {
        List<MusicLanguage> ordered = new ArrayList<>(languages);
        String selected = currentLang == null || currentLang.isEmpty() ? "en" : currentLang;
        String system = Locale.getDefault().getLanguage();
        ordered.sort((left, right) -> Integer.compare(languagePriority(left, selected, system), languagePriority(right, selected, system)));
        return ordered;
    }

    private int languagePriority(MusicLanguage language, String selected, String system) {
        String lang = language.lang == null ? "" : language.lang;
        if (lang.equalsIgnoreCase(selected)) {
            return 0;
        }
        if (!system.isEmpty() && lang.equalsIgnoreCase(system)) {
            return 1;
        }
        return 2;
    }

    private View languageRow(MusicLanguage language, Dialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackgroundColor(currentLang.equals(language.lang) ? Color.rgb(255, 240, 230) : CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.setBackgroundColor(Color.rgb(16, 11, 9));
        row.addView(iconBox, new LinearLayout.LayoutParams(dp(42), dp(42)));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iconBox.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
        if (language.icon == null || language.icon.isEmpty()) {
            icon.setImageResource(R.drawable.country);
            icon.setColorFilter(Color.WHITE);
        } else {
            icon.clearColorFilter();
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
            appPrefs.edit().putString("selected_lang", currentLang).apply();
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

    private void updateSearchActions() {
        if (searchInput == null || searchClearButton == null) {
            return;
        }
        boolean hasText = searchInput.getText() != null && searchInput.getText().toString().trim().length() > 0;
        searchClearButton.setVisibility(hasText && !searchLoading ? View.VISIBLE : View.GONE);
        if (searchMicButton != null) {
            searchMicButton.setEnabled(!searchLoading);
            searchMicButton.setAlpha(searchLoading ? 0.45f : 1f);
        }
    }

    private void setSearchLoading(boolean loading) {
        searchLoading = loading;
        if (searchInput != null) {
            searchInput.setEnabled(!loading);
            searchInput.setAlpha(loading ? 0.65f : 1f);
        }
        if (searchLoadingIcon != null) {
            searchLoadingIcon.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        updateSearchActions();
    }

    private void startVoiceSearch() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 21);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search songs");
        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Voice search is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHome() {
        showingDetail = false;
        showingHomeTabs = true;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        currentHomeTab = "new";
        progressBar.setVisibility(View.VISIBLE);
        showListLoading("Loading new songs...");
        repository.loadHome(currentLang, new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> {
                    homeSongs.clear();
                    homeSongs.addAll(songs);
                    renderHomeTabs("new");
                    loadMonthlyRank(false);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void loadMonthlyRank(boolean showWhenLoaded) {
        if (showWhenLoaded) {
            progressBar.setVisibility(View.VISIBLE);
            showListLoading("Loading monthly chart...");
        }
        repository.loadMonthlyRank(currentLang, new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> {
                    monthlyRankSongs.clear();
                    monthlyRankSongs.addAll(songs);
                    if (showWhenLoaded || "rank".equals(currentHomeTab)) {
                        renderHomeTabs("rank");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    void loadHomeFromMenu() {
        loadHome();
    }

    void loadMenuItems(String heading, String action, int fallbackIconRes) {
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = true;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        progressBar.setVisibility(View.VISIBLE);
        showListLoading("Loading " + heading.toLowerCase(Locale.getDefault()) + "...");
        repository.loadMenuItems(action, currentLang, new MusicRepository.MenuItemCallback() {
            @Override
            public void onItems(List<MenuListItem> items) {
                runOnUiThread(() -> renderMenuItems(heading, items, fallbackIconRes));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void loadSearch(String query) {
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        progressBar.setVisibility(View.VISIBLE);
        setSearchLoading(true);
        showListLoading("Searching songs...");
        repository.search(query, currentLang, new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> {
                    setSearchLoading(false);
                    renderSearchResults(query, songs);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setSearchLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void loadSongsByFilter(String heading, String action, String value) {
        if (value == null || value.trim().isEmpty()) {
            Toast.makeText(this, "No data available for this list.", Toast.LENGTH_SHORT).show();
            return;
        }
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = true;
        progressBar.setVisibility(View.VISIBLE);
        showListLoading("Loading " + heading.toLowerCase(Locale.getDefault()) + "...");
        repository.loadFilteredSongs(action, value, currentLang, new MusicRepository.Callback() {
            @Override
            public void onSongs(List<Song> songs) {
                runOnUiThread(() -> renderSongsWithBack(heading, songs, "List", () -> loadHome()));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void showListLoading(String message) {
        detailPlayIcon = null;
        detailWaveView = null;
        songLoadingViews.clear();
        songList.removeAllViews();
        scrollToTop();
        songList.addView(loadingRow(message));
    }

    private View loadingRow(String message) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackgroundColor(CARD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.waiting);
        icon.setColorFilter(ACCENT);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView text = bodyText(message, 15, MUTED);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setPadding(dp(12), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private void addResponsiveItems(List<View> items) {
        if (!isLandscapeLayout()) {
            for (View item : items) {
                songList.addView(item);
            }
            return;
        }
        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 0, 0, dp(10));
            songList.addView(row, rowParams);

            row.addView(items.get(i), twoColumnItemParams(false));
            if (i + 1 < items.size()) {
                row.addView(items.get(i + 1), twoColumnItemParams(true));
            } else {
                TextView spacer = new TextView(this);
                row.addView(spacer, twoColumnItemParams(true));
            }
        }
    }

    private LinearLayout.LayoutParams twoColumnItemParams(boolean right) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(right ? dp(6) : 0, 0, right ? 0 : dp(6), 0);
        return params;
    }

    private boolean isLandscapeLayout() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void renderSongs(String heading, List<Song> songs) {
        progressBar.setVisibility(View.GONE);
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        lastHeading = heading;
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();

        songList.addView(heading(heading));

        if (songs.isEmpty()) {
            TextView empty = bodyText("No playable songs found.", 16, MUTED);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        List<View> rows = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            rows.add(songRow(songs.get(i), i));
        }
        addResponsiveItems(rows);
    }

    private void renderSearchResults(String query, List<Song> songs) {
        renderSongsWithBack("Results: " + query, songs, "Main list", () -> loadHome());
    }

    private void renderSongsWithBack(String heading, List<Song> songs, String backLabel, Runnable backAction) {
        progressBar.setVisibility(View.GONE);
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = true;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        lastHeading = heading;
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();

        LinearLayout back = backButton(backLabel);
        back.setOnClickListener(view -> backAction.run());
        songList.addView(back);
        songList.addView(heading(heading));

        if (songs.isEmpty()) {
            TextView empty = bodyText("No playable songs found.", 16, MUTED);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        List<View> rows = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            rows.add(songRow(songs.get(i), i));
        }
        addResponsiveItems(rows);
    }

    private void renderHomeTabs(String activeTab) {
        progressBar.setVisibility(View.GONE);
        showingDetail = false;
        showingHomeTabs = true;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        currentHomeTab = activeTab == null || activeTab.isEmpty() ? "new" : activeTab;
        lastHeading = "rank".equals(currentHomeTab) ? "Monthly chart" : "New songs";
        List<Song> songs = "rank".equals(currentHomeTab) ? monthlyRankSongs : homeSongs;
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();

        songList.addView(homeTabs());

        if (songs.isEmpty()) {
            TextView empty = bodyText("No playable songs found.", 16, MUTED);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        List<View> rows = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            rows.add(songRow(songs.get(i), i));
        }
        addResponsiveItems(rows);
    }

    private LinearLayout homeTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(0, 0, 0, dp(12));
        tabs.addView(homeTabButton("New songs", "new"), new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        rankParams.setMargins(dp(8), 0, 0, 0);
        tabs.addView(homeTabButton("Monthly chart", "rank"), rankParams);
        return tabs;
    }

    private TextView homeTabButton(String text, String tab) {
        boolean active = tab.equals(currentHomeTab);
        TextView button = bodyText(text, 14, active ? Color.WHITE : ACCENT);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(roundedTopDrawable(active ? ACCENT : Color.rgb(255, 240, 230)));
        button.setOnClickListener(view -> {
            if ("rank".equals(tab) && monthlyRankSongs.isEmpty()) {
                currentHomeTab = "rank";
                loadMonthlyRank(true);
                return;
            }
            renderHomeTabs(tab);
        });
        return button;
    }

    private GradientDrawable roundedTopDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        float radius = dp(12);
        drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
        return drawable;
    }

    private void renderMenuItems(String heading, List<MenuListItem> items, int fallbackIconRes) {
        progressBar.setVisibility(View.GONE);
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        lastHeading = heading;
        currentSongs.clear();
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();
        songList.addView(heading(heading));
        if (!items.isEmpty() && "tourism".equals(items.get(0).type)) {
            songList.addView(mapIntroRow());
        }

        if (items.isEmpty()) {
            TextView empty = bodyText("No data yet.", 16, MUTED);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        List<View> rows = new ArrayList<>();
        for (MenuListItem item : items) {
            if ("memory".equals(item.type)) {
                rows.add(memoryTimelineRow(item, fallbackIconRes));
            } else if ("tourism".equals(item.type)) {
                rows.add(tourismMapRow(item, fallbackIconRes));
            } else {
                rows.add(menuListItemRow(item, fallbackIconRes));
            }
        }
        addResponsiveItems(rows);
    }

    private View menuListItemRow(MenuListItem item, int fallbackIconRes) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundColor(CARD);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setBackgroundColor(Color.rgb(255, 226, 209));
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));
        if (item.avatar == null || item.avatar.isEmpty()) {
            icon.setImageResource(fallbackIconRes);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        } else {
            imageLoader.load(item.avatar, icon);
        }

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, 0, 0);
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = bodyText(item.title, 17, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(title);

        if (item.subtitle != null && !item.subtitle.isEmpty()) {
            TextView subtitle = bodyText(item.subtitle, 13, MUTED);
            subtitle.setPadding(0, dp(4), 0, 0);
            textBox.addView(subtitle);
        }
        attachMenuItemClick(row, item);
        return row;
    }

    private View memoryTimelineRow(MenuListItem item, int fallbackIconRes) {
        LinearLayout row = (LinearLayout) menuListItemRow(item, fallbackIconRes);
        row.setPadding(dp(8), dp(10), dp(10), dp(10));
        TextView year = bodyText(item.id, 13, Color.WHITE);
        year.setGravity(Gravity.CENTER);
        year.setTypeface(Typeface.DEFAULT_BOLD);
        year.setBackgroundColor(ACCENT);
        row.addView(year, 0, new LinearLayout.LayoutParams(dp(52), dp(52)));
        return row;
    }

    private View tourismMapRow(MenuListItem item, int fallbackIconRes) {
        LinearLayout row = (LinearLayout) menuListItemRow(item, fallbackIconRes);
        row.setBackgroundColor(Color.rgb(255, 240, 230));
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        return row;
    }

    private View mapIntroRow() {
        TextView map = bodyText("World map · choose a country to hear music by region", 15, Color.WHITE);
        map.setGravity(Gravity.CENTER);
        map.setTypeface(Typeface.DEFAULT_BOLD);
        map.setPadding(dp(14), 0, dp(14), 0);
        map.setBackgroundColor(Color.rgb(36, 17, 11));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, 0, 0, dp(10));
        map.setLayoutParams(params);
        return map;
    }

    private void attachMenuItemClick(View row, MenuListItem item) {
        if ("artist".equals(item.type)) {
            row.setOnClickListener(view -> loadArtistDetail(item.id));
        } else if ("genre".equals(item.type)) {
            row.setOnClickListener(view -> loadSongsByFilter("Genre: " + item.title, "genre_songs", item.id));
        } else if ("memory".equals(item.type)) {
            row.setOnClickListener(view -> loadSongsByFilter(item.title, "year_songs", item.id));
        } else if ("tourism".equals(item.type)) {
            row.setOnClickListener(view -> loadSongsByFilter("Travel: " + item.title, "country_songs", item.id));
        }
    }

    private void renderPlaylistScreen() {
        progressBar.setVisibility(View.GONE);
        showingDetail = false;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        lastHeading = "Playlist";
        currentSongs.clear();
        currentSongs.addAll(playlistManager.playlistItems());
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();

        songList.addView(heading("Playlist"));

        if (currentSongs.isEmpty()) {
            TextView empty = bodyText("No songs in this playlist yet.", 16, MUTED);
            empty.setPadding(dp(8), dp(28), dp(8), dp(28));
            songList.addView(empty);
            return;
        }

        List<View> rows = new ArrayList<>();
        for (int i = 0; i < currentSongs.size(); i++) {
            rows.add(playlistRow(currentSongs.get(i), i));
        }
        addResponsiveItems(rows);
    }

    private void loadArtistDetail(String artistId) {
        if (artistId == null || artistId.trim().isEmpty()) {
            Toast.makeText(this, "Artist information is not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        showListLoading("Loading artist...");
        repository.loadArtist(artistId, new MusicRepository.ArtistCallback() {
            @Override
            public void onArtist(Artist artist, List<Song> songs) {
                runOnUiThread(() -> renderArtistDetail(artist, songs));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void renderArtistDetail(Artist artist, List<Song> songs) {
        progressBar.setVisibility(View.GONE);
        showingDetail = true;
        showingHomeTabs = false;
        detailBackToPlaylist = false;
        currentListHasHomeBack = false;
        detailPlayIcon = null;
        detailWaveView = null;
        detailSongId = "";
        lastHeading = "Artists";
        currentSongs.clear();
        currentSongs.addAll(songs);
        songList.removeAllViews();
        songLoadingViews.clear();
        scrollToTop();

        LinearLayout back = backButton("List");
        back.setOnClickListener(view -> renderSongs("Artists", new ArrayList<>(currentSongs)));
        songList.addView(back);

        ImageView avatar = new ImageView(this);
        avatar.setBackgroundColor(Color.rgb(255, 226, 209));
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(-1, dp(300));
        avatarParams.setMargins(0, 0, 0, dp(16));
        songList.addView(avatar, avatarParams);
        imageLoader.load(artist.avatar, avatar);

        songList.addView(heading(artist.name.isEmpty() ? "Artists" : artist.name));
        TagFlowLayout tags = new TagFlowLayout(this, dp(8), dp(8));
        tags.setPadding(0, dp(4), 0, dp(8));
        addTag(tags, "Songs", artist.songCount > 0 ? String.valueOf(artist.songCount) : String.valueOf(songs.size()));
        addTag(tags, "Language", artist.lang);
        songList.addView(tags);

        if (!artist.description.isEmpty()) {
            songList.addView(sectionTitle("Description"));
            TextView description = bodyText(artist.description.replace("\\n", "\n"), 15, MUTED);
            description.setLineSpacing(dp(3), 1.0f);
            songList.addView(description);
        }

        songList.addView(sectionTitle("Artist songs"));
        if (songs.isEmpty()) {
            TextView empty = bodyText("This artist has no songs yet.", 16, MUTED);
            empty.setPadding(dp(8), dp(18), dp(8), dp(24));
            songList.addView(empty);
            return;
        }
        List<View> rows = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            rows.add(songRow(songs.get(i), i));
        }
        addResponsiveItems(rows);
    }

    private View playlistRow(Song song, int index) {
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
        row.addView(cover, new LinearLayout.LayoutParams(dp(58), dp(58)));
        imageLoader.load(song.avatar, cover);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12), 0, dp(8), 0);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = bodyText(song.name, 16, TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        meta.addView(name);

        TextView artist = bodyText(song.artist.isEmpty() ? "Heart Beat Play" : song.artist, 12, MUTED);
        artist.setPadding(0, dp(4), 0, dp(6));
        meta.addView(artist);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        meta.addView(actions);

        actions.addView(smallIconButton(R.drawable.up_arrow, view -> {
            playlistManager.moveUp(index);
            renderPlaylistScreen();
        }));
        actions.addView(smallIconButton(R.drawable.down_arrow, view -> {
            playlistManager.moveDown(index);
            renderPlaylistScreen();
        }));
        actions.addView(smallIconButton(R.drawable.info, view -> {
            detailBackToPlaylist = true;
            showSongDetail(song, index);
        }));
        actions.addView(smallIconButton(R.drawable.delete, view -> {
            playlistManager.removeAt(index);
            updatePlaylistButtons();
            renderPlaylistScreen();
        }));

        row.setOnClickListener(view -> playAt(index));
        return row;
    }

    private View songRow(Song song, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundColor(CARD);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        FrameLayout coverFrame = new FrameLayout(this);
        row.addView(coverFrame, new LinearLayout.LayoutParams(dp(72), dp(72)));

        ImageView cover = new ImageView(this);
        cover.setBackgroundColor(Color.rgb(255, 226, 209));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverFrame.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        imageLoader.load(song.avatar, cover);

        ImageView loading = new ImageView(this);
        loading.setImageResource(R.drawable.waiting);
        loading.setColorFilter(Color.WHITE);
        loading.setBackgroundColor(Color.argb(122, 16, 11, 9));
        loading.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        loading.setPadding(dp(18), dp(18), dp(18), dp(18));
        loading.setVisibility(song.id.equals(loadingSongId) ? View.VISIBLE : View.GONE);
        coverFrame.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        registerSongLoadingView(song.id, loading);

        coverFrame.setOnClickListener(view -> {
            detailBackToPlaylist = false;
            showSongDetail(song, index);
        });

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

        ImageButton addPlaylist = new ImageButton(this);
        addPlaylist.setImageResource(R.drawable.add_to_playlist);
        addPlaylist.setColorFilter(Color.WHITE);
        addPlaylist.setBackgroundColor(Color.rgb(36, 17, 11));
        addPlaylist.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addPlaylist.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        addParams.setMargins(0, 0, dp(8), 0);
        row.addView(addPlaylist, addParams);
        addPlaylist.setOnClickListener(view -> addToPlaylist(song));

        ImageButton play = new ImageButton(this);
        play.setImageResource(android.R.drawable.ic_media_play);
        play.setColorFilter(Color.WHITE);
        play.setBackgroundColor(ACCENT);
        row.addView(play, new LinearLayout.LayoutParams(dp(46), dp(46)));
        play.setOnClickListener(view -> playAt(index));
        row.setOnClickListener(view -> playAt(index));
        return row;
    }

    private void registerSongLoadingView(String songId, ImageView view) {
        if (songId == null || songId.isEmpty()) {
            return;
        }
        List<ImageView> views = songLoadingViews.get(songId);
        if (views == null) {
            views = new ArrayList<>();
            songLoadingViews.put(songId, views);
        }
        views.add(view);
    }

    private void setLoadingSong(String songId) {
        loadingSongId = songId == null ? "" : songId;
        for (Map.Entry<String, List<ImageView>> entry : songLoadingViews.entrySet()) {
            int visibility = entry.getKey().equals(loadingSongId) ? View.VISIBLE : View.GONE;
            for (ImageView view : entry.getValue()) {
                view.setVisibility(visibility);
            }
        }
    }

    private void showSongDetail(Song song, int index) {
        showingDetail = true;
        progressBar.setVisibility(View.GONE);
        songList.removeAllViews();
        scrollToTop();

        LinearLayout back = backButton(lastHeading);
        back.setOnClickListener(view -> {
            if (detailBackToPlaylist) {
                renderPlaylistScreen();
            } else if (showingHomeTabs) {
                renderHomeTabs(currentHomeTab);
            } else if (currentListHasHomeBack) {
                renderSongsWithBack(lastHeading, new ArrayList<>(currentSongs), "List", () -> loadHome());
            } else {
                renderSongs(lastHeading, new ArrayList<>(currentSongs));
            }
        });
        songList.addView(back);

        FrameLayout coverFrame = new FrameLayout(this);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(-1, dp(320));
        coverParams.setMargins(0, 0, 0, dp(16));
        songList.addView(coverFrame, coverParams);

        ImageView cover = new ImageView(this);
        cover.setBackgroundColor(Color.rgb(255, 226, 209));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverFrame.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        imageLoader.load(song.avatar, cover);

        detailWaveView = new AudioWaveView(this);
        detailWaveView.setBackgroundColor(Color.argb(92, 16, 11, 9));
        detailWaveView.setVisibility(View.GONE);
        FrameLayout.LayoutParams waveParams = new FrameLayout.LayoutParams(dp(96), dp(58), Gravity.BOTTOM | Gravity.END);
        waveParams.setMargins(0, 0, dp(14), dp(14));
        coverFrame.addView(detailWaveView, waveParams);

        songList.addView(heading(song.name));
        songList.addView(songTagFlow(song));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(12), 0, dp(16));
        songList.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout play = iconActionButton("Play song", R.drawable.play);
        detailPlayIcon = (ImageView) play.getChildAt(0);
        detailSongId = song.id;
        updateDetailPlayIcon();
        actions.addView(play, new LinearLayout.LayoutParams(0, dp(48), 1));
        play.setOnClickListener(view -> playOrToggleAt(index));

        LinearLayout addPlaylist = iconActionButton("Playlist", R.drawable.add_to_playlist);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        addParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(addPlaylist, addParams);
        addPlaylist.setOnClickListener(view -> addToPlaylist(song));

        LinearLayout share = iconActionButton("Share", R.drawable.share);
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        shareParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(share, shareParams);
        share.setOnClickListener(view -> {
            if (!song.url.isEmpty()) {
                shareSong(song);
            }
        });

        if (!song.description.isEmpty()) {
            songList.addView(sectionTitle("Description"));
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

    private TagFlowLayout songTagFlow(Song song) {
        TagFlowLayout tags = new TagFlowLayout(this, dp(8), dp(8));
        tags.setPadding(0, dp(4), 0, dp(8));
        addTag(tags, "Artists", song.artist.isEmpty() ? "Heart Beat Play" : song.artist, R.drawable.singer, view -> {
            if (song.artistId == null || song.artistId.isEmpty()) {
                Toast.makeText(this, "Artist information is not available.", Toast.LENGTH_SHORT).show();
                return;
            }
            loadArtistDetail(song.artistId);
        });
        addTag(tags, "Album", song.album);
        addTag(tags, "Genre", song.genre, R.drawable.genre, view -> loadSongsByFilter("Genre: " + song.genre, "genre_songs", firstCsvValue(song.genre)));
        addTag(tags, "Year", song.year, R.drawable.memory, view -> loadSongsByFilter("Music from " + song.year, "year_songs", song.year));
        addTag(tags, "Language", song.country.isEmpty() ? song.lang : song.country + " · " + song.lang.toUpperCase());
        addTag(tags, "Plays", song.viewCount > 0 ? String.valueOf(song.viewCount) : "");
        return tags;
    }

    private void addTag(TagFlowLayout tags, String label, String value) {
        addTag(tags, label, value, 0, null);
    }

    private void addTag(TagFlowLayout tags, String label, String value, View.OnClickListener listener) {
        addTag(tags, label, value, 0, listener);
    }

    private void addTag(TagFlowLayout tags, String label, String value, int iconRes, View.OnClickListener listener) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        int tagColor = Color.rgb(138, 31, 17);
        TextView tag = bodyText(label + ": " + value, 13, tagColor);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setPadding(dp(10), 0, dp(10), 0);
        tag.setGravity(Gravity.CENTER);
        tag.setBackgroundColor(Color.rgb(255, 240, 230));
        if (iconRes != 0) {
            Drawable icon = getResources().getDrawable(iconRes).mutate();
            icon.setTint(tagColor);
            int iconSize = dp(14);
            icon.setBounds(0, 0, iconSize, iconSize);
            tag.setCompoundDrawables(icon, null, null, null);
            tag.setCompoundDrawablePadding(dp(5));
        }
        if (listener != null) {
            tag.setOnClickListener(listener);
        }
        tags.addView(tag, new LinearLayout.LayoutParams(-2, dp(34)));
    }

    private String firstCsvValue(String value) {
        if (value == null) {
            return "";
        }
        String[] parts = value.split(",");
        return parts.length == 0 ? value.trim() : parts[0].trim();
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

    private LinearLayout iconActionButton(String text, int iconRes) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackgroundColor(ACCENT);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

        TextView label = bodyText(text, 15, Color.WHITE);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(dp(8), 0, 0, 0);
        button.addView(label);
        return button;
    }

    private ImageButton smallIconButton(int iconRes, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setColorFilter(Color.WHITE);
        button.setBackgroundColor(Color.rgb(36, 17, 11));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(7), dp(7), dp(7), dp(7));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
        params.setMargins(0, 0, dp(6), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
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
        Song song = currentSongs.get(index);
        int playlistIndex = playlistManager.indexOf(song.id);
        if (playlistManager.hasMultiple() && playlistIndex >= 0) {
            playlistManager.setCurrentIndex(playlistIndex);
        } else {
            playlistManager.setSingle(song);
        }
        playPlaylistCurrent();
    }

    private void playPlaylistCurrent() {
        if (controller == null) {
            return;
        }
        Song song = playlistManager.current();
        if (song == null || !song.canPlay()) {
            return;
        }
        playerBar.setVisibility(View.VISIBLE);
        playbackProgressBar.setVisibility(View.VISIBLE);
        setLoadingSong(song.id);
        List<MediaItem> mediaItems = buildPlaylistMediaItems();
        int startIndex = playlistManager.isPlaylistActive() ? Math.max(0, playlistManager.currentIndex()) : 0;
        controller.setMediaItems(mediaItems, startIndex, 0);
        controller.prepare();
        controller.play();
        updateNowPlaying(song);
        updatePlayerState();
    }

    private List<MediaItem> buildPlaylistMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : playlistManager.items()) {
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
        return mediaItems;
    }

    private void updateNowPlaying(Song song) {
        currentPlayingSong = song;
        if (song == null) {
            nowPlayingTitleView.setText("Choose a song to play");
            nowPlayingArtistView.setText("Heart Beat Play");
        } else {
            nowPlayingTitleView.setText(song.name);
            nowPlayingArtistView.setText(song.artist.isEmpty() ? "Heart Beat Play" : song.artist);
        }
        updatePlaylistButtons();
    }

    private void addToPlaylist(Song song) {
        if (song == null || !song.canPlay()) {
            Toast.makeText(this, "This song has no playable audio file.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPlayingSong != null && playlistManager.indexOf(currentPlayingSong.id) < 0) {
            playlistManager.add(currentPlayingSong);
        }
        boolean added = playlistManager.add(song);
        if (currentPlayingSong != null && currentPlayingSong.id.equals(song.id)) {
            playlistManager.setCurrentIndex(playlistManager.indexOf(song.id));
        }
        updatePlaylistButtons();
        Toast.makeText(this, added ? "Added to playlist." : "Song is already in the playlist.", Toast.LENGTH_SHORT).show();
    }

    private void playNextFromPlaylist() {
        if (!playlistManager.hasMultiple()) {
            return;
        }
        playlistManager.next();
        playPlaylistCurrent();
    }

    private void playPreviousFromPlaylist() {
        if (!playlistManager.hasMultiple()) {
            return;
        }
        playlistManager.previous();
        playPlaylistCurrent();
    }

    private void updatePlaylistButtons() {
        int visibility = playlistManager.hasMultiple() && playlistManager.isPlaylistActive() ? View.VISIBLE : View.GONE;
        if (prevButton != null) {
            prevButton.setVisibility(visibility);
        }
        if (nextButton != null) {
            nextButton.setVisibility(visibility);
        }
    }

    private void playOrToggleAt(int index) {
        if (controller == null || index < 0 || index >= currentSongs.size()) {
            return;
        }
        Song song = currentSongs.get(index);
        String currentMediaId = controller.getCurrentMediaItem() == null ? "" : controller.getCurrentMediaItem().mediaId;
        if (song.id.equals(currentMediaId)) {
            playerBar.setVisibility(View.VISIBLE);
            if (controller.isPlaying()) {
                controller.pause();
            } else {
                playbackProgressBar.setVisibility(View.VISIBLE);
                controller.play();
            }
            updatePlayerState();
            return;
        }
        playAt(index);
    }

    private void stopAndHidePlayer() {
        if (controller != null) {
            controller.stop();
            controller.clearMediaItems();
        }
        playbackProgressBar.setVisibility(View.GONE);
        playerBar.setVisibility(View.GONE);
        playlistManager.setSingle(null);
        updateNowPlaying(null);
        detailSongId = "";
        currentPlayingSong = null;
        updatePlayerState();
    }

    private void showCurrentPlayingDetail() {
        if (currentPlayingSong == null) {
            Toast.makeText(this, "No song is currently playing.", Toast.LENGTH_SHORT).show();
            return;
        }
        int index = findSongIndex(currentPlayingSong.id);
        if (index < 0) {
            currentSongs.add(0, currentPlayingSong);
            index = 0;
        }
        detailBackToPlaylist = false;
        showSongDetail(currentPlayingSong, index);
    }

    private int findSongIndex(String songId) {
        for (int i = 0; i < currentSongs.size(); i++) {
            if (currentSongs.get(i).id.equals(songId)) {
                return i;
            }
        }
        return -1;
    }

    private void shareSong(Song song) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, song.name);
        shareIntent.putExtra(Intent.EXTRA_TEXT, song.name + "\n" + song.url);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void updatePlayerState() {
        if (controller != null && controller.isPlaying()) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
        updateDetailPlayIcon();
    }

    private void updateDetailPlayIcon() {
        if (detailPlayIcon == null) {
            return;
        }
        boolean detailSongIsPlaying = false;
        if (controller != null && controller.isPlaying() && controller.getCurrentMediaItem() != null) {
            String mediaId = controller.getCurrentMediaItem().mediaId;
            detailSongIsPlaying = mediaId != null && mediaId.equals(detailSongId);
        }
        detailPlayIcon.setImageResource(detailSongIsPlaying ? R.drawable.pause : R.drawable.play);
        if (detailWaveView != null) {
            detailWaveView.setVisibility(detailSongIsPlaying ? View.VISIBLE : View.GONE);
            if (detailSongIsPlaying) {
                detailWaveView.start();
            } else {
                detailWaveView.stop();
            }
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void scrollToTop() {
        if (mainScrollView != null) {
            mainScrollView.post(() -> mainScrollView.scrollTo(0, 0));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (showingDetail) {
            if (detailBackToPlaylist) {
                renderPlaylistScreen();
            } else if (showingHomeTabs) {
                renderHomeTabs(currentHomeTab);
            } else {
                renderSongs(lastHeading, new ArrayList<>(currentSongs));
            }
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != VOICE_SEARCH_REQUEST || resultCode != RESULT_OK || data == null) {
            return;
        }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) {
            return;
        }
        String query = results.get(0).trim();
        if (query.isEmpty()) {
            return;
        }
        searchInput.setText(query);
        searchInput.setSelection(searchInput.getText().length());
        loadSearch(query);
    }

    @Override
    protected void onDestroy() {
        cancelSleepTimerRunnableOnly();
        if (controller != null) {
            controller.removeListener(playerListener);
            controller.release();
            controller = null;
        }
        super.onDestroy();
    }
}
