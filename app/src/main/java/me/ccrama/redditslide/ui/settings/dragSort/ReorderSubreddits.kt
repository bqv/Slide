package me.ccrama.redditslide.ui.settings.dragSort

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import com.nambimobile.widgets.efab.FabOption
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.alphabetizeOnSubscribe
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.UserSubscriptions.UnsubscribeTask
import me.ccrama.redditslide.UserSubscriptions.addPinned
import me.ccrama.redditslide.UserSubscriptions.getMultiredditByDisplayName
import me.ccrama.redditslide.UserSubscriptions.getPinned
import me.ccrama.redditslide.UserSubscriptions.getSubscriptions
import me.ccrama.redditslide.UserSubscriptions.loadMultireddits
import me.ccrama.redditslide.UserSubscriptions.removePinned
import me.ccrama.redditslide.UserSubscriptions.setPinned
import me.ccrama.redditslide.UserSubscriptions.setSubNameToProperties
import me.ccrama.redditslide.UserSubscriptions.setSubscriptions
import me.ccrama.redditslide.UserSubscriptions.sort
import me.ccrama.redditslide.UserSubscriptions.sortNoExtras
import me.ccrama.redditslide.UserSubscriptions.syncMultiReddits
import me.ccrama.redditslide.UserSubscriptions.syncSubreddits
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.ui.settings.SettingsThemeFragment
import me.ccrama.redditslide.ui.settings.dragSort.DragSortRecycler.OnDragStateChangedListener
import me.ccrama.redditslide.ui.settings.dragSort.DragSortRecycler.OnItemMovedListener
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.LogUtil
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.MultiSubreddit
import net.dean.jraw.models.Subreddit
import net.dean.jraw.paginators.SubredditSearchPaginator
import net.dean.jraw.paginators.UserSubredditsPaginator

class ReorderSubreddits : BaseActivityAnim() {
    private var subs: CaseInsensitiveArrayList? = null
    private var adapter: CustomAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var input: String? = null
    var subscribe: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.reorder_subs, menu)
        subscribe = menu.findItem(R.id.alphabetize_subscribe)
        subscribe!!.isChecked = alphabetizeOnSubscribe
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.refresh -> {
                done = 0
                val d: Dialog = MaterialDialog(this@ReorderSubreddits)
                    .title(R.string.general_sub_sync)
                    .message(R.string.misc_please_wait)
                    //.progress(true, 100)
                    .cancelable(false)
                    .also { it.show() }
                object : AsyncTask<Void?, Void?, ArrayList<String>>() {
                    override fun doInBackground(vararg params: Void?): ArrayList<String> {
                        val newSubs = ArrayList(
                            syncSubreddits(this@ReorderSubreddits)
                        )
                        syncMultiReddits(this@ReorderSubreddits)
                        return newSubs
                    }

                    override fun onPostExecute(newSubs: ArrayList<String>) {
                        d.dismiss()
                        // Determine if we should insert subreddits at the end of the list or sorted
                        val sorted = ((subs == sortNoExtras(subs)))
                        val res = resources
                        for (s: String in newSubs) {
                            if (!subs!!.contains(s)) {
                                done++
                                subs!!.add(s)
                            }
                        }
                        if (sorted && done > 0) {
                            subs = sortNoExtras(subs)
                            adapter = CustomAdapter(subs!!)
                            recyclerView!!.adapter = adapter
                        } else if (done > 0) {
                            adapter!!.notifyDataSetChanged()
                            recyclerView!!.smoothScrollToPosition(subs!!.size)
                        }
                        AlertDialog.Builder(this@ReorderSubreddits)
                            .setTitle(R.string.reorder_sync_complete)
                            .setMessage(
                                res.getQuantityString(
                                    R.plurals.reorder_subs_added,
                                    done,
                                    done
                                )
                            )
                            .setPositiveButton(R.string.btn_ok, null)
                            .show()
                    }
                }.execute()
                return true
            }

            R.id.alphabetize -> {
                subs = sortNoExtras(subs)
                adapter = CustomAdapter(subs!!)
                //  adapter.setHasStableIds(true);
                recyclerView!!.adapter = adapter
                return true
            }

            R.id.alphabetize_subscribe -> {
                alphabetizeOnSubscribe = !alphabetizeOnSubscribe
                if (subscribe != null) subscribe!!.isChecked = alphabetizeOnSubscribe
                return true
            }

            R.id.info -> {
                AlertDialog.Builder(this@ReorderSubreddits)
                    .setTitle(R.string.reorder_subs_FAQ)
                    .setMessage(R.string.sorting_faq)
                    .show()
                return true
            }
        }
        return false
    }

    override fun onPause() {
        try {
            setSubscriptions(CaseInsensitiveArrayList(subs))
            SettingsThemeFragment.changed = true
        } catch (e: Exception) {
        }
        super.onPause()
    }

    override fun onBackPressed() {
        if (isMultiple) {
            chosen = ArrayList()
            doOldToolbar()
            adapter!!.notifyDataSetChanged()
            isMultiple = false
        } else {
            super.onBackPressed()
        }
    }

    private var chosen = ArrayList<String>()
    var isSubscribed: HashMap<String, Boolean>? = null
    private var isMultiple = false
    private var done = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        disableSwipeBackLayout()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_sort)
        setupAppBar(R.id.toolbar, R.string.settings_manage_subscriptions, false, true)
        mToolbar!!.popupTheme = ColorPreferences(this).fontStyle.baseId
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        isSubscribed = HashMap()
        if (Authentication.isLoggedIn) {
            object : AsyncTask<Void?, Void?, Void?>() {
                var success = true
                override fun doInBackground(vararg params: Void?): Void? {
                    val subs = ArrayList<Subreddit>()
                    val p = UserSubredditsPaginator(Authentication.reddit, "subscriber")
                    try {
                        while (p.hasNext()) {
                            subs.addAll(p.next())
                        }
                    } catch (e: Exception) {
                        success = false
                        return null
                    }
                    for (s: Subreddit in subs) {
                        isSubscribed!![s.displayName.lowercase()] = true
                    }
                    if (UserSubscriptions.multireddits == null) {
                        loadMultireddits()
                    }
                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    if (success) {
                        d!!.dismiss()
                        doShowSubs()
                    } else {
                        AlertDialog.Builder(this@ReorderSubreddits)
                            .setTitle(R.string.err_title)
                            .setMessage(R.string.misc_please_try_again_soon)
                            .setCancelable(false)
                            .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface?, which: Int -> finish() }
                            .show()
                    }
                }

                var d: Dialog? = null
                override fun onPreExecute() {
                    d = MaterialDialog(this@ReorderSubreddits)
                        //.progress(true, 100)
                        .message(R.string.misc_please_wait)
                        .title(R.string.reorder_loading_title)
                        .cancelable(false)
                        .also { it.show() }
                }
            }.execute()
        } else {
            doShowSubs()
        }
    }

    fun doShowSubs() {
        subs = CaseInsensitiveArrayList(getSubscriptions(this))
        recyclerView = findViewById<View>(R.id.subslist) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.itemAnimator = null
        val dragSortRecycler = DragSortRecycler()
        dragSortRecycler.setViewHandleId()
        dragSortRecycler.setFloatingAlpha()
        dragSortRecycler.setAutoScrollSpeed()
        dragSortRecycler.setAutoScrollWindow()
        dragSortRecycler.setOnItemMovedListener(OnItemMovedListener { from, to ->
            var to = to
            if (to == subs!!.size) {
                to -= 1
            }
            val item = subs!!.removeAt(from)
            subs!!.add(to, item)
            adapter!!.notifyDataSetChanged()
            val pinned = getPinned()
            if (pinned!!.contains(item) && pinned.size != 1) {
                pinned.remove(item)
                if (to > pinned.size) {
                    to = pinned.size
                }
                pinned.add(to, item)
                setPinned(pinned)
            }
        })
        dragSortRecycler.setOnDragStateChangedListener(
            object : OnDragStateChangedListener {
                override fun onDragStart() {}
                override fun onDragStop() {}
            })
        run {
            val collectionFab: FabOption =
                findViewById<View>(R.id.sort_fabOption_collection) as FabOption
            collectionFab.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if ((UserSubscriptions.multireddits != null
                                && !UserSubscriptions.multireddits!!.isEmpty())
                    ) {
                        AlertDialog.Builder(this@ReorderSubreddits)
                            .setTitle(R.string.create_or_import_multi)
                            .setPositiveButton(R.string.btn_new) { dialog: DialogInterface?, which: Int -> doCollection() }
                            .setNegativeButton(R.string.btn_import_multi) { dialog: DialogInterface?, which: Int ->
                                val multis: Array<String?> = arrayOfNulls(
                                    UserSubscriptions.multireddits!!.size
                                )
                                for ((i, m: MultiReddit?) in UserSubscriptions.multireddits!!.withIndex()) {
                                    multis[i] = m!!.displayName
                                }
                                MaterialDialog(this@ReorderSubreddits)
                                    .title(R.string.reorder_subreddits_title)
                                    .listItemsSingleChoice(
                                        initialSelection = -1,
                                        items = multis.filterNotNull()) { dialog: MaterialDialog, which: Int, text: CharSequence ->
                                            val name: String? = multis[which]
                                            val r: MultiReddit? =
                                                getMultiredditByDisplayName((name)!!)
                                            val b: StringBuilder = StringBuilder()
                                            for (s: MultiSubreddit in r!!.subreddits) {
                                                b.append(s.displayName)
                                                b.append("+")
                                            }
                                            val pos: Int = addSubAlphabetically(
                                                (MULTI_REDDIT
                                                        + r.displayName)
                                            )
                                            setSubNameToProperties(
                                                (MULTI_REDDIT
                                                        + r.displayName),
                                                b.toString()
                                            )
                                            adapter!!.notifyDataSetChanged()
                                            recyclerView!!.smoothScrollToPosition(pos)
                                        }
                                    .show()
                            }
                            .show()
                    } else {
                        doCollection()
                    }
                }
            })
        }
        run {
            val subFab: FabOption = findViewById<View>(R.id.sort_fabOption_sub) as FabOption
            subFab.setOnClickListener {
                val b = MaterialDialog(this@ReorderSubreddits)
                    .title(R.string.reorder_add_or_search_subreddit)
                    .input(hintRes = R.string.reorder_subreddit_name,
                        waitForPositiveButton = false) { dialog: MaterialDialog, raw: CharSequence ->
                        input = raw.toString()
                    }
                    .positiveButton(R.string.btn_add) { dialog: MaterialDialog ->
                        AsyncGetSubreddit().execute(input)
                    }
                    .negativeButton(R.string.btn_cancel)
                b.show()
            }
        }
        run {
            val domainFab: FabOption = findViewById<View>(R.id.sort_fabOption_domain) as FabOption
            domainFab.setOnClickListener {
                MaterialDialog(this@ReorderSubreddits)
                    .title(R.string.reorder_add_domain)
                    .input(
                        hint = "example.com${getString(R.string.reorder_domain_placeholder)}",
                        waitForPositiveButton = false) { dialog: MaterialDialog, raw: CharSequence ->
                        input = raw.toString().replace("\\s".toRegex(), "") //remove whitespace from input
                        dialog.getActionButton(WhichButton.POSITIVE).isEnabled = input!!.contains(".")
                    }
                    .positiveButton(R.string.btn_add) { dialog: MaterialDialog ->
                        try {
                            val url: String? = (input)
                            val sortedSubs: List<String> = sortNoExtras(subs)
                            if ((sortedSubs == subs)) {
                                subs!!.add(url)
                                subs = sortNoExtras(subs)
                                adapter = CustomAdapter(subs!!)
                                recyclerView!!.adapter = adapter
                            } else {
                                val pos: Int = addSubAlphabetically(url)
                                adapter!!.notifyDataSetChanged()
                                recyclerView!!.smoothScrollToPosition(pos)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            //todo make this better
                            AlertDialog.Builder(this@ReorderSubreddits)
                                .setTitle(R.string.reorder_url_err)
                                .setMessage(R.string.misc_please_try_again)
                                .show()
                        }
                    }
                    .negativeButton(R.string.btn_cancel)
                    .show()
            }
        }
        recyclerView!!.addItemDecoration(dragSortRecycler)
        recyclerView!!.addOnItemTouchListener(dragSortRecycler)
        recyclerView!!.addOnScrollListener(dragSortRecycler.scrollListener)
        dragSortRecycler.setViewHandleId()
        if (subs != null && !subs!!.isEmpty()) {
            adapter = CustomAdapter(subs!!)
            //  adapter.setHasStableIds(true);
            recyclerView!!.adapter = adapter
        } else {
            subs = CaseInsensitiveArrayList()
        }
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    diff += dy
                } else {
                    diff = 0
                }
            }
        })
    }

    var diff = 0
    fun doCollection() {
        val subs2: ArrayList<String> = sort(getSubscriptions(this))
        subs2.remove("frontpage")
        subs2.remove("all")
        val toRemove = ArrayList<String>()
        for (s: String in subs2) {
            if (s.contains(".") || s.contains(MULTI_REDDIT)) {
                toRemove.add(s)
            }
        }
        subs2.removeAll(toRemove.toSet())
        val subsAsChar = subs2.toTypedArray<CharSequence>()
        MaterialDialog(this@ReorderSubreddits)
            .title(R.string.reorder_subreddits_title)
            .listItemsMultiChoice(items = subsAsChar.toList()) { dialog, which, text ->
                val selectedSubs = ArrayList<String>()
                for (i: Int in which) {
                    selectedSubs.add(subsAsChar[i].toString())
                }
                val b = StringBuilder()
                for (s: String? in selectedSubs) {
                    b.append(s)
                    b.append("+")
                }
                val finalS = b.substring(0, b.length - 1)
                Log.v(LogUtil.getTag(), finalS)
                val pos = addSubAlphabetically(finalS)
                adapter!!.notifyDataSetChanged()
                recyclerView!!.smoothScrollToPosition(pos)
            }
            .positiveButton(R.string.btn_add)
            .negativeButton(R.string.btn_cancel)
            .also { it.show() }
    }

    fun doAddSub(subreddit: String?) {
        var subreddit = subreddit
        subreddit = subreddit!!.lowercase()
        val sortedSubs: List<String> = sortNoExtras(subs)
        if ((sortedSubs == subs)) {
            subs!!.add(subreddit)
            subs = sortNoExtras(subs)
            adapter = CustomAdapter(subs!!)
            recyclerView!!.adapter = adapter
        } else {
            val pos = addSubAlphabetically(subreddit)
            adapter!!.notifyDataSetChanged()
            recyclerView!!.smoothScrollToPosition(pos)
        }
    }

    private fun addSubAlphabetically(finalS: String?): Int {
        var i = subs!!.size - 1
        while (i >= 0 && finalS!!.compareTo(subs!![i]) < 0) {
            i--
        }
        i += 1
        subs!!.add(i, finalS)
        return i
    }

    private inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        public override fun onPostExecute(subreddit: Subreddit?) {
            if (subreddit != null) {
                doAddSub(subreddit.displayName)
            } else if (isSpecial(sub)) {
                doAddSub(sub)
            }
        }

        var otherSubs: ArrayList<Subreddit>? = null
        var sub: String? = null
        override fun doInBackground(vararg params: String?): Subreddit? {
            sub = params[0]
            if (isSpecial(sub)) return null
            try {
                return (if (subs!!.contains(params[0])) null else Authentication.reddit!!.getSubreddit(
                    params[0]
                ))
            } catch (e: Exception) {
                otherSubs = ArrayList()
                val p = SubredditSearchPaginator(Authentication.reddit, sub)
                while (p.hasNext()) {
                    otherSubs!!.addAll((p.next()))
                }
                if (otherSubs!!.isEmpty()) {
                    runOnUiThread {
                        try {
                            AlertDialog.Builder(this@ReorderSubreddits)
                                .setTitle(R.string.subreddit_err)
                                .setMessage(getString(R.string.subreddit_err_msg, params[0]))
                                .setPositiveButton(
                                    R.string.btn_ok
                                ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                                .setOnDismissListener(null)
                                .show()
                        } catch (ignored: Exception) {
                        }
                    }
                } else {
                    runOnUiThread {
                        try {
                            val subs = ArrayList<String>()
                            for (s: Subreddit in otherSubs!!) {
                                subs.add(s.displayName)
                            }
                            AlertDialog.Builder(this@ReorderSubreddits)
                                .setTitle(R.string.reorder_not_found_err)
                                .setItems(
                                    subs.toTypedArray()
                                ) { dialog: DialogInterface?, which: Int ->
                                    doAddSub(
                                        subs.get(which)
                                    )
                                }
                                .setPositiveButton(R.string.btn_cancel, null)
                                .show()
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
            return null
        }
    }

    fun doOldToolbar() {
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        mToolbar!!.visibility = View.VISIBLE
    }

    inner class CustomAdapter(private val items: ArrayList<String>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == 2) {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.spacer, parent, false)
                return SpacerViewHolder(v)
            }
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.subforsublistdrag, parent, false)
            return ViewHolder(v)
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == items.size) {
                2
            } else 1
        }

        fun doNewToolbar() {
            mToolbar!!.visibility = View.GONE
            mToolbar = findViewById<View>(R.id.toolbar2) as Toolbar
            mToolbar!!.title = resources.getQuantityString(
                R.plurals.reorder_selected, chosen.size,
                chosen.size
            )
            mToolbar!!.findViewById<View>(R.id.delete)
                .setOnClickListener {
                    val b = AlertDialog.Builder(this@ReorderSubreddits)
                        .setTitle(R.string.reorder_remove_title)
                        .setPositiveButton(
                            R.string.btn_remove
                        ) { dialog: DialogInterface?, which: Int ->
                            for (s: String in chosen) {
                                val index: Int = subs!!.indexOf(s)
                                subs!!.removeAt(index)
                                adapter!!.notifyItemRemoved(index)
                            }
                            isMultiple = false
                            chosen = ArrayList()
                            doOldToolbar()
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                    if (Authentication.isLoggedIn && Authentication.didOnline && isSingle(chosen)) {
                        b.setNeutralButton(
                            R.string.reorder_remove_unsubscribe
                        ) { dialog: DialogInterface?, which: Int ->
                            for (s: String in chosen) {
                                val index: Int = subs!!.indexOf(s)
                                subs!!.removeAt(index)
                                adapter!!.notifyItemRemoved(index)
                            }
                            UnsubscribeTask().execute(*chosen.toTypedArray())
                            for (s: String in chosen) {
                                isSubscribed!!.put(s.lowercase(), false)
                            }
                            isMultiple = false
                            chosen = ArrayList()
                            doOldToolbar()
                        }
                    }
                    b.show()
                }
            mToolbar!!.findViewById<View>(R.id.top)
                .setOnClickListener {
                    for (s: String in chosen) {
                        subs!!.remove(s)
                        subs!!.add(0, s)
                    }
                    isMultiple = false
                    doOldToolbar()
                    chosen = ArrayList()
                    notifyDataSetChanged()
                    recyclerView!!.smoothScrollToPosition(0)
                }
            mToolbar!!.findViewById<View>(R.id.pin)
                .setOnClickListener {
                    val pinned: List<String>? = getPinned()
                    val contained = pinned!!.containsAll(chosen)
                    for (s: String in chosen) {
                        if (contained) {
                            removePinned(s, this@ReorderSubreddits)
                        } else {
                            addPinned(s, this@ReorderSubreddits)
                            subs!!.remove(s)
                            subs!!.add(0, s)
                        }
                    }
                    isMultiple = false
                    doOldToolbar()
                    chosen = ArrayList()
                    notifyDataSetChanged()
                    recyclerView!!.smoothScrollToPosition(0)
                }
        }

        var textColorAttr = intArrayOf(R.attr.fontColor)
        var ta = obtainStyledAttributes(textColorAttr)
        var textColor = ta.getColor(0, Color.BLACK)
        fun updateToolbar() {
            mToolbar!!.title = resources.getQuantityString(
                R.plurals.reorder_selected, chosen.size,
                chosen.size
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ViewHolder) {
                val origPos = items[position]
                holder.text.text = origPos
                if (chosen.contains(origPos)) {
                    holder.itemView.setBackgroundColor(
                        Palette.getDarkerColor(holder.text.currentTextColor)
                    )
                    holder.text.setTextColor(Color.WHITE)
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                    holder.text.setTextColor(textColor)
                }
                if (!isSingle(origPos) || !Authentication.isLoggedIn) {
                    holder.check.visibility = View.GONE
                } else {
                    holder.check.visibility = View.VISIBLE
                }
                holder.check.setOnCheckedChangeListener { buttonView, isChecked ->
                    //do nothing
                }
                holder.check.isChecked =
                    isSubscribed!!.containsKey(origPos.lowercase()) && (isSubscribed!!.get(
                        origPos.lowercase()
                    ))!!
                holder.check.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!isChecked) {
                        UnsubscribeTask().execute(origPos)
                        Snackbar.make(
                            (mToolbar)!!,
                            getString(R.string.reorder_unsubscribed_toast, origPos),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        UserSubscriptions.SubscribeTask(this@ReorderSubreddits)
                            .execute(origPos)
                        Snackbar.make(
                            (mToolbar)!!,
                            getString(R.string.reorder_subscribed_toast, origPos),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    isSubscribed!![origPos.lowercase()] = isChecked
                }
                val colorView = holder.itemView.findViewById<View>(R.id.color)
                colorView.setBackgroundResource(R.drawable.circle)
                BlendModeUtil.tintDrawableAsModulate(
                    colorView.background,
                    Palette.getColor(origPos)
                )
                if (getPinned()!!.contains(origPos)) {
                    holder.itemView.findViewById<View>(R.id.pinned).visibility = View.VISIBLE
                } else {
                    holder.itemView.findViewById<View>(R.id.pinned).visibility = View.GONE
                }
                holder.itemView.setOnLongClickListener {
                    if (!isMultiple) {
                        isMultiple = true
                        chosen = ArrayList()
                        chosen.add(origPos)
                        doNewToolbar()
                        holder.itemView.setBackgroundColor(
                            Palette.getDarkerColor(Palette.getDefaultAccent())
                        )
                        holder.text.setTextColor(Color.WHITE)
                    } else if (chosen.contains(origPos)) {
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

                        //set the color of the text back to what it should be
                        holder.text.setTextColor(textColor)
                        chosen.remove(origPos)
                        if (chosen.isEmpty()) {
                            isMultiple = false
                            doOldToolbar()
                        }
                    } else {
                        chosen.add(origPos)
                        holder.itemView.setBackgroundColor(
                            Palette.getDarkerColor(Palette.getDefaultAccent())
                        )
                        holder.text.setTextColor(textColor)
                        updateToolbar()
                    }
                    true
                }
                holder.itemView.setOnClickListener {
                    if (!isMultiple) {
                        AlertDialog.Builder(this@ReorderSubreddits)
                            .setItems(arrayOf<CharSequence>(
                                getString(R.string.reorder_move),
                                if (getPinned()!!.contains(origPos)) "Unpin" else "Pin",
                                getString(R.string.btn_delete)
                            )
                            ) { dialog: DialogInterface?, which: Int ->
                                if (which == 2) {
                                    val b: AlertDialog.Builder =
                                        AlertDialog.Builder(this@ReorderSubreddits)
                                            .setTitle(R.string.reorder_remove_title)
                                            .setPositiveButton(
                                                R.string.btn_remove
                                            ) { dialog1: DialogInterface?, which1: Int ->
                                                subs!!.remove(
                                                    items.get(position)
                                                )
                                                adapter!!.notifyItemRemoved(position)
                                            }
                                            .setNegativeButton(R.string.btn_cancel, null)
                                    if ((Authentication.isLoggedIn
                                                && Authentication.didOnline
                                                && isSingle(origPos))
                                    ) {
                                        b.setNeutralButton(
                                            R.string.reorder_remove_unsubscribe
                                        ) { dialog12: DialogInterface?, which12: Int ->
                                            val sub: String = items.get(position)
                                            subs!!.remove(sub)
                                            adapter!!.notifyItemRemoved(position)
                                            UnsubscribeTask().execute(sub)
                                            isSubscribed!![sub.lowercase()] = false
                                        }
                                    }
                                    b.show()
                                } else if (which == 0) {
                                    val s: String =
                                        items.get(holder.getBindingAdapterPosition())
                                    subs!!.remove(s)
                                    subs!!.add(0, s)
                                    notifyItemMoved(holder.getBindingAdapterPosition(), 0)
                                    recyclerView!!.smoothScrollToPosition(0)
                                } else if (which == 1) {
                                    val s: String =
                                        items.get(holder.getBindingAdapterPosition())
                                    if (!getPinned()!!.contains(s)) {
                                        val index: Int = subs!!.indexOf(s)
                                        addPinned(
                                            s,
                                            this@ReorderSubreddits
                                        )
                                        subs!!.removeAt(index)
                                        subs!!.add(0, s)
                                        notifyItemMoved(
                                            holder.getBindingAdapterPosition(),
                                            0
                                        )
                                        recyclerView!!.smoothScrollToPosition(0)
                                    } else {
                                        removePinned(
                                            s,
                                            this@ReorderSubreddits
                                        )
                                        adapter!!.notifyItemChanged(
                                            holder.getBindingAdapterPosition()
                                        )
                                    }
                                }
                            }
                            .show()
                    } else {
                        if (chosen.contains(origPos)) {
                            holder.itemView.setBackgroundColor(Color.TRANSPARENT)

                            //set the color of the text back to what it should be
                            val textColorAttr = intArrayOf(R.attr.fontColor)
                            val ta = obtainStyledAttributes(textColorAttr)
                            holder.text.setTextColor(ta.getColor(0, Color.BLACK))
                            ta.recycle()
                            chosen.remove(origPos)
                            updateToolbar()
                            if (chosen.isEmpty()) {
                                isMultiple = false
                                doOldToolbar()
                            }
                        } else {
                            chosen.add(origPos)
                            holder.itemView.setBackgroundColor(
                                Palette.getDarkerColor(Palette.getDefaultAccent())
                            )
                            holder.text.setTextColor(Color.WHITE)
                            updateToolbar()
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return items.size + 1
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView
            val check: AppCompatCheckBox

            init {
                text = itemView.findViewById(R.id.name)
                check = itemView.findViewById(R.id.isSubscribed)
            }
        }

        inner class SpacerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.findViewById<View>(R.id.height).layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        DisplayUtil.dpToPxVertical(88)
                    )
            }
        }
    }

    /**
     * Check if all of the subreddits are single
     *
     * @param subreddits list of subreddits to check
     * @return if all of the subreddits are single
     * @see .isSingle
     */
    private fun isSingle(subreddits: List<String>): Boolean {
        for (subreddit: String in subreddits) {
            if (!isSingle(subreddit)) return false
        }
        return true
    }

    /**
     * If the subreddit isn't special, combined, or a multireddit - can attempt to be subscribed to
     *
     * @param subreddit name of a subreddit
     * @return if the subreddit is single
     */
    private fun isSingle(subreddit: String): Boolean {
        return !((isSpecial(subreddit)
                || subreddit.contains("+")
                || subreddit.contains(".")
                || subreddit.contains(MULTI_REDDIT)))
    }

    /**
     * Subreddits with important behaviour - frontpage, all, random, etc.
     *
     * @param subreddit name of a subreddit
     * @return if the subreddit is special
     */
    private fun isSpecial(subreddit: String?): Boolean {
        for (specialSubreddit: String in UserSubscriptions.specialSubreddits) {
            if (subreddit.equals(specialSubreddit, ignoreCase = true)) return true
        }
        return false
    }

    companion object {
        val MULTI_REDDIT = "/m/"
    }
}
