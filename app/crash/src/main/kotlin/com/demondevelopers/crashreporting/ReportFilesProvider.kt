package com.demondevelopers.crashreporting

import android.annotation.TargetApi
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class ReportFilesProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        for (i in sFiles.indices) {
            sUriMatcher.addURI(AUTHORITY, sFiles[i], i)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val fileIndex = sUriMatcher.match(uri)
        return if (fileIndex != -1 && sPaths[fileIndex] != null) {
            FileCursor(
                sDisplayNames[fileIndex],
                File(sPaths[fileIndex])
            )
        } else null
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val fileIndex = sUriMatcher.match(uri)
        return if (fileIndex != -1 && sPaths[fileIndex] != null) {
            ParcelFileDescriptor.open(
                File(sPaths[fileIndex]),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        } else super.openFile(uri, mode)
    }

    override fun getType(uri: Uri): String? {
        val fileIndex = sUriMatcher.match(uri)
        return if (fileIndex != -1 && sPaths[fileIndex] != null) {
            sMimeTypes[fileIndex]
        } else null
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        val type = getType(uri)
        return type?.let { arrayOf(it) }
    }

    // -- provides extra information
    private class FileCursor(displayName: String?, file: File) : MatrixCursor(sColumns, 1) {
        init {
            val row = newRow()
            row.add(displayName)
            row.add(file.length())
        }

        companion object {
            private val sColumns = arrayOf("_display_name", "_size")
        }
    }

    // -- methods below are not required for our ContentProvider
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return 0
    }

    companion object {
        // Authority must match AndroidManifest!
        private const val AUTHORITY = "com.demondevelopers.crashreporting.filesprovider"
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        const val FILE_INDEX_SCREENSHOT = 0
        const val FILE_INDEX_EVENTLOG = 1
        const val FILE_INDEX_SYSTEMLOG = 2
        private val sFiles = arrayOf(
            "screenshot.jpg", "event-log.txt", "system-log.txt"
        )
        private val sDisplayNames = arrayOf(
            "ScreenShot.jpg", "EventLog.txt", "SystemLog.txt"
        )
        private val sMimeTypes = arrayOf(
            "image/jpeg", "text/plain", "text/plain"
        )
        private val sPaths = arrayOfNulls<String>(sFiles.size)
        fun setFilePath(fileIndex: Int, filePath: String?): Uri {
            sPaths[fileIndex] = filePath
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .path(sFiles[fileIndex])
                .build()
        }
    }
}
