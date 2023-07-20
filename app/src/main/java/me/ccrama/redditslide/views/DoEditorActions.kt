package me.ccrama.redditslide.views

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.text.Editable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.snackbar.Snackbar
import gun0912.tedbottompicker.TedBottomPicker
import gun0912.tedbottompicker.TedBottomSheetDialogFragment
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.main.MainActivity
import me.ccrama.redditslide.Activities.Draw
import me.ccrama.redditslide.Drafts
import me.ccrama.redditslide.ImgurAlbum.UploadImgur
import me.ccrama.redditslide.ImgurAlbum.UploadImgurAlbum
import me.ccrama.redditslide.SpoilerRobotoTextView
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.util.DisplayUtil
import me.ccrama.redditslide.util.KeyboardUtil
import me.ccrama.redditslide.util.ProUtil
import me.ccrama.redditslide.util.SubmissionParser
import org.apache.commons.text.StringEscapeUtils
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object DoEditorActions {
    @JvmStatic
    fun doActions(
        editText: EditText, baseView: View,
        fm: FragmentManager, a: Activity, oldComment: String?,
        authors: Array<String>?
    ) {
        baseView.findViewById<View>(R.id.bold).setOnClickListener {
            if (editText.hasSelection()) {
                wrapString(
                    "**",
                    editText
                ) //If the user has text selected, wrap that text in the symbols
            } else {
                //If the user doesn't have text selected, put the symbols around the cursor's position
                val pos = editText.selectionStart
                editText.text.insert(pos, "**")
                editText.text.insert(pos + 1, "**")
                editText.setSelection(pos + 2) //put the cursor between the symbols
            }
        }
        if (baseView.findViewById<View?>(R.id.author) != null) {
            if (!authors.isNullOrEmpty()) {
                baseView.findViewById<View>(R.id.author)
                    .setOnClickListener {
                        if (authors.size == 1) {
                            val author = "/u/" + authors[0]
                            insertBefore(author, editText)
                        } else {
                            AlertDialog.Builder(a)
                                .setTitle(R.string.authors_above)
                                .setItems(authors) { dialog: DialogInterface?, which: Int ->
                                    val author: String = "/u/" + authors.get(which)
                                    insertBefore(author, editText)
                                }
                                .setNeutralButton(R.string.btn_cancel, null)
                                .show()
                        }
                    }
            } else {
                baseView.findViewById<View>(R.id.author).visibility = View.GONE
            }
        }
        baseView.findViewById<View>(R.id.italics).setOnClickListener {
            if (editText.hasSelection()) {
                wrapString(
                    "*",
                    editText
                ) //If the user has text selected, wrap that text in the symbols
            } else {
                //If the user doesn't have text selected, put the symbols around the cursor's position
                val pos = editText.selectionStart
                editText.text.insert(pos, "*")
                editText.text.insert(pos + 1, "*")
                editText.setSelection(pos + 1) //put the cursor between the symbols
            }
        }
        baseView.findViewById<View>(R.id.strike).setOnClickListener {
            if (editText.hasSelection()) {
                wrapString(
                    "~~",
                    editText
                ) //If the user has text selected, wrap that text in the symbols
            } else {
                //If the user doesn't have text selected, put the symbols around the cursor's position
                val pos = editText.selectionStart
                editText.text.insert(pos, "~~")
                editText.text.insert(pos + 2, "~~")
                editText.setSelection(pos + 2) //put the cursor between the symbols
            }
        }
        baseView.findViewById<View>(R.id.spoiler).setOnClickListener {
            if (editText.hasSelection()) {
                wrapString(
                    ">!",
                    "!<",
                    editText
                ) //If the user has text selected, wrap that text in the symbols
            } else {
                //If the user doesn't have text selected, put the symbols around the cursor's position
                val pos = editText.selectionStart
                editText.text.insert(pos, ">!")
                editText.text.insert(pos + 2, "!<")
                editText.setSelection(pos + 2) //put the cursor between the symbols
            }
        }
        baseView.findViewById<View>(R.id.savedraft)
            .setOnClickListener {
                Drafts.addDraft(editText.text.toString())
                val s: Snackbar = Snackbar.make(
                    baseView.findViewById<View>(R.id.savedraft), "Draft saved",
                    Snackbar.LENGTH_SHORT
                )
                val view: View = s.view
                val tv =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                tv.setTextColor(Color.WHITE)
                s.setAction(R.string.btn_discard) { Drafts.deleteDraft(Drafts.getDrafts().size - 1) }
                s.show()
            }
        baseView.findViewById<View>(R.id.draft).setOnClickListener {
            val drafts: ArrayList<String?> = Drafts.getDrafts()
            drafts.reverse()
            val draftText = arrayOfNulls<String>(drafts.size)
            for (i in drafts.indices) {
                draftText[i] = drafts[i]
            }
            if (drafts.isEmpty()) {
                AlertDialog.Builder(a)
                    .setTitle(R.string.dialog_no_drafts)
                    .setMessage(R.string.dialog_no_drafts_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
            } else {
                AlertDialog.Builder(a)
                    .setTitle(R.string.choose_draft)
                    .setItems(draftText) { dialog: DialogInterface?, which: Int ->
                        editText.setText(
                            editText.text.toString() + draftText.get(which)
                        )
                    }
                    .setNeutralButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.manage_drafts) { dialog: DialogInterface?, which: Int ->
                        val selected = BooleanArray(drafts.size)
                        AlertDialog.Builder(a)
                            .setTitle(R.string.choose_draft)
                            .setNeutralButton(R.string.btn_cancel, null)
                            .setNegativeButton(R.string.btn_delete) { dialog1: DialogInterface?, which1: Int ->
                                AlertDialog.Builder(a)
                                    .setTitle(R.string.really_delete_drafts)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.btn_yes) { dialog11: DialogInterface?, which11: Int ->
                                        val draf: ArrayList<String?> = ArrayList()
                                        for (i in draftText.indices) {
                                            if (!selected.get(i)) {
                                                draf.add(draftText.get(i))
                                            }
                                        }
                                        Drafts.save(draf)
                                    }
                                    .setNegativeButton(R.string.btn_no, null)
                                    .show()
                            }
                            .setMultiChoiceItems(draftText, selected) { _, index, isChecked ->
                                selected[index] = isChecked
                            }
                            .show()
                    }
                    .show()
            }
        }
        baseView.findViewById<View>(R.id.imagerep).setOnClickListener { v: View? ->
            e = editText.text
            sStart = editText.selectionStart
            sEnd = editText.selectionEnd
            val tedBottomPicker: TedBottomSheetDialogFragment = TedBottomPicker.with(
                fm.primaryNavigationFragment!!.activity
            )
                .setOnMultiImageSelectedListener { uri: List<Uri?> ->
                    handleImageIntent(uri, editText, a)
                } //.setLayoutResource(R.layout.image_sheet_dialog)
                .setTitle("Choose a photo")
                .create()
            tedBottomPicker.show(fm)
            KeyboardUtil.hideKeyboard(editText.context, editText.windowToken, 0)
        }
        baseView.findViewById<View>(R.id.draw).setOnClickListener { v: View? ->
            if (SettingValues.isPro) {
                doDraw(a, editText, fm)
            } else {
                val b: AlertDialog.Builder =
                    ProUtil.proUpgradeMsg(a, R.string.general_cropdraw_ispro)
                        .setNegativeButton(
                            R.string.btn_no_thanks
                        ) { dialog: DialogInterface, whichButton: Int -> dialog.dismiss() }
                if (SettingValues.previews > 0) {
                    b.setNeutralButton(
                        a.getString(R.string.pro_previews, SettingValues.previews)
                    ) { dialog: DialogInterface?, which: Int ->
                        SettingValues.decreasePreviewsLeft()
                        doDraw(a, editText, fm)
                    }
                }
                b.show()
            }
        }
        /*todo baseView.findViewById(R.id.superscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertBefore("^", editText);
            }
        });*/baseView.findViewById<View>(R.id.size)
            .setOnClickListener { insertBefore("#", editText) }
        baseView.findViewById<View>(R.id.quote).setOnClickListener {
            if (oldComment != null) {
                val showText = TextView(a)
                showText.text =
                    StringEscapeUtils.unescapeHtml4(oldComment) // text we get is escaped, we don't want that
                showText.setTextIsSelectable(true)
                val sixteen: Int = DisplayUtil.dpToPxVertical(24)
                showText.setPadding(sixteen, 0, sixteen, 0)
                val builder = MaterialDialog(a)
                builder.customView(view = showText, scrollable = false)
                    .title(R.string.editor_actions_quote_comment)
                    .cancelable(true)
                    .positiveButton(R.string.btn_select) { dialog: MaterialDialog ->
                        var selected = showText.text
                            .toString()
                            .substring(showText.selectionStart, showText.selectionEnd)
                        if (selected.isEmpty()) {
                            selected = StringEscapeUtils.unescapeHtml4(oldComment)
                        }
                        insertBefore(
                            "> " + selected.replace(
                                "\n".toRegex(),
                                "\n> "
                            ) + "\n\n", editText
                        )
                    }
                    .negativeButton(R.string.btn_cancel)
                    .show()
                KeyboardUtil.hideKeyboard(editText.context, editText.windowToken, 0)
            } else {
                insertBefore("> ", editText)
            }
        }
        baseView.findViewById<View>(R.id.bulletlist)
            .setOnClickListener {
                val start = editText.selectionStart
                val end = editText.selectionEnd
                var selected = editText.text.toString()
                    .substring(Math.min(start, end), Math.max(start, end))
                if (!selected.isEmpty()) {
                    selected = selected.replaceFirst("^[^\n]".toRegex(), "* $0")
                        .replace("\n".toRegex(), "\n* ")
                    editText.text.replace(Math.min(start, end), Math.max(start, end), selected)
                } else {
                    insertBefore("* ", editText)
                }
            }
        baseView.findViewById<View>(R.id.numlist).setOnClickListener {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            var selected =
                editText.text.toString().substring(Math.min(start, end), Math.max(start, end))
            if (!selected.isEmpty()) {
                selected = selected.replaceFirst("^[^\n]".toRegex(), "1. $0")
                    .replace("\n".toRegex(), "\n1. ")
                editText.text.replace(start.coerceAtMost(end), start.coerceAtLeast(end), selected)
            } else {
                insertBefore("1. ", editText)
            }
        }
        baseView.findViewById<View>(R.id.preview).setOnClickListener {
            val extensions = listOf(
                TablesExtension.create(),
                StrikethroughExtension.create()
            )
            val parser = Parser.builder().extensions(extensions).build()
            val renderer = HtmlRenderer.builder().extensions(extensions).build()
            val document = parser.parse(editText.text.toString())
            val html: String = renderer.render(document)
            val inflater = a.layoutInflater
            val dialoglayout = inflater.inflate(R.layout.parent_comment_dialog, null)
            setViews(
                html, "NO sub",
                dialoglayout.findViewById(R.id.firstTextView),
                dialoglayout.findViewById(R.id.commentOverflow)
            )
            AlertDialog.Builder(a)
                .setView(dialoglayout)
                .show()
        }
        baseView.findViewById<View>(R.id.link).setOnClickListener {
            val inflater = LayoutInflater.from(a)
            val layout = inflater.inflate(R.layout.insert_link, null) as LinearLayout
            val attrs = intArrayOf(R.attr.fontColor)
            val ta: TypedArray = baseView.context
                .obtainStyledAttributes(
                    ColorPreferences(baseView.context).fontStyle
                        .baseId, attrs
                )
            ta.recycle()
            var selectedText = ""
            //if the user highlighted text before inputting a URL, use that text for the descriptionBox
            if (editText.hasSelection()) {
                val startSelection = editText.selectionStart
                val endSelection = editText.selectionEnd
                selectedText = editText.text.toString().substring(startSelection, endSelection)
            }
            val selectedTextNotEmpty = selectedText.isNotEmpty()
            val dialog = MaterialDialog(editText.context)
                .title(R.string.editor_title_link)
                .customView(view = layout, scrollable = false)
                .positiveButton(R.string.editor_action_link, ) { dialog: MaterialDialog ->
                    val urlBox = dialog.findViewById<View>(R.id.url_box) as EditText
                    val textBox = dialog.findViewById<View>(R.id.text_box) as EditText
                    dialog.dismiss()
                    val s = ("[${textBox.text}](${urlBox.text})")
                    val start = editText.selectionStart.coerceAtLeast(0)
                    val end = editText.selectionEnd.coerceAtLeast(0)
                    editText.text.insert(start.coerceAtLeast(end), s)

                    //delete the selected text to avoid duplication
                    if (selectedTextNotEmpty) {
                        editText.text.delete(start, end)
                    }
                }

            //Tint the hint text if the base theme is Sepia
            if (SettingValues.currentTheme == 5) {
                (dialog.findViewById<View>(R.id.url_box) as EditText).setHintTextColor(
                    ContextCompat.getColor(dialog.context, R.color.md_grey_600)
                )
                (dialog.findViewById<View>(R.id.text_box) as EditText).setHintTextColor(
                    ContextCompat.getColor(dialog.context, R.color.md_grey_600)
                )
            }

            //use the selected text as the text for the link
            if (!selectedText.isEmpty()) {
                (dialog.findViewById<View>(R.id.text_box) as EditText).setText(selectedText)
            }
            dialog.show()
        }
        try {
            (editText as ImageInsertEditText)
                .setImageSelectedCallback { content: Uri, mimeType: String ->
                    e = editText.text
                    sStart = editText.getSelectionStart()
                    sEnd = editText.getSelectionEnd()
                    handleImageIntent(object : ArrayList<Uri?>() {
                        init {
                            add(content)
                        }
                    }, editText, a)
                }
        } catch (e: Exception) {
            //if thrown, there is likely an issue implementing this on the user's version of Android. There shouldn't be an issue, but just in case
        }
    }

    var e: Editable? = null
    var sStart = 0
    var sEnd = 0

    fun doDraw(a: Activity?, editText: EditText, fm: FragmentManager) {
        val intent = Intent(a, Draw::class.java)
        KeyboardUtil.hideKeyboard(editText.context, editText.windowToken, 0)
        e = editText.text
        val tedBottomPicker: TedBottomSheetDialogFragment = TedBottomPicker.with(
            fm.primaryNavigationFragment!!.activity
        )
            .setOnMultiImageSelectedListener { uri: List<Uri> ->
                Draw.uri = uri[0]
                val auxiliary: Fragment = AuxiliaryFragment()
                sStart = editText.selectionStart
                sEnd = editText.selectionEnd
                fm.beginTransaction().add(auxiliary, "IMAGE_UPLOAD").commit()
                fm.executePendingTransactions()
                auxiliary.startActivityForResult(intent, MainActivity.CHOOSE_IMAGE_RESULT)
            } //.setLayoutResource(R.layout.image_sheet_dialog)
            .setTitle("Choose a photo")
            .create()
        tedBottomPicker.show(fm)
    }

    fun getImageLink(b: Bitmap): String {
        val baos = ByteArrayOutputStream()
        b.compress(
            Bitmap.CompressFormat.JPEG, 100,
            baos
        ) // Not sure whether this should be jpeg or png, try both and see which works best
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    fun insertBefore(wrapText: String?, editText: EditText) {
        val start = Math.max(editText.selectionStart, 0)
        val end = Math.max(editText.selectionEnd, 0)
        editText.text.insert(Math.min(start, end), wrapText)
    }
    /* not using this method anywhere ¯\_(ツ)_/¯ */ //    public static void wrapNewline(String wrapText, EditText editText) {
    //        int start = Math.max(editText.getSelectionStart(), 0);
    //        int end = Math.max(editText.getSelectionEnd(), 0);
    //        String s = editText.getText().toString().substring(Math.min(start, end), Math.max(start, end));
    //        s = s.replace("\n", "\n" + wrapText);
    //        editText.getText().replace(Math.min(start, end), Math.max(start, end), s);
    //    }
    /**
     * Wrap selected text in one or multiple characters, handling newlines and spaces properly for markdown
     * @param wrapText Character(s) to wrap the selected text in
     * @param editText EditText
     */
    fun wrapString(wrapText: String, editText: EditText) {
        wrapString(wrapText, wrapText, editText)
    }

    /**
     * Wrap selected text in one or multiple characters, handling newlines, spaces, >s properly for markdown,
     * with different start and end text.
     * @param startWrap Character(s) to start wrapping with
     * @param endWrap Character(s) to close wrapping with
     * @param editText EditText
     */
    fun wrapString(startWrap: String, endWrap: String, editText: EditText) {
        val start = Math.max(editText.selectionStart, 0)
        val end = Math.max(editText.selectionEnd, 0)
        var selected =
            editText.text.toString().substring(Math.min(start, end), Math.max(start, end))
        // insert the wrapping character inside any selected spaces and >s because they stop markdown formatting
        // we use replaceFirst because anchors (\A, \Z) aren't consumed
        selected = selected.replaceFirst("\\A[\\n> ]*".toRegex(), "$0$startWrap")
            .replaceFirst("[\\n> ]*\\Z".toRegex(), "$endWrap$0")
        // 2+ newlines stop formatting, so we do the formatting on each instance of text surrounded by 2+ newlines
        /* in case anyone needs to understand this in the future:
         * ([^\n> ]) captures any character that isn't a newline, >, or space
         * (\n[> ]*){2,} captures any number of two or more newlines with any combination of spaces or >s since markdown ignores those by themselves
         * (?=[^\n> ]) performs a lookahead and ensures there's a character that isn't a newline, >, or space
         */selected = selected.replace(
            "([^\\n> ])(\\n[> ]*){2,}(?=[^\\n> ])".toRegex(),
            "$1$endWrap$2$startWrap"
        )
        editText.text.replace(start, end, selected)
    }

    private fun setViews(
        rawHTML: String, subredditName: String,
        firstTextView: SpoilerRobotoTextView, commentOverflow: CommentOverflow
    ) {
        if (rawHTML.isEmpty()) {
            return
        }
        val blocks: List<String> = SubmissionParser.getBlocks(rawHTML)
        var startIndex = 0
        // the <div class="md"> case is when the body contains a table or code block first
        if (blocks.get(0) != "<div class=\"md\">") {
            firstTextView.visibility = View.VISIBLE
            firstTextView.setTextHtml(blocks[0], subredditName)
            firstTextView.setLinkTextColor(
                ColorPreferences(firstTextView.context).getColor(subredditName)
            )
            startIndex = 1
        } else {
            firstTextView.text = ""
            firstTextView.visibility = View.GONE
        }
        if (blocks.size > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subredditName)
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size), subredditName)
            }
        } else {
            commentOverflow.removeAllViews()
        }
    }

    fun handleImageIntent(uris: List<Uri?>, ed: EditText, c: Context?) {
        handleImageIntent(uris, ed.text, c)
    }

    fun handleImageIntent(uris: List<Uri?>, ed: Editable?, c: Context?) {
        if (uris.size == 1) {
            // Get the Image from data (single image)
            try {
                UploadImgurDEA((c)!!).execute(uris[0])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            //Multiple images
            try {
                UploadImgurAlbumDEA((c)!!).execute(*uris.toTypedArray<Uri?>())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class AuxiliaryFragment : Fragment() {
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (data != null && data.data != null) {
                handleImageIntent(object : ArrayList<Uri?>() {
                    init {
                        add(data.data)
                    }
                }, e, context)
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            }
        }
    }

    private class UploadImgurDEA(c: Context) : UploadImgur() {
        init {
            this.c = c
            dialog = MaterialDialog(c)
                .title(R.string.editor_uploading_image)
                //.progress(false, 100)
                .cancelable(false)
                .also { it.show() }
        }

        override fun onPostExecute(result: JSONObject?) {
            dialog!!.dismiss()
            try {
                val attrs = intArrayOf(R.attr.fontColor)
                val ta: TypedArray = c!!.obtainStyledAttributes(
                    ColorPreferences(c).fontStyle.baseId,
                    attrs
                )
                val url = result!!.getJSONObject("data").getString("link")
                val layout = LinearLayout(c)
                layout.orientation = LinearLayout.VERTICAL
                val titleBox = TextView(c)
                titleBox.text = url
                layout.addView(titleBox)
                titleBox.isEnabled = false
                titleBox.setTextColor(ta.getColor(0, Color.WHITE))
                val descriptionBox = EditText(c)
                descriptionBox.setHint(R.string.editor_title)
                descriptionBox.isEnabled = true
                descriptionBox.setTextColor(ta.getColor(0, Color.WHITE))
                KeyboardUtil.toggleKeyboard(
                    c,
                    InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
                )
                if (e != null) {
                    descriptionBox.setText(e.toString().substring(sStart, sEnd))
                }
                ta.recycle()
                val sixteen: Int = DisplayUtil.dpToPxVertical(16)
                layout.setPadding(sixteen, sixteen, sixteen, sixteen)
                layout.addView(descriptionBox)
                MaterialDialog(c!!)
                    .title(R.string.editor_title_link)
                    .customView(view = layout, scrollable = false)
                    .positiveButton(R.string.editor_action_link) { dialog: MaterialDialog ->
                        dialog.dismiss()
                        var s: String = ("[${descriptionBox.text}]($url)")
                        if (descriptionBox.text.toString().trim { it <= ' ' }.isEmpty()) {
                            s = "$url "
                        }
                        val start = sStart.coerceAtLeast(0)
                        val end = sEnd.coerceAtLeast(0)
                        if (e != null) {
                            e!!.insert(start.coerceAtLeast(end), s)
                            e!!.delete(start, end)
                            e = null
                        }
                        sStart = 0
                        sEnd = 0
                    }
                    .cancelOnTouchOutside(false)
                    .show()
            } catch (e: Exception) {
                AlertDialog.Builder(c!!)
                    .setTitle(R.string.err_title)
                    .setMessage(R.string.editor_err_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
                e.printStackTrace()
            }
        }
    }

    private class UploadImgurAlbumDEA(c: Context) : UploadImgurAlbum() {
        init {
            this.c = c
            dialog = MaterialDialog(c)
                .title(R.string.editor_uploading_image)
                //.progress(false, 100)
                .cancelable(false)
                .also { it.show() }
        }

        override fun onPostExecute(result: String?) {
            dialog!!.dismiss()
            try {
                val attrs = intArrayOf(R.attr.fontColor)
                val ta: TypedArray = c!!.obtainStyledAttributes(
                    ColorPreferences(c).fontStyle.baseId,
                    attrs
                )
                val layout = LinearLayout(c)
                layout.orientation = LinearLayout.VERTICAL
                val titleBox = TextView(c)
                titleBox.text = finalUrl
                layout.addView(titleBox)
                titleBox.isEnabled = false
                titleBox.setTextColor(ta.getColor(0, Color.WHITE))
                val descriptionBox = EditText(c)
                descriptionBox.setHint(R.string.editor_title)
                descriptionBox.isEnabled = true
                descriptionBox.setTextColor(ta.getColor(0, Color.WHITE))
                if (e != null) {
                    descriptionBox.setText(e.toString().substring(sStart, sEnd))
                }
                ta.recycle()
                val sixteen: Int = DisplayUtil.dpToPxVertical(16)
                layout.setPadding(sixteen, sixteen, sixteen, sixteen)
                layout.addView(descriptionBox)
                MaterialDialog(c!!)
                    .title(R.string.editor_title_link)
                    .customView(view = layout, scrollable = false)
                    .positiveButton(R.string.editor_action_link) { dialog: MaterialDialog ->
                        dialog.dismiss()
                        val s = ("["
                                + descriptionBox.text.toString()
                                + "]("
                                + finalUrl
                                + ")")
                        val start = sStart.coerceAtLeast(0)
                        val end = sEnd.coerceAtLeast(0)
                        e!!.insert(start.coerceAtLeast(end), s)
                        e!!.delete(start, end)
                        e = null
                        sStart = 0
                        sEnd = 0
                    }
                    .cancelOnTouchOutside(false)
                    .show()
            } catch (e: Exception) {
                AlertDialog.Builder(c!!)
                    .setTitle(R.string.err_title)
                    .setMessage(R.string.editor_err_msg)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show()
                e.printStackTrace()
            }
        }
    }
}
