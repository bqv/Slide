package me.ccrama.redditslide.Activities

import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.util.LogUtil
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.http.oauth.OAuthData
import net.dean.jraw.http.oauth.OAuthException
import net.dean.jraw.http.oauth.OAuthHelper
import net.dean.jraw.models.LoggedInAccount

class Reauthenticate constructor() : BaseActivityAnim() {
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        applyColorTheme("")
        setContentView(R.layout.activity_login)
        setupAppBar(R.id.toolbar, "Re-authenticate", enableUpButton = true, colorToolbar = true)
        val scopes: Array<String> = arrayOf(
            "identity", "modcontributors", "modconfig", "modothers", "modwiki", "creddits",
            "livemanage", "account", "privatemessages", "modflair", "modlog", "report",
            "modposts", "modwiki", "read", "vote", "edit", "submit", "subscribe", "save",
            "wikiread", "flair", "history", "mysubreddits", "wikiedit"
        )
        val oAuthHelper: OAuthHelper = Authentication.reddit!!.oAuthHelper
        val credentials: Credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL)
        var authorizationUrl: String = oAuthHelper.getAuthorizationUrl(credentials, true, *scopes)
            .toExternalForm()
        authorizationUrl = authorizationUrl.replace("www.", "i.")
        authorizationUrl = authorizationUrl.replace("%3A%2F%2Fi", "://www")
        Log.v(LogUtil.getTag(), "Auth URL: $authorizationUrl")
        val cookieManager: CookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
        } else {
            cookieManager.removeAllCookie()
        }
        val webView: WebView = findViewById<View>(R.id.web) as WebView
        webView.loadUrl(authorizationUrl)
        webView.webChromeClient = object : WebChromeClient() {
            public override fun onProgressChanged(view: WebView, newProgress: Int) {
    //                activity.setProgress(newProgress * 1000);
            }
        }
        webView.webViewClient = object : WebViewClient() {
            public override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                if (url.contains("code=")) {
                    Log.v(LogUtil.getTag(), "WebView URL: $url")
                    // Authentication code received, prevent HTTP call from being made.
                    webView.stopLoading()
                    UserChallengeTask(oAuthHelper, credentials).execute(url)
                    webView.visibility = View.GONE
                    webView.clearCache(true)
                    webView.clearHistory()
                }
            }
        }
    }

    private inner class UserChallengeTask constructor(
        oAuthHelper: OAuthHelper,
        credentials: Credentials
    ) : AsyncTask<String?, Void?, OAuthData?>() {
        private val mOAuthHelper: OAuthHelper
        private val mCredentials: Credentials
        private var mMaterialDialog: MaterialDialog? = null

        init {
            Log.v(LogUtil.getTag(), "UserChallengeTask()")
            mOAuthHelper = oAuthHelper
            mCredentials = credentials
        }

        override fun onPreExecute() {
            //Show a dialog to indicate progress
            mMaterialDialog = MaterialDialog(this@Reauthenticate)
                .title(R.string.login_authenticating)
                //.progress(true, 0)
                .message(R.string.misc_please_wait)
                .cancelable(false)
            mMaterialDialog!!.show()
        }

        override fun doInBackground(vararg params: String?): OAuthData? {
            try {
                val oAuthData: OAuthData? =
                    mOAuthHelper.onUserChallenge(params[0], mCredentials)
                if (oAuthData != null) {
                    Authentication.reddit!!.authenticate(oAuthData)
                    Authentication.isLoggedIn = true
                    val refreshToken: String =
                        Authentication.reddit.oAuthData.refreshToken
                    val editor: SharedPreferences.Editor = authentication.edit()
                    val accounts: MutableSet<String>? =
                        authentication.getStringSet("accounts", HashSet())
                    val me: LoggedInAccount = Authentication.reddit.me()
                    var toRemove: String = ""
                    for (s: String in accounts!!) {
                        if (s.contains(me.getFullName())) {
                            toRemove = s
                        }
                    }
                    if (!toRemove.isEmpty()) accounts.remove(toRemove)
                    accounts.add(me.getFullName() + ":" + refreshToken)
                    Authentication.name = me.getFullName()
                    editor.putStringSet("accounts", accounts)
                    val tokens: MutableSet<String>? =
                        authentication.getStringSet("tokens", HashSet())
                    tokens!!.add(refreshToken)
                    editor.putStringSet("tokens", tokens)
                    editor.putString("lasttoken", refreshToken)
                    editor.remove("backedCreds")
                    appRestart.edit().remove("back").apply()
                    editor.apply()
                } else {
                    Log.e(LogUtil.getTag(), "Passed in OAuthData was null")
                }
                return oAuthData
            } catch (e: IllegalStateException) {
                // Handle me gracefully
                Log.e(LogUtil.getTag(), "OAuth failed")
                Log.e(LogUtil.getTag(), (e.message)!!)
            } catch (e: NetworkException) {
                Log.e(LogUtil.getTag(), "OAuth failed")
                Log.e(LogUtil.getTag(), (e.message)!!)
            } catch (e: OAuthException) {
                Log.e(LogUtil.getTag(), "OAuth failed")
                Log.e(LogUtil.getTag(), (e.message)!!)
            }
            return null
        }

        override fun onPostExecute(oAuthData: OAuthData?) {
            //Dismiss old progress dialog
            mMaterialDialog!!.dismiss()
            AlertDialog.Builder(this@Reauthenticate)
                .setTitle(R.string.reauth_complete)
                .setPositiveButton(
                    R.string.btn_ok,
                    DialogInterface.OnClickListener({ dialog: DialogInterface?, which: Int -> finish() })
                )
                .setCancelable(false)
                .setOnCancelListener(DialogInterface.OnCancelListener({ dialog: DialogInterface? -> finish() }))
                .show()
        }
    }

    companion object {
        private val CLIENT_ID: String = "KI2Nl9A_ouG9Qw"
        private val REDIRECT_URL: String = "http://www.ccrama.me"
    }
}
