package me.ccrama.redditslide

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.widget.Toast
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.alphabetizeOnSubscribe
import ltd.ucode.slide.SettingValues.appRestart
import ltd.ucode.slide.SettingValues.authentication
import ltd.ucode.slide.activity.MainActivity
import me.ccrama.redditslide.Activities.Login
import me.ccrama.redditslide.Activities.MultiredditOverview
import me.ccrama.redditslide.Activities.NewsActivity
import me.ccrama.redditslide.Toolbox.Toolbox
import me.ccrama.redditslide.ui.settings.dragSort.ReorderSubreddits
import me.ccrama.redditslide.util.NetworkUtil
import me.ccrama.redditslide.util.StringUtil
import net.dean.jraw.ApiException
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.AccountManager
import net.dean.jraw.managers.MultiRedditManager
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.Subreddit
import net.dean.jraw.paginators.ImportantUserPaginator
import net.dean.jraw.paginators.UserSubredditsPaginator
import java.util.Collections

object UserSubscriptions {
    const val SUB_NAME_TO_PROPERTIES = "multiNameToSubs"
    val defaultSubs = listOf(
        "subscribed", "all", "local"
    )
    @JvmField
    val specialSubreddits = listOf(
        "subscribed", "local", "all"
    )
    @JvmField
    var subscriptions: SharedPreferences? = null
    var multiNameToSubs: SharedPreferences? = null
    var newsNameToSubs: SharedPreferences? = null
    var news: SharedPreferences? = null
    @JvmField
    var pinned: SharedPreferences? = null

    @JvmStatic
    fun setSubNameToProperties(name: String?, description: String?) {
        multiNameToSubs!!.edit().putString(name, description).apply()
    }

    @JvmStatic
    fun getMultiNameToSubs(all: Boolean): Map<String, String> {
        return getNameToSubs(multiNameToSubs, all)
    }

    fun getNewsNameToSubs(all: Boolean): Map<String, String> {
        return getNameToSubs(newsNameToSubs, all)
    }

    private fun getNameToSubs(sP: SharedPreferences?, all: Boolean): Map<String, String> {
        val multiNameToSubsMapBase: MutableMap<String, String> = HashMap()
        val multiNameToSubsObject = sP!!.all
        for ((key, value) in multiNameToSubsObject) {
            multiNameToSubsMapBase[key] = value.toString()
        }
        if (all) multiNameToSubsMapBase.putAll(subsNameToMulti)
        val multiNameToSubsMap: MutableMap<String, String> = HashMap()
        for ((key, value) in multiNameToSubsMapBase) {
            multiNameToSubsMap[key.lowercase()] = value
        }
        return multiNameToSubsMap
    }

    private val subsNameToMulti: Map<String, String>
        private get() {
            val multiNameToSubsMap: MutableMap<String, String> = HashMap()
            val multiNameToSubsObject = multiNameToSubs!!.all
            for ((key, value) in multiNameToSubsObject) {
                multiNameToSubsMap[value.toString()] = key
            }
            return multiNameToSubsMap
        }

    fun doMainActivitySubs(c: MainActivity) {
        if (NetworkUtil.isConnected(c)) {
            val s = subscriptions!!.getString(Authentication.name, "")
            if (s!!.isEmpty()) {
                //get online subs
                c.updateSubs(syncSubscriptionsOverwrite(c)!!)
            } else {
                val subredditsForHome = CaseInsensitiveArrayList()
                for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    subredditsForHome.add(s2.lowercase())
                }
                c.updateSubs(subredditsForHome)
            }
            c.updateMultiNameToSubs(getMultiNameToSubs(false))
        } else {
            val s = subscriptions!!.getString(Authentication.name, "")
            val subredditsForHome: MutableList<String> = CaseInsensitiveArrayList()
            if (!s!!.isEmpty()) {
                for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    subredditsForHome.add(s2.lowercase())
                }
            }
            val finals = CaseInsensitiveArrayList()
            val offline: List<String> = OfflineSubreddit.allFormatted
            for (subs in subredditsForHome) {
                if (offline.contains(subs)) {
                    finals.add(subs)
                }
            }
            for (subs in offline) {
                if (!finals.contains(subs)) {
                    finals.add(subs)
                }
            }
            c.updateSubs(finals)
            c.updateMultiNameToSubs(getMultiNameToSubs(false))
        }
    }

    @JvmStatic
    fun doNewsSubs(c: NewsActivity) {
        if (NetworkUtil.isConnected(c)) {
            val s = news!!.getString("subs", "news,android")
            if (s!!.isEmpty()) {
                //get online subs
                c.updateSubs(syncSubscriptionsOverwrite(c))
            } else {
                val subredditsForHome = CaseInsensitiveArrayList()
                for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    subredditsForHome.add(s2.lowercase())
                }
                c.updateSubs(subredditsForHome)
            }
            c.updateMultiNameToSubs(getNewsNameToSubs(false))
        } else {
            val s = news!!.getString("subs", "news,android")
            val subredditsForHome: MutableList<String> = CaseInsensitiveArrayList()
            if (!s!!.isEmpty()) {
                for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    subredditsForHome.add(s2.lowercase())
                }
            }
            val finals = CaseInsensitiveArrayList()
            val offline: List<String> = OfflineSubreddit.allFormatted
            for (subs in subredditsForHome) {
                if (offline.contains(subs)) {
                    finals.add(subs)
                }
            }
            for (subs in offline) {
                if (!finals.contains(subs)) {
                    finals.add(subs)
                }
            }
            c.updateSubs(finals)
            c.updateMultiNameToSubs(getMultiNameToSubs(false))
        }
    }

    @JvmStatic
    fun doCachedModSubs() {
        if (modOf == null || modOf!!.isEmpty()) {
            val s = subscriptions!!.getString(Authentication.name + "mod", "")
            if (!s!!.isEmpty()) {
                modOf = CaseInsensitiveArrayList()
                for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    modOf!!.add(s2.lowercase())
                }
            }
        }
    }

    fun cacheModOf() {
        subscriptions!!.edit()
            .putString(Authentication.name + "mod", StringUtil.arrayToString(modOf))
            .apply()
    }

    @JvmStatic
    fun getSubscriptions(c: Context?): CaseInsensitiveArrayList? {
        val s = subscriptions!!.getString(Authentication.name, "")
        return if (s!!.isEmpty()) {
            //get online subs
            syncSubscriptionsOverwrite(c)
        } else {
            val subredditsForHome = CaseInsensitiveArrayList()
            for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (!subredditsForHome.contains(s2)) subredditsForHome.add(s2)
            }
            subredditsForHome
        }
    }

    var pins: CaseInsensitiveArrayList? = null
    @JvmStatic
    fun getPinned(): CaseInsensitiveArrayList? {
        val s = pinned!!.getString(Authentication.name, "")
        return if (s!!.isEmpty()) {
            //get online subs
            CaseInsensitiveArrayList()
        } else if (pins == null) {
            pins = CaseInsensitiveArrayList()
            for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (!pins!!.contains(s2)) pins!!.add(s2)
            }
            pins
        } else {
            pins
        }
    }

    @JvmStatic
    fun getSubscriptionsForShortcut(c: Context?): CaseInsensitiveArrayList? {
        val s = subscriptions!!.getString(Authentication.name, "")
        return if (s!!.isEmpty()) {
            //get online subs
            syncSubscriptionsOverwrite(c)
        } else {
            val subredditsForHome = CaseInsensitiveArrayList()
            for (s2 in s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (!s2.contains("/m/")) subredditsForHome.add(s2.lowercase())
            }
            subredditsForHome
        }
    }

    fun hasSubs(): Boolean {
        val s = subscriptions!!.getString(Authentication.name, "")
        return s!!.isEmpty()
    }

    @JvmField
    var modOf: CaseInsensitiveArrayList? = null
    @JvmField
    var multireddits: ArrayList<MultiReddit?>? = null
    var public_multireddits = HashMap<String, List<MultiReddit?>?>()
    fun doOnlineSyncing() {
        if (Authentication.mod) {
            doModOf()
            if (modOf != null) {
                for (sub in modOf!!) {
                    Toolbox.ensureConfigCachedLoaded(sub)
                    Toolbox.ensureUsernotesCachedLoaded(sub)
                }
            }
        }
        doFriendsOf()
        loadMultireddits()
    }

    var toreturn: CaseInsensitiveArrayList? = null
    @JvmField
    var friends: CaseInsensitiveArrayList? = CaseInsensitiveArrayList()
    fun syncSubscriptionsOverwrite(c: Context?): CaseInsensitiveArrayList? {
        toreturn = CaseInsensitiveArrayList()
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                toreturn = syncSubreddits(c)
                toreturn = sort(toreturn)
                setSubscriptions(toreturn)
                return null
            }
        }.execute()
        if (toreturn!!.isEmpty()) {
            //failed, load defaults
            toreturn!!.addAll(defaultSubs)
        }
        return toreturn
    }

    @JvmStatic
    fun syncSubreddits(c: Context?): CaseInsensitiveArrayList {
        val toReturn = CaseInsensitiveArrayList()
        if (Authentication.isLoggedIn && NetworkUtil.isConnected(c)) {
            val pag = UserSubredditsPaginator(Authentication.reddit, "subscriber")
            pag.setLimit(100)
            try {
                while (pag.hasNext()) {
                    for (s in pag.next()) {
                        toReturn.add(s.displayName.lowercase())
                    }
                }
                if (toReturn.isEmpty() && subscriptions!!.getString(Authentication.name, "")
                        .isNullOrEmpty()
                ) {
                    toreturn!!.addAll(defaultSubs)
                }
            } catch (e: Exception) {
                //failed;
                e.printStackTrace()
            }
            addSubsToHistory(toReturn)
        } else {
            toReturn.addAll(defaultSubs)
        }
        return toReturn
    }

    @JvmStatic
    fun syncMultiReddits(c: Context?) {
        try {
            multireddits = ArrayList(MultiRedditManager(Authentication.reddit).mine())
            for (multiReddit in multireddits!!) {
                if (MainActivity.multiNameToSubsMap.containsKey(
                        ReorderSubreddits.MULTI_REDDIT + multiReddit!!.displayName
                    )
                ) {
                    val concatenatedSubs = StringBuilder()
                    for (subreddit in multiReddit.subreddits) {
                        concatenatedSubs.append(subreddit.displayName)
                        concatenatedSubs.append("+")
                    }
                    MainActivity.multiNameToSubsMap[ReorderSubreddits.MULTI_REDDIT + multiReddit.displayName] =
                        concatenatedSubs.toString()
                    setSubNameToProperties(
                        ReorderSubreddits.MULTI_REDDIT + multiReddit.displayName,
                        concatenatedSubs.toString()
                    )
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
        } catch (e: NetworkException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun setSubscriptions(subs: CaseInsensitiveArrayList?) {
        subscriptions!!.edit().putString(Authentication.name, StringUtil.arrayToString(subs))
            .apply()
    }

    @JvmStatic
    fun setPinned(subs: CaseInsensitiveArrayList?) {
        pinned!!.edit().putString(Authentication.name, StringUtil.arrayToString(subs)).apply()
        pins = null
    }

    @JvmStatic
    fun switchAccounts() {
        val editor = appRestart.edit()
        editor.putBoolean("back", true)
        editor.putString("subs", "")
        authentication.edit().remove("backedCreds").remove("expires").commit()
        editor.putBoolean("loggedin", Authentication.isLoggedIn)
        editor.putString("name", Authentication.name)
        editor.commit()
    }

    /**
     * @return list of multireddits if they are available, null if could not fetch multireddits
     */
    @JvmStatic
    fun getMultireddits(callback: MultiCallback) {
        object : AsyncTask<Void?, Void?, List<MultiReddit?>?>() {
            override fun doInBackground(vararg params: Void?): List<MultiReddit?>? {
                loadMultireddits()
                return multireddits
            }

            override fun onPostExecute(multiReddits: List<MultiReddit?>?) {
                callback.onComplete(multiReddits)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @JvmStatic
    fun loadMultireddits() {
        if (Authentication.isLoggedIn && Authentication.didOnline && (multireddits == null || multireddits!!.isEmpty())) {
            try {
                multireddits = ArrayList(MultiRedditManager(Authentication.reddit).mine())
            } catch (e: Exception) {
                multireddits = null
                e.printStackTrace()
            }
        }
    }

    /**
     * @return list of multireddits if they are available, null if could not fetch multireddits
     */
    fun getPublicMultireddits(callback: MultiCallback, profile: String) {
        if (profile.isEmpty()) {
            getMultireddits(callback)
        }
        if (public_multireddits[profile] == null) {
            // It appears your own multis are pre-loaded at some point
            // but some other user's multis obviously can't be so
            // don't return until we've loaded them.
            loadPublicMultireddits(callback, profile)
        } else {
            callback.onComplete(public_multireddits[profile])
        }
    }

    private fun loadPublicMultireddits(callback: MultiCallback, profile: String) {
        object : AsyncTask<Void?, Void?, List<MultiReddit?>?>() {
            override fun doInBackground(vararg params: Void?): List<MultiReddit?>? {
                try {
                    public_multireddits[profile] = ArrayList<MultiReddit?>(
                        MultiRedditManager(Authentication.reddit).getPublicMultis(profile)
                    )
                } catch (e: Exception) {
                    public_multireddits[profile] = null
                    e.printStackTrace()
                }
                return public_multireddits[profile]
            }

            override fun onPostExecute(multiReddits: List<MultiReddit?>?) {
                callback.onComplete(multiReddits)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun doModOf(): CaseInsensitiveArrayList {
        val finished = CaseInsensitiveArrayList()
        val pag = UserSubredditsPaginator(Authentication.reddit, "moderator")
        pag.setLimit(100)
        try {
            while (pag.hasNext()) {
                for (s in pag.next()) {
                    finished.add(s.displayName.lowercase())
                }
            }
            modOf = finished
            cacheModOf()
        } catch (e: Exception) {
            //failed;
            e.printStackTrace()
        }
        return finished
    }

    fun doFriendsOfMain(main: MainActivity) {
        main.doFriends(doFriendsOf())
    }

    private fun doFriendsOf(): List<String?>? {
        if (friends == null || friends!!.isEmpty()) {
            friends = CaseInsensitiveArrayList()
            val finished = CaseInsensitiveArrayList()
            val pag = ImportantUserPaginator(Authentication.reddit, "friends")
            pag.setLimit(100)
            try {
                while (pag.hasNext()) {
                    for (s in pag.next()) {
                        finished.add(s.fullName)
                    }
                }
                friends = finished
                return friends
            } catch (e: Exception) {
                //failed;
                e.printStackTrace()
            }
        }
        return friends
    }

    @JvmStatic
    fun getMultiredditByDisplayName(displayName: String): MultiReddit? {
        if (multireddits != null) {
            for (multiReddit in multireddits!!) {
                if (multiReddit!!.displayName == displayName) {
                    return multiReddit
                }
            }
        }
        return null
    }

    fun getPublicMultiredditByDisplayName(
        profile: String,
        displayName: String
    ): MultiReddit? {
        if (profile.isEmpty()) {
            return getMultiredditByDisplayName(displayName)
        }
        if (public_multireddits[profile] != null) {
            for (multiReddit in public_multireddits[profile]!!) {
                if (multiReddit!!.displayName == displayName) {
                    return multiReddit
                }
            }
        }
        return null
    }

    //Gets user subscriptions + top 500 subs + subs in history
    @JvmStatic
    fun getAllSubreddits(c: Context): CaseInsensitiveArrayList {
        val finalReturn = CaseInsensitiveArrayList()
        val history = history
        val defaults = getDefaults(c)
        finalReturn.addAll(getSubscriptions(c)!!)
        for (s in finalReturn) {
            history.remove(s)
            defaults.remove(s)
        }
        for (s in history) {
            defaults.remove(s)
        }
        for (s in history) {
            if (!finalReturn.contains(s)) {
                finalReturn.add(s)
            }
        }
        for (s in defaults) {
            if (!finalReturn.contains(s)) {
                finalReturn.add(s)
            }
        }
        return finalReturn
    }

    //Gets user subscriptions + top 500 subs + subs in history
    @JvmStatic
    fun getAllUserSubreddits(c: Context?): CaseInsensitiveArrayList {
        val finalReturn = CaseInsensitiveArrayList()
        finalReturn.addAll(getSubscriptions(c)!!)
        finalReturn.removeAll(history)
        finalReturn.addAll(history)
        return finalReturn
    }

    val history: CaseInsensitiveArrayList
        get() {
            val hist = subscriptions!!.getString("subhistory", "")!!
                .lowercase().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val history = CaseInsensitiveArrayList()
            Collections.addAll(history, *hist)
            return history
        }

    fun getDefaults(c: Context): CaseInsensitiveArrayList {
        val history = CaseInsensitiveArrayList()
        Collections.addAll(
            history,
            *c.getString(R.string.top_500_csv).split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
        return history
    }

    @JvmStatic
    fun addSubreddit(s: String, c: Context?) {
        val subs = getSubscriptions(c)
        subs!!.add(s)
        if (alphabetizeOnSubscribe) {
            setSubscriptions(sortNoExtras(subs))
        } else {
            setSubscriptions(subs)
        }
    }

    fun removeSubreddit(s: String, c: Context?) {
        val subs = getSubscriptions(c)
        subs!!.remove(s)
        setSubscriptions(subs)
    }

    @JvmStatic
    fun addPinned(s: String, c: Context?) {
        val subs = getPinned()
        subs!!.add(s)
        setPinned(subs)
    }

    @JvmStatic
    fun removePinned(s: String, c: Context?) {
        val subs = getPinned()
        subs!!.remove(s)
        setPinned(subs)
    }

    //Sets sub as "searched for", will apply to all accounts
    fun addSubToHistory(s: String) {
        var history = subscriptions!!.getString("subhistory", "")
        if (!history!!.contains(s.lowercase())) {
            history += "," + s.lowercase()
            subscriptions!!.edit().putString("subhistory", history).apply()
        }
    }

    //Sets a list of subreddits as "searched for", will apply to all accounts
    fun addSubsToHistory(s2: ArrayList<Subreddit>) {
        val history = StringBuilder(
            subscriptions!!.getString("subhistory", "")!!.lowercase()
        )
        for (s in s2) {
            if (!history.toString().contains(s.displayName.lowercase())) {
                history.append(",").append(s.displayName.lowercase())
            }
        }
        subscriptions!!.edit().putString("subhistory", history.toString()).apply()
    }

    fun addSubsToHistory(s2: CaseInsensitiveArrayList) {
        val history = StringBuilder(
            subscriptions!!.getString("subhistory", "")!!.lowercase()
        )
        for (s in s2) {
            if (!history.toString().contains(s.lowercase())) {
                history.append(",").append(s.lowercase())
            }
        }
        subscriptions!!.edit().putString("subhistory", history.toString()).apply()
    }

    @JvmStatic
    fun syncSubredditsGetObject(): ArrayList<Subreddit> {
        val toReturn = ArrayList<Subreddit>()
        if (Authentication.isLoggedIn) {
            val pag = UserSubredditsPaginator(Authentication.reddit, "subscriber")
            pag.setLimit(100)
            try {
                while (pag.hasNext()) {
                    toReturn.addAll(pag.next())
                }
            } catch (e: Exception) {
                //failed;
                e.printStackTrace()
            }
            addSubsToHistory(toReturn)
            return toReturn
        }
        return toReturn
    }

    @JvmStatic
    fun syncSubredditsGetObjectAsync(mainActivity: Login) {
        val toReturn = ArrayList<Subreddit>()
        object : AsyncTask<Void?, Void?, Void?>() {
            override fun doInBackground(vararg params: Void?): Void? {
                if (Authentication.isLoggedIn) {
                    val pag = UserSubredditsPaginator(Authentication.reddit, "subscriber")
                    pag.setLimit(100)
                    try {
                        while (pag.hasNext()) {
                            toReturn.addAll(pag.next())
                        }
                    } catch (e: Exception) {
                        //failed;
                        e.printStackTrace()
                    }
                }
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                mainActivity.doLastStuff(toReturn)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Sorts the subreddit ArrayList, keeping special subreddits at the top of the list (e.g.
     * frontpage, all, the random subreddits). Always adds frontpage and all
     *
     * @param unsorted the ArrayList to sort
     * @return the sorted ArrayList
     * @see .sortNoExtras
     */
    @JvmStatic
    fun sort(unsorted: CaseInsensitiveArrayList?): CaseInsensitiveArrayList {
        val subs = CaseInsensitiveArrayList(unsorted)
        if (!subs.contains("frontpage")) {
            subs.add("frontpage")
        }
        if (!subs.contains("all")) {
            subs.add("all")
        }
        return sortNoExtras(subs)
    }

    /**
     * Sorts the subreddit ArrayList, keeping special subreddits at the top of the list (e.g.
     * frontpage, all, the random subreddits)
     *
     * @param unsorted the ArrayList to sort
     * @return the sorted ArrayList
     * @see .sort
     */
    @JvmStatic
    fun sortNoExtras(unsorted: CaseInsensitiveArrayList?): CaseInsensitiveArrayList {
        val subs: MutableList<String> = CaseInsensitiveArrayList(unsorted)
        val finals = CaseInsensitiveArrayList()
        for (subreddit in getPinned()!!) {
            if (subs.contains(subreddit)) {
                subs.remove(subreddit)
                finals.add(subreddit)
            }
        }
        for (subreddit in specialSubreddits) {
            if (subs.contains(subreddit)) {
                subs.remove(subreddit)
                finals.add(subreddit)
            }
        }
        Collections.sort(subs, java.lang.String.CASE_INSENSITIVE_ORDER)
        finals.addAll(subs)
        return finals
    }

    class SyncMultireddits(var c: Context) : AsyncTask<Void?, Void?, Boolean?>() {
        public override fun onPostExecute(b: Boolean?) {
            val i = Intent(c, MultiredditOverview::class.java)
            c.startActivity(i)
            (c as Activity).finish()
        }

        override fun doInBackground(vararg params: Void?): Boolean? {
            syncMultiReddits(c)
            return null
        }
    }

    interface MultiCallback {
        fun onComplete(multis: List<MultiReddit?>?)
    }

    class SubscribeTask(var context: Context) : AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg subreddits: String?): Void? {
            val m = AccountManager(Authentication.reddit)
            for (subreddit in subreddits) {
                try {
                    m.subscribe(Authentication.reddit!!.getSubreddit(subreddit))
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Couldn't subscribe, subreddit is private, quarantined, or invite only",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return null
        }
    }

    class UnsubscribeTask : AsyncTask<String?, Void?, Void?>() {
        override fun doInBackground(vararg subreddits: String?): Void? {
            val m = AccountManager(Authentication.reddit)
            try {
                for (subreddit in subreddits) {
                    m.unsubscribe(Authentication.reddit!!.getSubreddit(subreddit))
                }
            } catch (e: Exception) {
            }
            return null
        }
    }
}
