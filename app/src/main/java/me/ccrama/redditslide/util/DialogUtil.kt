package me.ccrama.redditslide.util

import android.content.DialogInterface
import android.os.Environment
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileCallback
import com.afollestad.materialdialogs.files.folderChooser
import ltd.ucode.slide.R

object DialogUtil {
    @JvmStatic
    fun showErrorDialog(activity: AppCompatActivity, action: FileCallback) {
        showBaseChooserDialog(
            activity,
            R.string.err_something_wrong, R.string.err_couldnt_save_choose_new,
            action
        )
    }

    @JvmStatic
    fun showFirstDialog(activity: AppCompatActivity, action: FileCallback) {
        showBaseChooserDialog(
            activity,
            R.string.set_save_location, R.string.set_save_location_msg,
            action
        )
    }

    private fun showBaseChooserDialog(
        activity: AppCompatActivity,
        @StringRes titleId: Int,
        @StringRes messageId: Int,
        action: FileCallback
    ) {
        AlertDialog.Builder(activity)
            .setTitle(titleId)
            .setMessage(messageId)
            .setPositiveButton(
                android.R.string.ok
            ) { dialog: DialogInterface?, which: Int -> showFolderChooserDialog(activity, action) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showFolderChooserDialog(activity: AppCompatActivity?, action: FileCallback) {
        MaterialDialog(activity!!).show {
            folderChooser(activity,
                initialDirectory = Environment.getExternalStorageDirectory(),
                allowFolderCreation = true,
            ) { dialog, file ->
                action!!.invoke(dialog, file)
            }
        }
    }
}
