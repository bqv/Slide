package ltd.ucode.slide

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ltd.ucode.slide.App.Companion.forceRestart
import ltd.ucode.slide.App.Companion.setDefaultErrorHandler
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.NetworkUtil
import net.dean.jraw.RedditClient
import net.dean.jraw.http.LoggingMode
import net.dean.jraw.http.NetworkException
import net.dean.jraw.http.OkHttpAdapter
import net.dean.jraw.http.oauth.Credentials
import net.dean.jraw.http.oauth.OAuthData
import net.dean.jraw.models.LoggedInAccount
import okhttp3.Protocol
import java.util.Calendar
import java.util.UUID

class Authentication(context: Context?) {
    var hasDone = false
    fun updateToken(c: Context) {
        if (BuildConfig.DEBUG) LogUtil.v("Executing update token")
        if (reddit == null) {
            hasDone = true
            isLoggedIn = false
            //reddit = RedditClient(
            //    UserAgent.of("android:ltd.ucode.slide:v" + BuildConfig.VERSION_NAME)
            //)
            reddit!!.loggingMode = LoggingMode.ALWAYS
            didOnline = true
            VerifyCredentials(c).execute()
        } else {
            UpdateToken(c).execute()
        }
    }

    init {
        setDefaultErrorHandler(context)
        if (NetworkUtil.isConnected(context)) {
            hasDone = true
            httpAdapter = OkHttpAdapter(App.client, Protocol.HTTP_2)
            isLoggedIn = false
            //reddit = RedditClient(
            //    UserAgent.of("android:ltd.ucode.slide:v" + BuildConfig.VERSION_NAME),
            //    httpAdapter
            //)
            reddit!!.retryLimit = 2
            if (BuildConfig.DEBUG) reddit!!.loggingMode = LoggingMode.ALWAYS
            didOnline = true
            VerifyCredentials(context).execute()
        } else {
            isLoggedIn = SettingValues.appRestart.getBoolean("loggedin", false)
            name = SettingValues.appRestart.getString("name", "")
            if ((name!!.isEmpty() || !isLoggedIn) && !SettingValues.authentication.getString("lasttoken", "")!!
                    .isEmpty()
            ) {
                for (s in SettingValues.authentication.getStringSet(
                    "accounts",
                    HashSet()
                )!!) {
                    if (s.contains(SettingValues.authentication.getString("lasttoken", "")!!)) {
                        name = s.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        break
                    }
                }
                isLoggedIn = true
            }
        }
    }

    class UpdateToken(var context: Context) : AsyncTask<Void?, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Void?): Void? {
            if (authedOnce && NetworkUtil.isConnected(context)) {
                didOnline = true
                if (name != null && !name!!.isEmpty()) {
                    Log.v(LogUtil.getTag(), "REAUTH")
                    if (isLoggedIn) {
                        try {
                            val credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL)
                            Log.v(LogUtil.getTag(), "REAUTH LOGGED IN")
                            val oAuthHelper = reddit!!.oAuthHelper
                            oAuthHelper.refreshToken = refresh
                            val finalData: OAuthData
                            if (SettingValues.authentication.contains("backedCreds")
                                && SettingValues.authentication.getLong("expires", 0) > Calendar.getInstance()
                                    .timeInMillis
                            ) {
                                finalData = oAuthHelper.refreshToken(
                                    credentials,
                                    SettingValues.authentication.getString(
                                        "backedCreds",
                                        ""
                                    )
                                ) //does a request
                            } else {
                                finalData = oAuthHelper.refreshToken(credentials) //does a request
                                SettingValues.authentication.edit()
                                    .putLong(
                                        "expires",
                                        Calendar.getInstance().timeInMillis + 3000000
                                    )
                                    .commit()
                            }
                            SettingValues.authentication.edit()
                                .putString("backedCreds", finalData.dataNode.toString())
                                .commit()
                            reddit!!.authenticate(finalData)
                            refresh = oAuthHelper.refreshToken
                            refresh = reddit!!.oAuthHelper.refreshToken
                            if (reddit!!.isAuthenticated) {
                                if (me == null) {
                                    me = reddit!!.me()
                                }
                                isLoggedIn = true
                            }
                            Log.v(LogUtil.getTag(), "AUTHENTICATED")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val fcreds = Credentials.userlessApp(CLIENT_ID, UUID.randomUUID())
                        val authData: OAuthData
                        if (BuildConfig.DEBUG) LogUtil.v("Not logged in")
                        try {
                            authData = reddit!!.oAuthHelper.easyAuth(fcreds)
                            SettingValues.authentication.edit()
                                .putLong(
                                    "expires",
                                    Calendar.getInstance().timeInMillis + 3000000
                                )
                                .commit()
                            SettingValues.authentication.edit()
                                .putString("backedCreds", authData.dataNode.toString())
                                .commit()
                            name = "LOGGEDOUT"
                            mod = false
                            reddit!!.authenticate(authData)
                            Log.v(LogUtil.getTag(), "REAUTH LOGGED IN")
                        } catch (e: Exception) {
                            try {
                                (context as Activity).runOnUiThread {
                                    try {
                                        AlertDialog.Builder(context)
                                            .setTitle(R.string.err_general)
                                            .setMessage(R.string.err_no_connection)
                                            .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                                                UpdateToken(context)
                                                    .executeOnExecutor(THREAD_POOL_EXECUTOR)
                                            }
                                            .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, which: Int ->
                                                forceRestart(
                                                    context,
                                                    false
                                                )
                                            }
                                            .show()
                                    } catch (ignored: Exception) {
                                    }
                                }
                            } catch (e2: Exception) {
                                Toast.makeText(
                                    context,
                                    "Reddit could not be reached. Try again soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            //TODO fail
                        }
                    }
                }
            }
            if (BuildConfig.DEBUG) LogUtil.v("Done loading token")
            return null
        }
    }

    class VerifyCredentials(var mContext: Context?) : AsyncTask<String?, Void?, Void?>() {
        var lastToken: String?
        var single = false

        init {
            lastToken = SettingValues.authentication.getString("lasttoken", "")
        }

        protected override fun doInBackground(vararg subs: String?): Void? {
            doVerify(lastToken, reddit, single, mContext)
            return null
        }
    }

    companion object {
        private const val CLIENT_ID = ""
        private const val REDIRECT_URL = "http://slide.ucode.ltd"
        @JvmField
        var isLoggedIn = false
        @JvmField
        var reddit: RedditClient? = null
        @JvmField
        var me: LoggedInAccount? = null
        @JvmField
        var mod = false
        @JvmField
        var name: String? = null
        @JvmField
        var refresh: String? = null
        @JvmField
        var didOnline = false
        private var httpAdapter: OkHttpAdapter? = null
        fun resetAdapter() {
            object : AsyncTask<Void?, Void?, Void?>() {
                protected override fun doInBackground(vararg params: Void?): Void? {
                    if (httpAdapter != null && httpAdapter!!.nativeClient != null) {
                        httpAdapter!!.nativeClient.connectionPool().evictAll()
                    }
                    return null
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        var authedOnce = false
        @JvmStatic
        fun doVerify(
            lastToken: String?,
            baseReddit: RedditClient?,
            single: Boolean,
            mContext: Context?
        ) {
            try {
                if (BuildConfig.DEBUG) LogUtil.v("TOKEN IS $lastToken")
                if (!lastToken!!.isEmpty()) {
                    val credentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL)
                    val oAuthHelper = baseReddit!!.oAuthHelper
                    oAuthHelper.refreshToken = lastToken
                    try {
                        val finalData: OAuthData
                        if ((!single
                                    && SettingValues.authentication.contains("backedCreds")) && SettingValues.authentication.getLong(
                                "expires",
                                0
                            ) > Calendar.getInstance()
                                .timeInMillis
                        ) {
                            finalData = oAuthHelper.refreshToken(
                                credentials,
                                SettingValues.authentication.getString("backedCreds", "")
                            )
                        } else {
                            finalData = oAuthHelper.refreshToken(credentials) //does a request
                            if (!single) {
                                SettingValues.authentication.edit()
                                    .putLong(
                                        "expires",
                                        Calendar.getInstance().timeInMillis + 3000000
                                    )
                                    .apply()
                            }
                        }
                        baseReddit.authenticate(finalData)
                        if (!single) {
                            SettingValues.authentication.edit()
                                .putString("backedCreds", finalData.dataNode.toString())
                                .apply()
                            refresh = oAuthHelper.refreshToken
                            if (BuildConfig.DEBUG) {
                                LogUtil.v("ACCESS TOKEN IS " + finalData.accessToken)
                            }
                            isLoggedIn = true
                            UserSubscriptions.doCachedModSubs()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is NetworkException) {
                            Toast.makeText(
                                mContext, "Error " + e.response
                                    .statusMessage + ": " + e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    didOnline = true
                } else if (!single) {
                    if (BuildConfig.DEBUG) LogUtil.v("NOT LOGGED IN")
                    val fcreds = Credentials.userlessApp(CLIENT_ID, UUID.randomUUID())
                    val authData: OAuthData
                    try {
                        authData = reddit!!.oAuthHelper.easyAuth(fcreds)
                        SettingValues.authentication.edit()
                            .putLong(
                                "expires",
                                Calendar.getInstance().timeInMillis + 3000000
                            )
                            .apply()
                        SettingValues.authentication.edit()
                            .putString("backedCreds", authData.dataNode.toString())
                            .apply()
                        reddit!!.authenticate(authData)
                        name = "LOGGEDOUT"
                        App.notFirst = true
                        didOnline = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is NetworkException) {
                            Toast.makeText(
                                mContext, "Error " + e.response
                                    .statusMessage + ": " + e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                if (!single) authedOnce = true
            } catch (e: Exception) {
                //TODO fail
            }
        }
    }
}
