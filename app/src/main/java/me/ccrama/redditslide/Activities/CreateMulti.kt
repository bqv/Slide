/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.ccrama.redditslide.Activities

import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import ltd.ucode.slide.Authentication
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.UserSubscriptions
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.LogUtil
import net.dean.jraw.ApiException
import net.dean.jraw.http.MultiRedditUpdateRequest
import net.dean.jraw.http.NetworkException
import net.dean.jraw.managers.MultiRedditManager
import net.dean.jraw.models.MultiReddit
import net.dean.jraw.models.Subreddit
import java.util.Locale
import java.util.regex.Pattern

/**
 * This class handles creation of Multireddits.
 */
class CreateMulti : BaseActivityAnim() {
    private var subs: ArrayList<String>? = null
    private var adapter: CustomAdapter? = null
    private var title: EditText? = null
    private var recyclerView: RecyclerView? = null
    private var input: String? = null
    private var old: String? = null

    //Shows a dialog with all Subscribed subreddits and allows the user to select which ones to include in the Multireddit
    private lateinit var all: Array<String?>

    protected override fun onCreate(savedInstanceState: Bundle?) {
        overrideSwipeFromAnywhere()
        super.onCreate(savedInstanceState)
        applyColorTheme()
        setContentView(R.layout.activity_createmulti)
        setupAppBar(R.id.toolbar, "", enableUpButton = true, colorToolbar = true)
        findViewById<View>(R.id.add).setOnClickListener { showSelectDialog() }
        title = findViewById<View>(R.id.name) as EditText?
        subs = ArrayList()
        if (intent.hasExtra(EXTRA_MULTI)) {
            val multi: String = intent.extras!!.getString(EXTRA_MULTI)!!
            old = multi
            title!!.setText(multi.replace("%20", " "))
            UserSubscriptions.getMultireddits(object : UserSubscriptions.MultiCallback {
                override fun onComplete(multis: List<MultiReddit?>?) {
                    for (multiReddit in multis!!) {
                        if (multiReddit!!.displayName == multi) {
                            for (sub in multiReddit.subreddits) {
                                subs!!.add(sub.displayName.lowercase())
                            }
                        }
                    }
                }
            })
        }
        recyclerView = findViewById<View>(R.id.subslist) as RecyclerView?
        adapter = CustomAdapter(subs!!)
        //  adapter.setHasStableIds(true);
        recyclerView!!.adapter = adapter
        recyclerView!!.layoutManager = LinearLayoutManager(this)
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this@CreateMulti)
            .setTitle(R.string.general_confirm_exit)
            .setMessage(R.string.multi_save_option)
            .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, i: Int ->
                MultiredditOverview.multiActivity!!.finish()
                SaveMulti().execute()
            }
            .setNegativeButton(R.string.btn_no) { dialog: DialogInterface?, i: Int -> finish() }
            .show()
    }

    fun showSelectDialog() {
        //List of all subreddits of the multi
        val multiSubs: List<String?> = ArrayList(subs)
        val sorted: MutableList<String?> = ArrayList(subs)

        //Add all user subs that aren't already on the list
        for (s in UserSubscriptions.sort(UserSubscriptions.getSubscriptions(this))) {
            if (!sorted.contains(s)) sorted.add(s)
        }

        //Array of all subs
        all = arrayOfNulls(sorted.size)
        //Contains which subreddits are checked
        val checked = BooleanArray(all.size)


        //Remove special subreddits from list and store it in "all"
        var i = 0
        for (s in sorted) {
            if (s != "all" && s != "frontpage" && !s!!.contains("+") && !s.contains(".") && !s.contains(
                    "/m/"
                )
            ) {
                all[i] = s
                i++
            }
        }

        //Remove empty entries & store which subreddits are checked
        val list: MutableList<String?> = ArrayList()
        i = 0
        for (s in all) {
            if (s != null && !s.isEmpty()) {
                list.add(s)
                if (multiSubs.contains(s)) {
                    checked[i] = true
                }
                i++
            }
        }

        //Convert List back to Array
        all = list.filterNotNull().toTypedArray()
        val toCheck = ArrayList(subs)
        AlertDialog.Builder(this)
            .setMultiChoiceItems(
                all,
                checked
            ) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                if (!isChecked) {
                    toCheck.remove(all[which])
                } else {
                    toCheck.add(all[which])
                }
                Log.v(LogUtil.getTag(), "Done with " + all[which])
            }
            .setTitle(R.string.multireddit_selector)
            .setPositiveButton(getString(R.string.btn_add).uppercase(Locale.getDefault())) { dialog: DialogInterface?, which: Int ->
                subs = toCheck
                adapter = CustomAdapter(subs!!)
                recyclerView!!.adapter = adapter
            }
            .setNegativeButton(R.string.reorder_add_subreddit) { dialog: DialogInterface?, which: Int ->
                MaterialDialog(this@CreateMulti)
                    .title(R.string.reorder_add_subreddit)
                    .input(
                        hintRes = R.string.reorder_subreddit_name,
                        waitForPositiveButton = false
                    ) { _, raw ->
                        input = raw.toString()
                            .replace("\\s".toRegex(), "") //remove whitespace from input
                    }
                    .positiveButton(R.string.btn_add) { dialog: MaterialDialog ->
                        AsyncGetSubreddit().execute(input)
                    }
                    .negativeButton(R.string.btn_cancel)
                    .show()
            }
            .show()
    }

    private inner class AsyncGetSubreddit : AsyncTask<String?, Void?, Subreddit?>() {
        public override fun onPostExecute(subreddit: Subreddit?) {
            if (subreddit != null || input.equals(
                    "friends",
                    ignoreCase = true
                ) || input.equals("mod", ignoreCase = true)
            ) {
                subs!!.add(input!!)
                adapter!!.notifyDataSetChanged()
                recyclerView!!.smoothScrollToPosition(subs!!.size)
            }
        }

        override fun doInBackground(vararg params: String?): Subreddit? {
            return try {
                if (subs!!.contains(params[0])) null else Authentication.reddit!!.getSubreddit(
                    params[0]
                )
            } catch (e: Exception) {
                runOnUiThread(Runnable {
                    try {
                        AlertDialog.Builder(this@CreateMulti)
                            .setTitle(R.string.subreddit_err)
                            .setMessage(getString(R.string.subreddit_err_msg, params[0]))
                            .setPositiveButton(
                                R.string.btn_ok,
                                DialogInterface.OnClickListener { dialog: DialogInterface, which: Int -> dialog.dismiss() })
                            .setOnDismissListener(null)
                            .show()
                    } catch (ignored: Exception) {
                    }
                })
                null
            }
        }
    }

    /**
     * Responsible for showing a list of subreddits which are added to this Multireddit
     */
    inner class CustomAdapter(private val items: ArrayList<String>) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v =
                LayoutInflater.from(parent.context).inflate(R.layout.subforsublist, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val origPos = items[position]
            holder.text.text = origPos
            val colorView = holder.itemView.findViewById<View>(R.id.color)
            colorView.setBackgroundResource(R.drawable.circle)
            BlendModeUtil.tintDrawableAsModulate(colorView.background, Palette.getColor(origPos))
            holder.itemView.setOnClickListener {
                AlertDialog.Builder(this@CreateMulti)
                    .setTitle(R.string.really_remove_subreddit_title)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        subs!!.remove(origPos)
                        adapter = CustomAdapter(subs!!)
                        recyclerView!!.adapter = adapter
                    }
                    .setNegativeButton(R.string.btn_no, null)
                    .show()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView

            init {
                text = itemView.findViewById(R.id.name)
            }
        }
    }

    /**
     * Saves a Multireddit with applicable data in an async task
     */
    inner class SaveMulti : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val multiName = title!!.text.toString().replace(" ", "").replace("-", "_")
                val validName = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_]{2,20}$")
                val m = validName.matcher(multiName)
                if (!m.matches()) {
                    Log.v(LogUtil.getTag(), "Invalid multi name")
                    throw IllegalArgumentException(multiName)
                }
                if (old != null && !old!!.isEmpty() && old!!.replace(" ", "") != multiName) {
                    Log.v(LogUtil.getTag(), "Renaming")
                    MultiRedditManager(Authentication.reddit).rename(old, multiName)
                }
                Log.v(LogUtil.getTag(), "Create or Update, Name: $multiName")
                MultiRedditManager(Authentication.reddit).createOrUpdate(
                    MultiRedditUpdateRequest.Builder(
                        Authentication.name, multiName
                    ).subreddits(subs).build()
                )
                runOnUiThread(Runnable {
                    Log.v(LogUtil.getTag(), "Update Subreddits")
                    MultiredditOverview.multiActivity!!.finish()
                    UserSubscriptions.SyncMultireddits(this@CreateMulti).execute()
                })
                runOnUiThread(Runnable {
                    val context: Context = applicationContext
                    val text: CharSequence = getString(R.string.multi_saved_successfully)
                    val duration = Toast.LENGTH_SHORT
                    val toast = Toast.makeText(context, text, duration)
                    toast.show()
                })
            } catch (e: NetworkException) {
                runOnUiThread(Runnable {
                    var errorMsg: String? = getString(R.string.misc_err)
                    //Creating correct error message if the multireddit has more than 100 subs or its name already exists
                    if (e is ApiException) {
                        errorMsg =
                            getString(R.string.misc_err) + ": " + (e as ApiException).explanation +
                                    "\n" + getString(R.string.misc_retry)
                    } else if (e.response.statusCode == 409) {
                        //The HTTP status code returned when the name of the multireddit already exists or
                        //has more than 100 subs is 409
                        errorMsg = getString(R.string.multireddit_save_err)
                    }
                    AlertDialog.Builder(this@CreateMulti)
                        .setTitle(R.string.err_title)
                        .setMessage(errorMsg)
                        .setNeutralButton(R.string.btn_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }
                        .create()
                        .show()
                })
                e.printStackTrace()
            } catch (e: ApiException) {
                runOnUiThread(Runnable {
                    var errorMsg: String? = getString(R.string.misc_err)
                    if (e is ApiException) {
                        errorMsg = getString(R.string.misc_err) + ": " + e.explanation +
                                "\n" + getString(R.string.misc_retry)
                    } else if ((e as NetworkException).response.statusCode == 409) {
                        errorMsg = getString(R.string.multireddit_save_err)
                    }
                    AlertDialog.Builder(this@CreateMulti)
                        .setTitle(R.string.err_title)
                        .setMessage(errorMsg)
                        .setNeutralButton(R.string.btn_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }
                        .create()
                        .show()
                })
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                runOnUiThread(Runnable {
                    AlertDialog.Builder(this@CreateMulti)
                        .setTitle(R.string.multireddit_invalid_name)
                        .setMessage(R.string.multireddit_invalid_name_msg)
                        .setNeutralButton(R.string.btn_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }
                        .create()
                        .show()
                })
            }
            return null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_create_multi, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                AlertDialog.Builder(this@CreateMulti)
                    .setTitle(getString(R.string.delete_multireddit_title, title!!.text.toString()))
                    .setMessage(R.string.cannot_be_undone)
                    .setPositiveButton(R.string.btn_yes) { dialog: DialogInterface?, which: Int ->
                        MultiredditOverview.multiActivity!!.finish()
                        MaterialDialog(this@CreateMulti)
                            .title(R.string.deleting)
                            //.progress(true, 100)
                            .message(R.string.misc_please_wait)
                            .cancelable(false)
                            .show()
                        object : AsyncTask<Void?, Void?, Void?>() {
                            override fun doInBackground(vararg params: Void?): Void? {
                                try {
                                    MultiRedditManager(Authentication.reddit).delete(old)
                                    runOnUiThread(Runnable { UserSubscriptions.SyncMultireddits(this@CreateMulti)
                                        .execute() })
                                } catch (e: Exception) {
                                    runOnUiThread(Runnable {
                                        AlertDialog.Builder(this@CreateMulti)
                                            .setTitle(R.string.err_title)
                                            .setMessage(R.string.misc_err)
                                            .setNeutralButton(R.string.btn_ok) { dialogInterface: DialogInterface?, i: Int -> finish() }
                                            .create()
                                            .show()
                                    })
                                    e.printStackTrace()
                                }
                                return null
                            }
                        }.execute()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
                true
            }

            R.id.save -> {
                if (title!!.text.toString().isEmpty()) {
                    AlertDialog.Builder(this@CreateMulti)
                        .setTitle(R.string.multireddit_title_empty)
                        .setMessage(R.string.multireddit_title_empty_msg)
                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface, which: Int ->
                            dialog.dismiss()
                            title!!.requestFocus()
                        }
                        .show()
                } else if (subs!!.isEmpty()) {
                    AlertDialog.Builder(this@CreateMulti)
                        .setTitle(R.string.multireddit_no_subs)
                        .setMessage(R.string.multireddit_no_subs_msg)
                        .setPositiveButton(R.string.btn_ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                        .show()
                } else {
                    SaveMulti().execute()
                }
                true
            }

            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> false
        }
    }

    companion object {
        const val EXTRA_MULTI = "multi"
    }
}
