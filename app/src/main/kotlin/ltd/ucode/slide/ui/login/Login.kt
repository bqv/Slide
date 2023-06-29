package ltd.ucode.slide.ui.login

import android.annotation.TargetApi
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import ltd.ucode.slide.App.Companion.forceRestart
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.databinding.ActivityLoginBinding
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.UserSubscriptions.setSubscriptions
import me.ccrama.redditslide.UserSubscriptions.sort
import me.ccrama.redditslide.UserSubscriptions.switchAccounts
import me.ccrama.redditslide.UserSubscriptions.syncSubredditsGetObjectAsync
import me.ccrama.redditslide.Visuals.GetClosestColor
import me.ccrama.redditslide.Visuals.Palette
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.http.oauth.OAuthData
import net.dean.jraw.http.oauth.OAuthException
import net.dean.jraw.http.oauth.OAuthHelper
import net.dean.jraw.models.Subreddit

@AndroidEntryPoint
class Login : BaseActivityAnim() {
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding
    private lateinit var adapter: LoginAdapter

    var d: Dialog? = null
    private var subNames: CaseInsensitiveArrayList? = null

    public override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstance)
        applyColorTheme(subreddit = "")
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            val view = binding.root
            setContentView(view)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
            return
        }
        setupAppBar(binding.toolbar.id, R.string.title_login, enableUpButton = true, colorToolbar = true)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        adapter = LoginAdapter(this)

        binding.loginInstance.apply {
            threshold = 1
            setAdapter(this@Login.adapter)
        }

        binding.loginUsername.addTextChangedListener {
            viewModel.updateUsername(it.toString())
        }

        binding.loginPassword.addTextChangedListener {
            viewModel.updatePassword(it.toString())
        }

        binding.loginInstance.addTextChangedListener {
            viewModel.updateInstance(it.toString())
        }

        binding.loginButton.setOnClickListener {
            val username = binding.loginUsername.text?.toString()
            val password = binding.loginPassword.text?.toString()

            android.widget.Toast.makeText(this,
                "Would login as $username:$password",
                android.widget.Toast.LENGTH_LONG)

            viewModel.doLogin()
        }
    }

    private fun setupObservers() {
        viewModel.instanceList.observe(this, Observer {
            renderInstanceList(it.keys)
        })
    }

    private fun renderInstanceList(instances: Set<String>) {
        adapter.clear()
        adapter.addData(instances.toList())
        adapter.notifyDataSetChanged()
    }

    fun setupWebView(savedInstance: Bundle?) {
        //if (Authentication.reddit == null) {
        //    Authentication(applicationContext)
        //}
        //val oAuthHelper = Authentication.reddit!!.oAuthHelper
        //val credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL)
        //var authorizationUrl = oAuthHelper.getAuthorizationUrl(credentials, true, *scopes).toExternalForm()
        //authorizationUrl = authorizationUrl.replace("www.", "i.")
        //authorizationUrl = authorizationUrl.replace("%3A%2F%2Fi", "://www")
        //Log.v(LogUtil.getTag(), "Auth URL: $authorizationUrl")
        val webView = findViewById<View>(R.id.web) as WebView
        webView.clearCache(true)
        webView.clearHistory()
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.minimumFontSize = 1
        webSettings.minimumLogicalFontSize = 1
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        } else {
            val cookieSyncMngr = CookieSyncManager.createInstance(this)
            cookieSyncMngr.startSync()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMngr.stopSync()
            cookieSyncMngr.sync()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                me.ccrama.redditslide.util.LogUtil.v(url)
                if (url.contains("code=")) {
                    Log.v(me.ccrama.redditslide.util.LogUtil.getTag(), "WebView URL: $url")
                    // Authentication code received, prevent HTTP call from being made.
                    webView.stopLoading()
                    //UserChallengeTask(oAuthHelper, credentials).execute(url)
                    webView.visibility = View.GONE
                }
            }
        }
        webView.loadUrl("about:blank")
    }

    @TargetApi(Build.VERSION_CODES.O)
    override fun setAutofill() {
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_AUTO
    }

    private fun doSubStrings(subs: ArrayList<Subreddit>) {
        subNames = CaseInsensitiveArrayList()
        for (s in subs) {
            subNames!!.add(s.displayName.lowercase())
        }
        subNames = sort(subNames)
        if (!subNames!!.contains("slideforreddit")) {
            AlertDialog.Builder(this@Login)
                .setTitle(R.string.login_subscribe_rslideforreddit)
                .setMessage(R.string.login_subscribe_rslideforreddit_desc)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    subNames!!.add(2, "slideforreddit")
                    setSubscriptions(subNames)
                    forceRestart(this@Login, true)
                }
                .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int ->
                    setSubscriptions(subNames)
                    forceRestart(this@Login, true)
                }
                .setCancelable(false)
                .show()
        } else {
            setSubscriptions(subNames)
            forceRestart(this@Login, true)
        }
    }

    fun doLastStuff(subs: ArrayList<Subreddit>) {
        d!!.dismiss()
        AlertDialog.Builder(this@Login)
            .setTitle(R.string.login_sync_colors)
            .setMessage(R.string.login_sync_colors_desc)
            .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                for (s in subs) {
                    if ((s.dataNode.has("key_color")
                                && !s.dataNode["key_color"]
                            .asText()
                            .isEmpty()) && Palette.getColor(s.displayName.lowercase()) == Palette
                            .getDefaultColor()
                    ) {
                        Palette.setColor(
                            s.displayName.lowercase(),
                            GetClosestColor.getClosestColor(
                                s.dataNode["key_color"].asText(),
                                this@Login
                            )
                        )
                    }
                }
                doSubStrings(subs)
            }
            .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int ->
                doSubStrings(
                    subs
                )
            }
            .setOnDismissListener { dialog: DialogInterface? -> doSubStrings(subs) }
            .create()
            .show()
    }

    private inner class UserChallengeTask(oAuthHelper: OAuthHelper, credentials: Credentials) :
        AsyncTask<String?, Void?, OAuthData?>() {
        private val mOAuthHelper: OAuthHelper
        private val mCredentials: Credentials
        private var mMaterialDialog: MaterialDialog? = null

        init {
            Log.v(me.ccrama.redditslide.util.LogUtil.getTag(), "UserChallengeTask()")
            mOAuthHelper = oAuthHelper
            mCredentials = credentials
        }

        override fun onPreExecute() {
            //Show a dialog to indicate progress
            val builder = MaterialDialog.Builder(this@Login).title(R.string.login_authenticating)
                .progress(true, 0)
                .content(R.string.misc_please_wait)
                .cancelable(false)
            mMaterialDialog = builder.build()
            mMaterialDialog!!.show()
        }

        override fun doInBackground(vararg params: String?): OAuthData? {
            try {
                val oAuthData = mOAuthHelper.onUserChallenge(params[0], mCredentials)
                if (oAuthData != null) {
                    Authentication.reddit!!.authenticate(oAuthData)
                    Authentication.isLoggedIn = true
                    val refreshToken = Authentication.reddit.oAuthData.refreshToken
                    val editor = authentication.edit()
                    val accounts = authentication.getStringSet(
                        "accounts",
                        HashSet()
                    )
                    val me = Authentication.reddit.me()
                    accounts!!.add(me.fullName + ":" + refreshToken)
                    Authentication.name = me.fullName
                    editor.putStringSet("accounts", accounts)
                    val tokens = authentication.getStringSet(
                        "tokens",
                        HashSet()
                    )
                    tokens!!.add(refreshToken)
                    editor.putStringSet("tokens", tokens)
                    editor.putString("lasttoken", refreshToken)
                    editor.remove("backedCreds")
                    appRestart.edit().remove("back").commit()
                    editor.commit()
                } else {
                    Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), "Passed in OAuthData was null")
                }
                return oAuthData
            } catch (e: IllegalStateException) {
                // Handle me gracefully
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), "OAuth failed")
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), e.message!!)
            } catch (e: NetworkException) {
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), "OAuth failed")
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), e.message!!)
            } catch (e: OAuthException) {
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), "OAuth failed")
                Log.e(me.ccrama.redditslide.util.LogUtil.getTag(), e.message!!)
            }
            return null
        }

        override fun onPostExecute(oAuthData: OAuthData?) {
            //Dismiss old progress dialog
            mMaterialDialog!!.dismiss()
            if (oAuthData != null) {
                appRestart.edit().putBoolean("firststarting", true).apply()
                switchAccounts()
                d = MaterialDialog.Builder(this@Login).cancelable(false)
                    .title(R.string.login_starting)
                    .progress(true, 0)
                    .content(R.string.login_starting_desc)
                    .build()
                d!!.show()
                syncSubredditsGetObjectAsync(this@Login)
            } else {
                //Show a dialog if data is null
                AlertDialog.Builder(this@Login)
                    .setTitle(R.string.err_authentication)
                    .setMessage(R.string.login_failed_err_decline)
                    .setNeutralButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                        forceRestart(this@Login, true)
                        finish()
                    }
                    .show()
            }
        }
    }

    companion object {
        private const val CLIENT_ID = ""
        private const val REDIRECT_URL = "about:blank"
    }
}
