# Heartbeat Music Android

Native Android app for the CarrotMusic / Heart Beat Play catalog.

## Build variants

Use one codebase with two build flavors:

- `mobile`: Android phone/tablet package `com.carrot.heartbeatmusic`.
- `tv`: Android TV package `com.carrot.heartbeatmusic.tv`, with Leanback launcher metadata.

This keeps playback/API code shared while allowing separate Play Store listings, package IDs, launcher behavior, and future TV-specific layout polish.

## Commands

The project lives on `/Volumes`, so Gradle may need a cache directory outside the mounted volume:

```sh
gradle --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleMobileDebug
gradle --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleTvDebug
```

Release builds:

```sh
gradle --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleMobileRelease
gradle --project-cache-dir /tmp/carrotmusicapp-gradle-cache :app:assembleTvRelease
```

## Signing

The release signing config points at the existing keystore:

```txt
music_for_life.keystore
```

Create `keystore.properties` from `keystore.properties.example`, or provide:

```sh
export MUSIC_STORE_PASSWORD="..."
export MUSIC_KEY_ALIAS="..."
export MUSIC_KEY_PASSWORD="..."
```

If credentials are missing, release builds fall back to debug signing so local builds still work.

## Data source

The app reads CarrotMusic data from:

```txt
https://heartbeatplay.com/api.php
```

The API file is implemented in `/Volumes/htdocs/CarrotMusic/api.php`.
