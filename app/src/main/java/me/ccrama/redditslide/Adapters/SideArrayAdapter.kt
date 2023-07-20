package me.ccrama.redditslide.Adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.cardview.widget.CardView
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues.single
import ltd.ucode.slide.SettingValues.subredditSearchMethod
import ltd.ucode.slide.ui.main.MainActivity
import ltd.ucode.slide.ui.main.MainPagerAdapterComment
import me.ccrama.redditslide.Activities.SubredditView
import me.ccrama.redditslide.CaseInsensitiveArrayList
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.UserSubscriptions.getMultiNameToSubs
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.StringUtil
import org.apache.commons.lang3.StringUtils

class SideArrayAdapter(
    context: Context?, objects: ArrayList<String?>?,
    allSubreddits: ArrayList<String>?, view: ListView
) : ArrayAdapter<String?>(context!!, 0, objects!!) {
    private val objects: MutableList<String>
    private var filter: Filter
    var baseItems: CaseInsensitiveArrayList
    var fitems: CaseInsensitiveArrayList?
    var parentL: ListView
    var openInSubView = true

    override fun isEnabled(position: Int): Boolean {
        return false
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = SubFilter()
        }
        return filter
    }

    var height = 0
    var multiToMatch: Map<String, String>

    init {
        this.objects = ArrayList(allSubreddits)
        filter = SubFilter()
        fitems = CaseInsensitiveArrayList(objects)
        baseItems = CaseInsensitiveArrayList(objects)
        parentL = view
        multiToMatch = getMultiNameToSubs(true)
    }

    private fun hideSearchbarUI() {
        try {
            //Hide the toolbar search UI without an animation because we're starting a new activity
            if ((subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                    || subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                && (context as MainActivity).findViewById<View>(R.id.toolbar_search)
                    .visibility == View.VISIBLE) {
                (context as MainActivity).findViewById<View>(
                    R.id.toolbar_search_suggestions).visibility = View.GONE
                (context as MainActivity).findViewById<View>(R.id.toolbar_search).visibility = View.GONE
                (context as MainActivity).findViewById<View>(R.id.close_search_toolbar).visibility = View.GONE

                //Play the exit animations of the search toolbar UI to avoid the animations failing to animate upon the next time
                //the search toolbar UI is called. Set animation to 0 because the UI is already hidden.
                (context as MainActivity).exitAnimationsForToolbarSearch(0,
                    ((context as MainActivity).findViewById<View>(
                        R.id.toolbar_search_suggestions) as CardView),
                    ((context as MainActivity).findViewById<View>(
                        R.id.toolbar_search) as AutoCompleteTextView),
                    ((context as MainActivity).findViewById<View>(
                        R.id.close_search_toolbar) as ImageView))
                if (single) {
                    (context as MainActivity).supportActionBar!!.title = (context as MainActivity).selectedSub
                } else {
                    (context as MainActivity).supportActionBar!!.title = (context as MainActivity).tabViewModeTitle
                }
            }
        } catch (npe: NullPointerException) {
            Log.e(javaClass.name, npe.message!!)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (position < fitems!!.size) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.subforsublist, parent, false)
            val sub: String?
            val base = fitems!![position]
            sub = if (multiToMatch.containsKey(fitems!![position]) && !fitems!![position]
                    .contains("/m/")) {
                multiToMatch[fitems!![position]]
            } else {
                fitems!![position]
            }
            val t = convertView.findViewById<TextView>(R.id.name)
            t.text = sub
            if (height == 0) {
                val finalConvertView = convertView
                convertView.viewTreeObserver
                    .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            height = finalConvertView!!.height
                            finalConvertView.viewTreeObserver
                                .removeOnGlobalLayoutListener(this)
                        }
                    })
            }
            val subreddit = if (sub!!.contains("+") || sub.contains("/m/")) sub else StringUtil.sanitizeString(
                sub.replace(context.getString(R.string.search_goto) + " ", ""))
            val colorView = convertView.findViewById<View>(R.id.color)
            colorView.setBackgroundResource(R.drawable.circle)
            BlendModeUtil.tintDrawableAsModulate(colorView.background, Palette.getColor(subreddit))
            convertView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    if (base.startsWith(context.getString(R.string.search_goto) + " ")
                        || !(context as MainActivity).usedArray!!.contains(base)) {
                        hideSearchbarUI()
                        val inte = Intent(context, SubredditView::class.java)
                        inte.putExtra(SubredditView.EXTRA_SUBREDDIT, subreddit)
                        (context as Activity).startActivityForResult(inte, MainActivity.SEARCH_RESULT)
                    } else {
                        if ((context as MainActivity).commentPager
                            && (context as MainActivity).adapter is MainPagerAdapterComment) {
                            (context as MainActivity).openingComments = null
                            (context as MainActivity).toOpenComments = -1
                            ((context as MainActivity).adapter as MainPagerAdapterComment?)!!.size = (context as MainActivity).usedArray!!.size + 1
                            (context as MainActivity).reloadItemNumber = (context as MainActivity).usedArray!!.indexOf(base)
                            (context as MainActivity).adapter!!.notifyDataSetChanged()
                            (context as MainActivity).doPageSelectedComments(
                                (context as MainActivity).usedArray!!.indexOf(base))
                            (context as MainActivity).reloadItemNumber = -2
                        }
                        try {
                            //Hide the toolbar search UI with an animation because we're just changing tabs
                            if ((subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                    || subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                                && (context as MainActivity).findViewById<View>(
                                    R.id.toolbar_search).visibility == View.VISIBLE) {
                                (context as MainActivity).findViewById<View>(
                                    R.id.close_search_toolbar).performClick()
                            }
                        } catch (npe: NullPointerException) {
                            Log.e(javaClass.name, npe.message!!)
                        }
                        (context as MainActivity).pager.currentItem = (context as MainActivity).usedArray!!.indexOf(base)
                        (context as MainActivity).drawerLayout!!.closeDrawers()
                        if ((context as MainActivity).drawerSearch != null) {
                            (context as MainActivity).drawerSearch!!.setText("")
                        }
                    }
                    KeyboardUtil.hideKeyboard(context, view.windowToken, 0)
                }
            })
            convertView.setOnLongClickListener(View.OnLongClickListener { view ->
                hideSearchbarUI()
                val inte = Intent(context, SubredditView::class.java)
                inte.putExtra(SubredditView.EXTRA_SUBREDDIT, subreddit)
                (context as Activity).startActivityForResult(inte, MainActivity.SEARCH_RESULT)
                KeyboardUtil.hideKeyboard(context, view.windowToken, 0)
                true
            })
        } else {
            convertView = LayoutInflater.from(context).inflate(R.layout.spacer, parent, false)
            val params = convertView.findViewById<View>(R.id.height).layoutParams
            if (fitems!!.size * height < parentL.height
                && (subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_DRAWER
                    || subredditSearchMethod
                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)) {
                params.height = parentL.height - (count - 1) * height
            } else {
                params.height = 0
            }
            convertView.layoutParams = params
        }
        return convertView
    }

    override fun getCount(): Int {
        return fitems!!.size + 1
    }

    fun updateHistory(history: ArrayList<String>) {
        for (s in history) {
            if (!objects.contains(s)) {
                objects.add(s)
            }
        }
        notifyDataSetChanged()
    }

    private inner class SubFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val results = FilterResults()
            val prefix = constraint.toString().lowercase()
            if (prefix == null || prefix.isEmpty()) {
                val list = CaseInsensitiveArrayList(baseItems)
                results.values = list
                results.count = list.size
            } else {
                openInSubView = true
                val list = CaseInsensitiveArrayList(objects)
                val nlist = CaseInsensitiveArrayList()
                for (sub in list) {
                    if (StringUtils.containsIgnoreCase(sub, prefix)) nlist.add(sub)
                    if (sub == prefix) openInSubView = false
                }
                if (openInSubView) {
                    nlist.add(context.getString(R.string.search_goto) + " " + prefix)
                }
                results.values = nlist
                results.count = nlist.size
            }
            return results
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            fitems = results.values as CaseInsensitiveArrayList
            clear()
            if (fitems != null) {
                addAll(fitems!!)
                notifyDataSetChanged()
            }
        }
    }
}
