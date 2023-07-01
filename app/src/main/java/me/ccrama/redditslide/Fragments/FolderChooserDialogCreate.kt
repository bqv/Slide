package me.ccrama.redditslide.Fragments

import android.Manifest
import android.R
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.ListCallback
import java.io.File
import java.io.Serializable
import java.util.Collections

class FolderChooserDialogCreate : DialogFragment(), ListCallback {
    private var parentFolder: File? = null
    private var parentContents: Array<File>? = null
    private var canGoUp = false
    private var callback: FolderCallback? = null

    val contentsArray: Array<String?>
        get() {
            if (parentContents == null) {
                return if (canGoUp) {
                    arrayOf(builder.goUpLabel)
                } else arrayOf()
            }
            val results = arrayOfNulls<String>(parentContents!!.size + if (canGoUp) 1 else 0)
            if (canGoUp) {
                results[0] = builder.goUpLabel
            }
            for (i in parentContents!!.indices) {
                results[if (canGoUp) i + 1 else i] = parentContents!![i].name
            }
            return results
        }

    fun listFiles(): Array<File>? {
        val contents = parentFolder!!.listFiles()
        val results: MutableList<File> = ArrayList()
        if (contents != null) {
            for (fi in contents) {
                if (fi.isDirectory) {
                    results.add(fi)
                }
            }
            Collections.sort(results, FolderSorter())
            return results.toTypedArray()
        }
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val permission = when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.TIRAMISU..Int.MAX_VALUE -> Manifest.permission.READ_MEDIA_IMAGES
            in Build.VERSION_CODES.M..Build.VERSION_CODES.S_V2 -> Manifest.permission.READ_EXTERNAL_STORAGE
            else -> null
        }
        if (permission != null
            && ActivityCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED
        ) {
            return MaterialDialog.Builder(requireActivity())
                .title(com.afollestad.materialdialogs.commons.R.string.md_error_label)
                .content(com.afollestad.materialdialogs.commons.R.string.md_storage_perm_error)
                .positiveText(R.string.ok)
                .build()
        }
        check(!(arguments == null || !requireArguments().containsKey("builder"))) { "You must create a FolderChooserDialog using the Builder." }
        if (!requireArguments().containsKey("current_path")) {
            requireArguments().putString("current_path", builder.initialPath)
        }
        parentFolder = File(requireArguments().getString("current_path"))
        checkIfCanGoUp()
        parentContents = listFiles()
        val builder = MaterialDialog.Builder(requireActivity())
            .typeface(builder.mediumFont, builder.regularFont)
            .title(parentFolder!!.absolutePath)
            .items(*contentsArray)
            .itemsCallback(this)
            .onPositive { dialog: MaterialDialog, which: DialogAction? ->
                dialog.dismiss()
                callback!!.onFolderSelection(
                    this@FolderChooserDialogCreate,
                    parentFolder!!,
                    builder.isSaveToLocation
                )
            }
            .onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }
            .autoDismiss(false)
            .positiveText(builder.chooseButton)
            .negativeText(builder.cancelButton)
        if (this.builder.allowNewFolder) {
            builder.neutralText(this.builder.newFolderButton)
            builder.onNeutral { dialog: MaterialDialog?, which: DialogAction? -> createNewFolder() }
        }
        if ("/" == this.builder.initialPath) {
            canGoUp = false
        }
        return builder.build()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (callback != null) {
            callback!!.onFolderChooserDismissed(this)
        }
    }

    private fun createNewFolder() {
        MaterialDialog.Builder(requireActivity())
            .title(builder.newFolderButton)
            .input(0, 0, false) { dialog: MaterialDialog?, input: CharSequence ->
                val newFile = File(parentFolder, input.toString())
                if (newFile.mkdir()) {
                    reload()
                } else {
                    val msg = ("Unable to create folder "
                            + newFile.absolutePath
                            + ", make sure you have the WRITE_EXTERNAL_STORAGE permission or root permissions.")
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    override fun onSelection(materialDialog: MaterialDialog, view: View, i: Int, s: CharSequence) {
        if (canGoUp && i == 0) {
            parentFolder = parentFolder!!.parentFile
            if (parentFolder != null && parentFolder!!.absolutePath == "/storage/emulated") {
                parentFolder = parentFolder!!.parentFile
            }
            if (parentFolder != null) {
                canGoUp = parentFolder!!.parent != null
            }
        } else {
            parentFolder = parentContents!![if (canGoUp) i - 1 else i]
            canGoUp = true
            if (parentFolder!!.absolutePath == "/storage/emulated") {
                parentFolder = Environment.getExternalStorageDirectory()
            }
        }
        reload()
    }

    private fun checkIfCanGoUp() {
        canGoUp = try {
            parentFolder!!.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray().size > 1
        } catch (e: IndexOutOfBoundsException) {
            false
        }
    }

    private fun reload() {
        parentContents = listFiles()
        val dialog = dialog as MaterialDialog?
        dialog!!.setTitle(parentFolder!!.absolutePath)
        requireArguments().putString("current_path", parentFolder!!.absolutePath)
        dialog.setItems(*contentsArray)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback =
            if (activity is FolderCallback) {
                activity as FolderCallback?
            } else if (parentFragment is FolderCallback) {
                parentFragment as FolderCallback?
            } else {
                throw IllegalStateException(
                    "FolderChooserDialog needs to be shown from an Activity/Fragment implementing FolderCallback."
                )
            }
    }

    fun show(fragmentActivity: FragmentActivity) {
        show(fragmentActivity.supportFragmentManager)
    }

    fun show(fragmentManager: FragmentManager) {
        val tag = builder.tag
        val frag = fragmentManager.findFragmentByTag(tag)
        if (frag != null) {
            (frag as DialogFragment).dismiss()
            fragmentManager.beginTransaction().remove(frag).commit()
        }
        show(fragmentManager, tag)
    }

    private val builder: Builder
        private get() = requireArguments().getSerializable("builder") as Builder

    interface FolderCallback {
        fun onFolderSelection(
            dialog: FolderChooserDialogCreate,
            folder: File, isSaveToLocation: Boolean
        )

        fun onFolderChooserDismissed(dialog: FolderChooserDialogCreate)
    }

    class Builder(@field:Transient val context: Context) : Serializable {
        var isSaveToLocation = false

        @StringRes
        var chooseButton: Int

        @StringRes
        var cancelButton: Int
        var initialPath: String?
        var tag: String? = null
        var allowNewFolder = false

        @StringRes
        var newFolderButton = 0
        var goUpLabel: String
        var mediumFont: String? = null
        var regularFont: String? = null

        init {
            chooseButton = com.afollestad.materialdialogs.commons.R.string.md_choose_label
            cancelButton = R.string.cancel
            goUpLabel = "..."
            initialPath = Environment.getExternalStorageDirectory().absolutePath
        }

        fun typeface(medium: String?, regular: String?): Builder {
            mediumFont = medium
            regularFont = regular
            return this
        }

        fun chooseButton(@StringRes text: Int): Builder {
            chooseButton = text
            return this
        }

        fun cancelButton(@StringRes text: Int): Builder {
            cancelButton = text
            return this
        }

        fun goUpLabel(text: String): Builder {
            goUpLabel = text
            return this
        }

        fun allowNewFolder(allow: Boolean, @StringRes buttonLabel: Int): Builder {
            var buttonLabel = buttonLabel
            allowNewFolder = allow
            if (buttonLabel == 0) {
                buttonLabel = com.afollestad.materialdialogs.commons.R.string.new_folder
            }
            newFolderButton = buttonLabel
            return this
        }

        fun initialPath(initialPath: String?): Builder {
            var initialPath = initialPath
            if (initialPath == null) {
                initialPath = File.separator
            }
            this.initialPath = initialPath
            return this
        }

        fun tag(tag: String?): Builder {
            var tag = tag
            if (tag == null) {
                tag = DEFAULT_TAG
            }
            this.tag = tag
            return this
        }

        fun isSaveToLocation(isSaveToLocation: Boolean): Builder {
            this.isSaveToLocation = isSaveToLocation
            return this
        }

        fun build(): FolderChooserDialogCreate {
            val dialog = FolderChooserDialogCreate()
            val args = Bundle()
            args.putSerializable("builder", this)
            dialog.arguments = args
            return dialog
        }

        fun show(fragmentManager: FragmentManager): FolderChooserDialogCreate {
            val dialog = build()
            dialog.show(fragmentManager)
            return dialog
        }

        fun show(fragmentActivity: FragmentActivity): FolderChooserDialogCreate {
            return show(fragmentActivity.supportFragmentManager)
        }
    }

    private class FolderSorter : Comparator<File> {
        override fun compare(lhs: File, rhs: File): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

    companion object {
        private const val DEFAULT_TAG = "[MD_FOLDER_SELECTOR]"
    }
}
