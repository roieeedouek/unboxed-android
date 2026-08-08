package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.view.CUSTOM_THEME_KEY
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_AUTO
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_DAY
import com.github.livingwithhippos.unchained.settings.view.ThemeItem
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.google.android.material.color.DynamicColors
import java.util.Locale
import timber.log.Timber

/**
 * Provides the list of available themes, used to easily get them with ids from anything with a
 * Context
 */
fun Context.getThemeList(): List<ThemeItem> {
    val staticThemes =
        listOf(
        ThemeItem(
            "Waves",
            "waves_01",
            THEME_DAY,
            R.style.Theme_Unchained_Material3_Waves_One,
            ResourcesCompat.getColor(resources, R.color.radical_red, null),
            ResourcesCompat.getColor(resources, R.color.waves_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.waves_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Black and White",
            "bnw_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_BnW_One,
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Red",
            "red_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Red_One,
            ResourcesCompat.getColor(resources, R.color.red_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.red_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.red_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Orange",
            "orange_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Orange_One,
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Yellow",
            "yellow_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Yellow_One,
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Purple",
            "purple_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Purple_One,
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Green",
            "green_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Green_One,
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.green_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Blue",
            "blue_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Blue_One,
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Grey",
            "grey_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Grey_One,
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_primaryContainer, null),
        ),
    )
    return if (DynamicColors.isDynamicColorAvailable()) {
        staticThemes + getDynamicWallpaperThemeItem() + getCustomThemeItem()
    } else {
        staticThemes
    }
}

/**
 * The "Material You" theme entry: colors come from the device wallpaper via Android's dynamic
 * color system instead of a fixed palette, so its preview swatch is only a best-effort
 * approximation of the current wallpaper colors, not the exact colors DynamicColors will apply
 */
private fun Context.getDynamicWallpaperThemeItem(): ThemeItem {
    val isNight =
        resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    // system_accent1/2 tonal ramps are always available once dynamic color itself is available
    val primaryTone = if (isNight) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
    val surfaceTone = if (isNight) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_10
    val containerTone = if (isNight) android.R.color.system_accent2_700 else android.R.color.system_accent2_100
    return ThemeItem(
        name = "Material You",
        key = "dynamic_wallpaper",
        nightMode = THEME_AUTO,
        themeID = R.style.Theme_Unchained_Material3_Dynamic,
        primaryColorID = ResourcesCompat.getColor(resources, primaryTone, null),
        surfaceColorID = ResourcesCompat.getColor(resources, surfaceTone, null),
        primaryContainerColorID = ResourcesCompat.getColor(resources, containerTone, null),
        isDynamic = true,
    )
}

/**
 * The "Custom" theme entry: colors are generated from a user-picked seed color instead of a
 * fixed palette or the wallpaper. The preview swatch approximates the generated palette by
 * blending the seed toward white, since the real palette only exists once DynamicColors applies
 * it to an actual activity.
 */
private fun Context.getCustomThemeItem(): ThemeItem {
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val seedColor =
        preferences.getInt(
            PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY,
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primary, null),
        )
    return ThemeItem(
        name = "Custom",
        key = CUSTOM_THEME_KEY,
        nightMode = THEME_AUTO,
        themeID = R.style.Theme_Unchained_Material3_DynamicCustom,
        primaryColorID = seedColor,
        surfaceColorID = ColorUtils.blendARGB(seedColor, Color.WHITE, 0.9f),
        primaryContainerColorID = ColorUtils.blendARGB(seedColor, Color.WHITE, 0.7f),
        isDynamic = true,
    )
}

/**
 * Show a toast message
 *
 * @param stringResource: the string resource to be retrieved and shown
 * @param length How long to display the message. Either {@link #LENGTH_SHORT} or
 *   {@link #LENGTH_LONG} Defaults to short
 */
fun Context.showToast(stringResource: Int, length: Int = Toast.LENGTH_SHORT) =
    this.showToast(getString(stringResource, length))

/**
 * Show a toast message
 *
 * @param message the message and shown
 * @param length the duration of the toast. Defaults to short
 */
fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

/**
 * Return the int value of the color of a certain attribute for the current theme
 *
 * @param attributeID: the attribute id, like R.attr.colorAccent
 * @return the int value of the color
 */
fun Context.getThemeColor(@AttrRes attributeID: Int): Int {
    // get a reference to the current theme
    val typedValue = TypedValue()
    val theme: Resources.Theme = this.theme
    theme.resolveAttribute(attributeID, typedValue, true)
    return typedValue.data
}

/**
 * Sets the status and navigation bar icon colors (light or dark) to match the theme actually
 * applied to this activity right now, instead of relying on a static day/night assumption. Themes
 * don't necessarily get darker in night mode (none of them currently have night-specific colors)
 * and dynamic color themes aren't knowable ahead of time at all, so the only way to get readable
 * system icons for every theme, in every mode, is to check the real colors after they're applied.
 * See #315.
 */
fun Activity.applyThemeAwareSystemBarIconColors() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.isAppearanceLightStatusBars =
        ColorUtils.calculateLuminance(getThemeColor(android.R.attr.colorPrimary)) > 0.5
    controller.isAppearanceLightNavigationBars =
        ColorUtils.calculateLuminance(getThemeColor(com.google.android.material.R.attr.colorSurface)) >
            0.5
}

/**
 * Returns a Drawable from its id with the tint color of the current theme
 *
 * @param id the Drawable id
 * @return the themed Drawable
 */
fun Context.getThemedDrawable(@DrawableRes id: Int): Drawable {
    return ResourcesCompat.getDrawable(resources, id, this.theme)
        ?: throw IllegalArgumentException("Drawable with id $id was missing")
}

// todo: verify if these can extend context and not fragment

/**
 * Copy some text on the clipboard
 *
 * @param label: the label of the text copied
 * @param text: the text to be copied to clipboard
 */
fun Fragment.copyToClipboard(label: String, text: String) {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData = ClipData.newPlainText(label, text)
    // Set the clipboard's primary clip.
    clipboard.setPrimaryClip(clip)
}

/**
 * Get the text from the clipboard
 *
 * @return the text on the clipboard or "" if empty or not text
 */
fun Fragment.getClipboardText(): String {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var text = ""
    if (
        clipboard.hasPrimaryClip() &&
            (clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true ||
                clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_HTML) == true)
    ) {
        val item = clipboard.primaryClip!!.getItemAt(0)
        text = item.text.toString()
    } else {
        Timber.d(
            "Clipboard was empty or did not contain any text mime type: ${clipboard.primaryClipDescription}"
        )
    }
    return text
}

// todo: move extensions to own file base on dependencies, for example for these ones Either is
// needed
/**
 * Download a file in the public download folder
 *
 * @param source the file Uri
 * @param title the title to show on the notification
 * @param description the title to show on the notification
 * @param fileName the name to give to the downloaded file, title will be used if this is null
 * @return a Long identifying the download or null if an error has occurred
 */
fun DownloadManager.downloadFileInStandardFolder(
    source: Uri,
    title: String,
    description: String,
    fileName: String = title,
): EitherResult<Exception, Long> {
    return try {
        val request: DownloadManager.Request =
            DownloadManager.Request(source)
                .setTitle(title)
                .setDescription(description)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadID = this.enqueue(request)
        EitherResult.Success(downloadID)
    } catch (e: Exception) {
        Timber.e("Error starting download of ${source.path}, exception ${e.message}")
        EitherResult.Failure(e)
    }
}

/**
 * Return the Uri from a downloaded file id returned by the download manager
 *
 * @param id the file id
 * @return the file Uri or null if the id wasn't found or the download wasn't successful
 */
@SuppressLint("Range")
fun Context.getDownloadedFileUri(id: Long): Uri? {
    val manager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val cursor = manager.query(DownloadManager.Query().setFilterById(id))
    if (cursor.moveToFirst()) {
        val columnIndex: Int = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        if (cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL)
            return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri()
    }
    cursor.close()
    return null
}

@SuppressLint("Range")
fun Uri.getFileName(context: Context): String {
    var fileName = ""
    when (this.scheme) {
        SCHEME_CONTENT -> {
            val cursor: Cursor? = context.contentResolver.query(this, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
            }
        }
        else -> fileName = this.lastPathSegment ?: ""
    }
    return fileName
}

/**
 * Open an url from available apps
 *
 * @param url: the url to be opened
 * @param showErrorToast: set to true if an error toast should be displayed
 * @return true if the passed url is correct, false otherwise
 */
fun Context.openExternalWebPage(url: String, showErrorToast: Boolean = true): Boolean {
    // todo: check if app supporting this index are available, otherwise
    // android.content.ActivityNotFoundException can be thrown by this
    // this pattern accepts everything that is something.tld since there were too many new tlds and
    // Google gave up updating their regex
    if (url.isWebUrl()) {
        try {
            val webIntent =
                Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(webIntent)
        } catch (ex: android.content.ActivityNotFoundException) {
            Timber.e("Error opening externally a link ${ex.message}")
            showToast(R.string.browser_not_found, length = Toast.LENGTH_LONG)
        } catch (ex: SecurityException) {
            // the default app has marked itself as available to open these links
            // but does not have exported=true in its manifest activity
            Timber.e("Bugged app cannot receive external links ${ex.message}")
            showToast(R.string.invalid_player_found, length = Toast.LENGTH_LONG)
        }
        return true
    } else if (showErrorToast) showToast(R.string.invalid_url)

    return false
}

/**
 * this function can be used to create a new context with a particular locale. It must be used while
 * overriding Activity.attachBaseContext like this: override fun attachBaseContext(base: Context?) {
 * if (base != null) super.attachBaseContext(getUpdatedLocaleContext(base, "en")) else
 * super.attachBaseContext(null) } it must be applied to all the activities or added to a
 * BaseActivity extended by them
 */
fun Activity.getUpdatedLocaleContext(context: Context, language: String): Context {
    val locale = Locale.forLanguageTag(language)
    val configuration = Configuration(context.resources.configuration)
    // check if this is necessary
    Locale.setDefault(locale)
    configuration.setLocale(locale)
    return context.createConfigurationContext(configuration)
}

fun AppCompatActivity.setNavigationBarColor(color: Int, alpha: Int = 0) {
    val newColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    // set the color before applying the light bar effect
    window.navigationBarColor = newColor

    val luminance = Color.luminance(color)
    if (luminance >= 0.25) {
        when {
            SDK_INT >= Build.VERSION_CODES.R -> {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            }
            else -> {
                // the check above is not recognized
                @Suppress("DEPRECATION") @SuppressLint("InlinedApi")
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    } else
        @Suppress("DEPRECATION") @SuppressLint("InlinedApi")
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
}

/**
 * Translates one of TorBox's string error codes (the `error` field of its standard response
 * envelope) into a user-facing message. Falls back to the given server-provided [detail] message
 * (documented by TorBox as always being user-safe) for the long tail of less common codes, rather
 * than hand-translating all of them into every supported locale.
 */
fun Context.getApiErrorMessage(errorCode: String?, detail: String? = null): String {
    return when (errorCode) {
        "NO_AUTH" -> getString(R.string.error_no_auth)
        "BAD_TOKEN" -> getString(R.string.error_bad_token)
        "AUTH_ERROR" -> getString(R.string.error_auth_error)
        "INVALID_OPTION" -> getString(R.string.error_invalid_option)
        "ITEM_NOT_FOUND" -> getString(R.string.error_item_not_found)
        "MISSING_REQUIRED_OPTION" -> getString(R.string.error_missing_required_option)
        "TOO_MANY_OPTIONS" -> getString(R.string.error_too_many_options)
        "BOZO_TORRENT" -> getString(R.string.error_bozo_torrent)
        "TOO_MUCH_DATA" -> getString(R.string.error_too_much_data)
        "DOWNLOAD_TOO_LARGE" -> getString(R.string.error_download_too_large)
        "MONTHLY_LIMIT" -> getString(R.string.error_monthly_limit)
        "COOLDOWN_LIMIT" -> getString(R.string.error_cooldown_limit)
        "ACTIVE_LIMIT" -> getString(R.string.error_active_limit)
        "PLAN_RESTRICTED_FEATURE" -> getString(R.string.error_plan_restricted_feature)
        "DUPLICATE_ITEM" -> getString(R.string.error_duplicate_item)
        "UNSUPPORTED_SITE" -> getString(R.string.error_unsupported_site)
        "TEMPORARILY_DISABLED" -> getString(R.string.error_temporarily_disabled)
        "DATABASE_ERROR" -> getString(R.string.error_database_error)
        "NOT_OWNER" -> getString(R.string.error_not_owner)
        else -> detail?.takeIf { it.isNotBlank() } ?: getString(R.string.unknown_error)
    }
}

/** Translates one of TorBox's `download_state` values into a user-facing label. */
fun Context.getStatusTranslation(status: String): String {
    return when (status) {
        "queued" -> getString(R.string.queued)
        "downloading" -> getString(R.string.downloading)
        "uploading" -> getString(R.string.uploading)
        "completed" -> getString(R.string.status_completed)
        "cached" -> getString(R.string.status_cached)
        "paused" -> getString(R.string.status_paused)
        "stalled (no seeds)" -> getString(R.string.status_stalled)
        "metaDL" -> getString(R.string.status_meta_dl)
        "checkingResumeData" -> getString(R.string.status_checking_resume_data)
        "error" -> getString(R.string.error)
        else -> status
    }
}

@Suppress("DEPRECATION")
fun Context.vibrate(duration: Long = 200) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
}

/** AssetManager extensions */

/**
 * This function returns the list of files and folder found in a path of the assets folder, it
 * removes the "/" at the end and checks again if no files are found.
 */
fun AssetManager.smartList(path: String): Array<String>? {
    val result = this.list(path)
    if (result.isNullOrEmpty()) if (path.endsWith("/")) return this.list(path.dropLast(1))
    return result
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 34 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 34 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}
