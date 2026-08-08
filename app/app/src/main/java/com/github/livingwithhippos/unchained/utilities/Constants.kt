package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.R

const val BASE_URL = "https://api.torbox.app/v1/api/"
const val REFERRAL_LINK = "https://torbox.app/subscription"
const val ACCOUNT_LINK = "https://torbox.app/settings"
const val DEFAULT_PLUGINS_REPOSITORY_LINK =
    "https://gitlab.com/LivingWithHippos/unchained-plugins/-/raw/main/repository/repository.json"

/** Folder name for the plugins installed manually, not from a web repository */
const val MANUAL_PLUGINS_REPOSITORY_NAME = "common_repository"

/** Value stored in the credentials to mark a token obtained by pasting it manually. */
const val AUTH_METHOD_MANUAL: String = "manual"

/** Value stored in the credentials to mark a token obtained through the device-code flow. */
const val AUTH_METHOD_DEVICE_FLOW: String = "device_flow"

const val HASH_PATTERN: String = "[a-zA-Z0-9]{32,}"
const val MAGNET_PATTERN: String = "magnet:\\?xt=urn:btih:([a-zA-Z0-9]{32,})"
const val TORRENT_PATTERN: String = "https?://[^\\s]{7,}\\.torrent"
const val IP_PATTERN: String = "^(((?!25?[6-9])[12]\\d|[1-9])?\\d\\.?\\b){4}"

const val FEEDBACK_URL = "https://github.com/LivingWithHippos/unchained-android"
const val GPLV3_URL = "https://www.gnu.org/licenses/gpl-3.0.en.html"

const val SCHEME_MAGNET = "magnet"
const val SCHEME_HTTP = "http"
const val SCHEME_HTTPS = "https"

// TorBox's download_state vocabulary (torrents/webdl "mylist" endpoints), confirmed live plus
// the documented qBittorrent-derived states. Unlike RD, TorBox has no explicit file-selection
// state: every file downloads automatically as soon as the torrent/webdl item is added.

/** States the item is not going to advance from. */
val endedStatusList = listOf("completed", "cached", "uploading")

/** States the item will still advance from, and is worth actively polling. */
val loadingStatusList =
    listOf(
        "downloading",
        "metaDL",
        "checkingResumeData",
        "queued",
        "paused",
        "stalled (no seeds)",
    )

const val DOWNLOADS_TAB = 0
const val TORRENTS_TAB = 1

object SIGNATURE {
    const val URL =
        "https://gist.githubusercontent.com/LivingWithHippos/5525e73f0439d06c1c3ff4f9484e35dd/raw/unchained_versions.json"
    const val PLAY_STORE = "31F17448AA3888B63ED04EB5F965E3F70C12592F"
    const val F_DROID = "412DABCABBDB75A82FF66F767C71EE045C02275B"
    const val GITHUB = "0E7BE3FA6B47C20394517C568570E10761A0A4FA"
}

object APP_LINK {
    const val PLAY_STORE =
        "https://play.google.com/store/apps/details?id=com.github.livingwithhippos.unchained"
    const val F_DROID = "https://f-droid.org/packages/com.github.livingwithhippos.unchained/"
    const val GITHUB = "https://github.com/LivingWithHippos/unchained-android/releases"
}

object PreferenceKeys {
    // todo: move all keys here
    object DownloadManager {
        const val KEY = "download_manager"
        const val SYSTEM = "download_manager_system"
        const val OKHTTP = "download_manager_okhttp"
        const val UNMETERED_ONLY_KEY = "download_only_on_unmetered"
        const val VIBRATE_ON_FINISH = "vibrate_on_download"
    }

    object Ui {
        const val SEARCH_START_DESTINATION_KEY = "search_start_destination"
        const val CUSTOM_THEME_SEED_COLOR_KEY = "custom_theme_seed_color"

        object SearchStartDestination {
            const val FILES = "files"
            const val PLUGINS = "plugins"
        }
    }

    object Kodi {
        const val SERVER_PICKER = "select_kodi_on_play"
    }
}

/** Used to map file extension and their icon */
val extensionIconMap: Map<String, Int> =
    mapOf(
        // this will be used as default value if no extension is recognized
        "default" to R.drawable.icon_file,
        // ARCHIVES
        "zip" to R.drawable.icon_archive,
        "rar" to R.drawable.icon_archive,
        "7z" to R.drawable.icon_archive,
        "tar" to R.drawable.icon_archive,
        "gz" to R.drawable.icon_archive,
        "arj" to R.drawable.icon_archive,
        "deb" to R.drawable.icon_archive,
        "pkg" to R.drawable.icon_archive,
        "rpm" to R.drawable.icon_archive,
        // AUDIO
        "aif" to R.drawable.icon_audio,
        "cda" to R.drawable.icon_audio,
        "mid" to R.drawable.icon_audio,
        "midi" to R.drawable.icon_audio,
        "mp3" to R.drawable.icon_audio,
        "mpa" to R.drawable.icon_audio,
        "ogg" to R.drawable.icon_audio,
        "wav" to R.drawable.icon_audio,
        "wma" to R.drawable.icon_audio,
        "wpl" to R.drawable.icon_audio,
        // PICTURES
        "ai" to R.drawable.icon_image,
        "bmp" to R.drawable.icon_image,
        "gif" to R.drawable.icon_image,
        "ico" to R.drawable.icon_image,
        "jpeg" to R.drawable.icon_image,
        "jpg" to R.drawable.icon_image,
        "png" to R.drawable.icon_image,
        "psd" to R.drawable.icon_image,
        "ps" to R.drawable.icon_image,
        "svg" to R.drawable.icon_image,
        "tiff" to R.drawable.icon_image,
        "tif" to R.drawable.icon_image,
        "raw" to R.drawable.icon_image,
        // VIDEOS
        "3g2" to R.drawable.icon_movie,
        "3gp" to R.drawable.icon_movie,
        "avi" to R.drawable.icon_movie,
        "flv" to R.drawable.icon_movie,
        "h264" to R.drawable.icon_movie,
        "m4v" to R.drawable.icon_movie,
        "mkv" to R.drawable.icon_movie,
        "mov" to R.drawable.icon_movie,
        "mp4" to R.drawable.icon_movie,
        "mpg" to R.drawable.icon_movie,
        "mpeg" to R.drawable.icon_movie,
        "rm" to R.drawable.icon_movie,
        "swf" to R.drawable.icon_movie,
        "vob" to R.drawable.icon_movie,
        "wmv" to R.drawable.icon_movie,
        // CAPTIONS
        "srt" to R.drawable.icon_subtitles,
        "scc" to R.drawable.icon_subtitles,
        "vtt" to R.drawable.icon_subtitles,
        "itt" to R.drawable.icon_subtitles,
        "smi" to R.drawable.icon_subtitles,
        "sami" to R.drawable.icon_subtitles,
        "sbv" to R.drawable.icon_subtitles,
        "aaf" to R.drawable.icon_subtitles,
        "mcc" to R.drawable.icon_subtitles,
        "mxf" to R.drawable.icon_subtitles,
        "asc" to R.drawable.icon_subtitles,
        "stl" to R.drawable.icon_subtitles,
        "scr" to R.drawable.icon_subtitles,
        "sub" to R.drawable.icon_subtitles,
        "idx" to R.drawable.icon_subtitles,
    )
