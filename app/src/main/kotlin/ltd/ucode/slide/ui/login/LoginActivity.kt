package ltd.ucode.slide.ui.login

import android.annotation.TargetApi
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.slide.App
import ltd.ucode.slide.App.Companion.forceRestart
import ltd.ucode.slide.R
import ltd.ucode.slide.databinding.ActivityLoginBinding
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserSubscriptions.setSubscriptions
import me.ccrama.redditslide.UserSubscriptions.sort
import me.ccrama.redditslide.Visuals.GetClosestColor
import me.ccrama.redditslide.Visuals.Palette
import net.dean.jraw.models.Subreddit

@AndroidEntryPoint
class LoginActivity : BaseActivityAnim() {
    private val logger: KLogger = KotlinLogging.logger {}
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
        binding.loginUsername.addTextChangedListener {
            viewModel.updateUsername(it.toString())
        }

        binding.loginInstance.apply {
            this@LoginActivity.adapter = LoginAdapter(this@LoginActivity)

            threshold = 1
            setAdapter(this@LoginActivity.adapter)

            addTextChangedListener {
                viewModel.updateInstance(it.toString())
            }
        }

        binding.loginPassword.addTextChangedListener {
            viewModel.updatePassword(it.toString())
        }

        binding.loginTotpChip.apply {
            isChecked = false
            setOnClickListener {
                if (binding.loginTotpChip.isChecked) {
                    binding.loginTotpLayout.visibility = View.VISIBLE
                    binding.loginTotp.focusOtpInput()
                } else {
                    binding.loginTotpLayout.visibility = View.GONE
                    binding.loginTotp.reset()
                }
            }
            callOnClick()
        }

        binding.loginTotp.inputChangedListener { inputComplete, it ->
            if (inputComplete)
                viewModel.updateToken(it)
            else
                viewModel.clearToken()
        }

        binding.loginButton.setOnClickListener {
            binding.progress.visibility = View.VISIBLE
            viewModel.doLogin(onSuccess = {
                UserSubscriptions.switchAccounts()
                App.forceRestart(this, true)
                finish()
            }, onFailure = {
                binding.progress.visibility = View.INVISIBLE
            })
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
        val webView = findViewById<WebView>(R.id.web)
            .apply {
                clearCache(true)
                clearHistory()
            }
        val webSettings = webView.settings
            .apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                minimumFontSize = 1
                minimumLogicalFontSize = 1
            }
        val cookieManager = CookieManager.getInstance()
            .apply {
                removeAllCookies(null)
                flush()
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
            AlertDialog.Builder(this@LoginActivity)
                .setTitle(R.string.login_subscribe_rslideforreddit)
                .setMessage(R.string.login_subscribe_rslideforreddit_desc)
                .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                    subNames!!.add(2, "slideforreddit")
                    setSubscriptions(subNames)
                    forceRestart(this@LoginActivity, true)
                }
                .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int ->
                    setSubscriptions(subNames)
                    forceRestart(this@LoginActivity, true)
                }
                .setCancelable(false)
                .show()
        } else {
            setSubscriptions(subNames)
            forceRestart(this@LoginActivity, true)
        }
    }

    fun doLastStuff(subs: ArrayList<Subreddit>) {
        d!!.dismiss()
        AlertDialog.Builder(this@LoginActivity)
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
                                this@LoginActivity
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
}
