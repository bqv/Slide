package me.ccrama.redditslide.util

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.appContext
import ltd.ucode.slide.Constants
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.SettingValues.isNight
import ltd.ucode.slide.SettingValues.linkHandlingMode
import ltd.ucode.slide.SettingValues.readerMode
import ltd.ucode.slide.SettingValues.selectedBrowser
import ltd.ucode.slide.data.IPost
import me.ccrama.redditslide.Activities.Crosspost
import me.ccrama.redditslide.Activities.MakeExternal
import me.ccrama.redditslide.Activities.ReaderMode
import me.ccrama.redditslide.Activities.Website
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.SubmissionViews.PopulateBase
import me.ccrama.redditslide.ui.settings.SettingsHandlingFragment.LinkHandlingMode
import org.apache.commons.text.StringEscapeUtils
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder

object LinkUtil {
    private var mCustomTabsSession: CustomTabsSession? = null
    private val mClient: CustomTabsClient? = null
    private val mConnection: CustomTabsServiceConnection? = null
    const val EXTRA_URL = "url"
    const val EXTRA_COLOR = "color"
    const val ADAPTER_POSITION = "adapter_position"

    /**
     * Attempts to open the `url` in a custom tab. If no custom tab activity can be found,
     * falls back to opening externally
     *
     * @param url             URL to open
     * @param color           Color to provide to the browser UI if applicable
     * @param contextActivity The current activity
     * @param packageName     The package name recommended to use for connecting to custom tabs
     * related components.
     */
    fun openCustomTab(
        url: String, color: Int,
        contextActivity: Activity, packageName: String
    ) {
        val intent = Intent(contextActivity, MakeExternal::class.java)
        intent.putExtra(EXTRA_URL, url)
        val pendingIntent =
            PendingIntent.getActivity(contextActivity, 0, intent, PendingIntent.FLAG_MUTABLE)
        val builder = CustomTabsIntent.Builder(session)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(color)
                    .build()
            )
            .setShowTitle(true)
            .setStartAnimations(contextActivity, R.anim.slide_up_fade_in, 0)
            .setExitAnimations(contextActivity, 0, R.anim.slide_down_fade_out)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .addMenuItem(
                contextActivity.getString(R.string.open_links_externally),
                pendingIntent
            )
            .setCloseButtonIcon(
                DrawableUtil.drawableToBitmap(
                    ContextCompat.getDrawable(
                        contextActivity,
                        R.drawable.ic_arrow_back
                    )
                )
            )
        try {
            val customTabsIntent = builder.build()
            customTabsIntent.intent.setPackage(packageName)
            customTabsIntent.launchUrl(
                contextActivity,
                formatURL(StringEscapeUtils.unescapeHtml4(url))
            )
        } catch (anfe: ActivityNotFoundException) {
            Log.w(LogUtil.getTag(), "Unknown url: $anfe")
            openExternally(url)
        }
    }

    /**
     * Opens the `url` using the method the user has set in their preferences (custom tabs,
     * internal, external) falling back as needed
     *
     * @param url             URL to open
     * @param color           Color to provide to the browser UI if applicable
     * @param contextActivity The current activity
     */
    @JvmOverloads
    @JvmStatic
    fun openUrl(
        url: String, color: Int, contextActivity: Activity,
        adapterPosition: Int? = null, submission: IPost? = null
    ) {
        if (contextActivity !is ReaderMode && ((readerMode
                    && !SettingValues.readerNight)
                    || (readerMode
                    && SettingValues.readerNight
                    && isNight))
        ) {
            val i = Intent(contextActivity, ReaderMode::class.java)
            openIntentThemed(i, url, color, contextActivity, adapterPosition, submission)
        } else if (linkHandlingMode == LinkHandlingMode.EXTERNAL.value) {
            openExternally(url)
        } else {
            val packageName = CustomTabsHelper.getPackageNameToUse(contextActivity)
            if (linkHandlingMode == LinkHandlingMode.CUSTOM_TABS.value
                && packageName != null
            ) {
                openCustomTab(url, color, contextActivity, packageName)
            } else {
                val i = Intent(contextActivity, Website::class.java)
                openIntentThemed(i, url, color, contextActivity, adapterPosition, submission)
            }
        }
    }

    private fun openIntentThemed(
        intent: Intent, url: String, color: Int,
        contextActivity: Activity, adapterPosition: Int?,
        submission: IPost?
    ) {
        intent.putExtra(EXTRA_URL, url)
        if (adapterPosition != null && submission != null) {
            PopulateBase.addAdaptorPosition(intent, submission, adapterPosition)
        }
        intent.putExtra(EXTRA_COLOR, color)
        contextActivity.startActivity(intent)
    }

    /**
     * Corrects mistakes users might make when typing URLs, e.g. case sensitivity in the scheme
     * and converts to Uri
     *
     * @param url URL to correct
     * @return corrected as a Uri
     */
    @JvmStatic
    fun formatURL(url: String): Uri {
        var url = url
        if (url.startsWith("//")) {
            url = "https:$url"
        }
        if (url.startsWith("/")) {
            url = "https://reddit.com$url"
        }
        if (!url.contains("://") && !url.startsWith("mailto:")) {
            url = "http://$url"
        }
        val uri = Uri.parse(url)
        return uri.normalizeScheme()
    }

    @JvmStatic
    fun tryOpenWithVideoPlugin(url: String): Boolean {
        return if (App.videoPlugin) {
            try {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.setClassName(
                    appContext.getString(R.string.youtube_plugin_package),
                    appContext.getString(R.string.youtube_plugin_class)
                )
                sharingIntent.putExtra("url", removeUnusedParameters(url))
                sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(sharingIntent)
                true
            } catch (ignored: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Opens the `uri` externally or shows an application chooser if it is set to open in this
     * application
     *
     * @param url     URL to open
     */
    @JvmStatic
    fun openExternally(url: String) {
        var url = url
        url = StringEscapeUtils.unescapeHtml4(CompatUtil.fromHtml(url).toString())
        val uri = formatURL(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        overridePackage(intent)
        appContext.startActivity(intent)
    }

    val session: CustomTabsSession?
        get() {
            if (mClient == null) {
                mCustomTabsSession = null
            } else if (mCustomTabsSession == null) {
                mCustomTabsSession = mClient.newSession(object : CustomTabsCallback() {
                    override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                        Log.w(LogUtil.getTag(), "onNavigationEvent: Code = $navigationEvent")
                    }
                })
            }
            return mCustomTabsSession
        }

    @JvmStatic
    fun copyUrl(url: String?, context: Context?) {
        var url = url
        url = StringEscapeUtils.unescapeHtml4(CompatUtil.fromHtml(url!!).toString())
        ClipboardUtil.copyToClipboard(context, "Link", url)
        Toast.makeText(context, R.string.submission_link_copied, Toast.LENGTH_SHORT).show()
    }

    fun crosspost(submission: IPost?, mContext: Activity) {
        Crosspost.toCrosspost = submission
        mContext.startActivity(Intent(mContext, Crosspost::class.java))
    }

    private fun overridePackage(intent: Intent) {
        val packageName = appContext
            .packageManager
            .resolveActivity(intent, 0)!!.activityInfo.packageName

        // Gets the default app from a URL that is most likely never link handled by another app, hopefully guaranteeing a browser
        val browserPackageName = appContext
            .packageManager
            .resolveActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TEST_URL)),
                0
            )!!.activityInfo.packageName
        var packageToSet = packageName
        if (packageName == appContext.packageName) {
            packageToSet = browserPackageName
        }
        if (packageToSet == browserPackageName && selectedBrowser.isNotEmpty()) {
            try {
                appContext
                    .packageManager
                    .getPackageInfo(
                        selectedBrowser,
                        PackageManager.GET_ACTIVITIES
                    )
                packageToSet = selectedBrowser
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        }
        if (packageToSet != packageName) {
            intent.setPackage(packageToSet)
        }
    }

    fun removeUnusedParameters(url: String): String {
        var returnUrl = url
        return try {
            val urlParts = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (urlParts.size > 1) {
                val paramArray =
                    urlParts[1].split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val stringBuilder = StringBuilder()
                stringBuilder.append(urlParts[0])
                for (i in paramArray.indices) {
                    val paramPairArray =
                        paramArray[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    if (paramPairArray.size > 1) {
                        if (i == 0) {
                            stringBuilder.append("?")
                        } else {
                            stringBuilder.append("&")
                        }
                        stringBuilder.append(URLDecoder.decode(paramPairArray[0], "UTF-8"))
                        stringBuilder.append("=")
                        stringBuilder.append(URLDecoder.decode(paramPairArray[1], "UTF-8"))
                    }
                }
                returnUrl = stringBuilder.toString()
            }
            returnUrl
        } catch (ignored: UnsupportedEncodingException) {
            returnUrl
        }
    }

    @JvmStatic
    fun setTextWithLinks(s: String, text: SpoilerRobotoTextView) {
        val parts = s.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val b = StringBuilder()
        for (item in parts) try {
            val url = URL(item)
            b.append(" <a href=\"").append(url).append("\">").append(url).append("</a>")
        } catch (e: MalformedURLException) {
            b.append(" ").append(item)
        }
        text.setTextHtml(b.toString(), "no sub")
    }

    @JvmStatic
    fun launchMarketUri(context: Context, @StringRes resId: Int) {
        try {
            launchMarketUriIntent(context, "market://details?id=", resId)
        } catch (anfe: ActivityNotFoundException) {
            launchMarketUriIntent(context, "http://play.google.com/store/apps/details?id=", resId)
        }
    }

    private fun launchMarketUriIntent(
        context: Context, uriString: String,
        @StringRes resId: Int
    ) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(uriString + context.getString(resId))
            )
        )
    }
}
