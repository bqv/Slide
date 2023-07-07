package me.ccrama.redditslide.Activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import ltd.ucode.slide.R
import ltd.ucode.slide.ui.BaseActivityAnim
import me.ccrama.redditslide.Fragments.SubredditListView

class SubredditSearch constructor() : BaseActivityAnim() {
    public override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.edit -> {
                run {
                    MaterialDialog(this@SubredditSearch)
                        .input(hintRes = R.string.discover_search, prefill = term,
                            waitForPositiveButton = false,
                        inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) { dialog: MaterialDialog, input: CharSequence ->
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = input.length >= 3
                        }
                        .positiveButton(R.string.search_all) { dialog: MaterialDialog ->
                            val inte: Intent =
                                Intent(this@SubredditSearch, SubredditSearch::class.java)
                            inte.putExtra(
                                "term",
                                dialog.getInputField().text.toString()
                            )
                            this@SubredditSearch.startActivity(inte)
                            finish()
                        }
                        .negativeButton(R.string.btn_cancel)
                        .show()
                }
                return true
            }

            else -> return false
        }
    }

    var term: String? = null
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        term = intent.extras!!.getString("term")
        applyColorTheme("")
        setContentView(R.layout.activity_fragmentinner)
        setupAppBar(R.id.toolbar, term, enableUpButton = true, colorToolbar = true)
        val f: Fragment = SubredditListView()
        val args: Bundle = Bundle()
        args.putString("id", term)
        f.arguments = args
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentcontent, f)
        fragmentTransaction.commit()
    }
}
