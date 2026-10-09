# AGENTS.md - Heartbeat Music Android

Read this first when working in `/Volumes/htdocs/CarrotMusicApp`.

## Scope

This is the native Android app for CarrotMusic / Heart Beat Play. Keep changes inside this project unless the task explicitly involves the source API at `/Volumes/htdocs/CarrotMusic/api.php`.

Do not scan generated or large directories:

- `.git/`
- `.gradle/`
- `app/build/`
- `build/`

Use `rg` or `find` with targeted paths before opening files.

## Project Map

- `settings.gradle`, `build.gradle`, `gradle.properties`: Gradle project config.
- `app/build.gradle`: Android app config, product flavors, dependencies, signing.
- `app/src/main/AndroidManifest.xml`: shared Android permissions, activity, media playback service.
- `app/src/mobile/AndroidManifest.xml`: mobile launcher.
- `app/src/tv/AndroidManifest.xml`: Android TV / Leanback launcher.
- `app/src/main/java/com/carrot/heartbeatmusic/MainActivity.java`: native UI, list/detail screens, language selector, player controls.
- `app/src/main/java/com/carrot/heartbeatmusic/MusicRepository.java`: API calls and local cache logic.
- `app/src/main/java/com/carrot/heartbeatmusic/Song.java`: song JSON model.
- `app/src/main/java/com/carrot/heartbeatmusic/MusicLanguage.java`: language/country JSON model.
- `app/src/main/java/com/carrot/heartbeatmusic/MenuListItem.java`: generic right-menu list item model.
- `app/src/main/java/com/carrot/heartbeatmusic/MusicMenuAction.java`: interface for a right-panel menu action.
- `app/src/main/java/com/carrot/heartbeatmusic/MusicListMenuAction.java`: handles `Danh sách nhạc`.
- `app/src/main/java/com/carrot/heartbeatmusic/GenreMenuAction.java`: handles `Thể loại`.
- `app/src/main/java/com/carrot/heartbeatmusic/MemoryMenuAction.java`: handles `Ký ức âm nhạc`.
- `app/src/main/java/com/carrot/heartbeatmusic/TourismMenuAction.java`: handles `Du lịch`.
- `app/src/main/java/com/carrot/heartbeatmusic/PlaylistManager.java`: manages the in-app playlist queue, current index, and prev/next navigation.
- `app/src/main/java/com/carrot/heartbeatmusic/MusicPlaybackService.java`: Media3 background playback service.
- `app/src/main/java/com/carrot/heartbeatmusic/ImageLoader.java`: simple remote image loading helper.
- `app/src/main/java/com/carrot/heartbeatmusic/TagFlowLayout.java`: wraps detail tag chips onto multiple lines.
- `app/src/main/res/drawable/app_icon.png`: app icon copied from `icon.png`.
- `app/src/main/res/drawable/back.png`: back button icon copied from `images/back.png`.
- `app/src/main/res/drawable/add_to_playlist.png`, `next.png`, `prev.png`: playlist controls copied from `images/`.
- `images/`: source images provided by the user.
- `music_for_life.keystore`: existing keystore. Do not commit or expose passwords.

## Related API

The app reads data from `BuildConfig.MUSIC_API_BASE`, currently `https://heartbeatplay.com/api.php`. Local source is:

- `/Volumes/htdocs/CarrotMusic/api.php`

Only edit that file when changing app-facing JSON fields or cache-safe response formatting. Do not scan the whole `CarrotMusic` site unless the task needs existing helper behavior.

Useful API actions:

- `action=home&limit=36&lang=vi`
- `action=search&q=...&lang=vi`
- `action=languages`
- `action=song&id=...`
- `action=genres`
- `action=memories`
- `action=tourism`

## Existing Helpers To Reuse

- Use `MusicRepository.loadHome`, `search`, and `loadLanguages` for API loading.
- Use `MusicRepository` cache helpers instead of adding separate cache code elsewhere.
- Use `ImageLoader.load` for remote covers/icons.
- Use `Song` and `MusicLanguage` models rather than parsing JSON inside UI code.
- Use `MenuListItem` for right-panel menu lists.
- Put right-panel menu behavior in its own `*MenuAction.java` file.
- Use `PlaylistManager` for add-to-playlist, current queue, and prev/next state.
- Use `playAt()` / `playPlaylistCurrent()` in `MainActivity` to start playback so media metadata, notification, and lock-screen playback stay consistent.
- If API menu items have no avatar, render the matching local icon from `app/src/main/res/drawable/`.

## Build Notes

Because this project lives on `/Volumes`, Gradle may need a cache directory outside the mounted volume:

```sh
GRADLE_USER_HOME=/tmp/gradle-user-home /tmp/gradle-8.10.2/bin/gradle --no-daemon --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleMobileDebug
GRADLE_USER_HOME=/tmp/gradle-user-home /tmp/gradle-8.10.2/bin/gradle --no-daemon --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleTvDebug
```

If a system `gradle` is available, it can be used with the same `--project-cache-dir`.

## Checklist

- Keep UI changes in `MainActivity.java` unless adding a reusable model/helper is clearly useful.
- If adding JSON fields, update `/Volumes/htdocs/CarrotMusic/api.php` and the matching Java model.
- If changing public app resources, confirm both `mobile` and `tv` debug builds.
- Do not touch keystore credentials or generated build outputs.
