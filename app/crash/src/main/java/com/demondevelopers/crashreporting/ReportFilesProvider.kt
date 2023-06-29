package com.demondevelopers.crashreporting;

import java.io.File;
import java.io.FileNotFoundException;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;


public class ReportFilesProvider extends ContentProvider
{
	// Authority must match AndroidManifest!
	private static String     AUTHORITY   = "com.demondevelopers.example.crashreporting.filesprovider";

	private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	public static final int FILE_INDEX_SCREENSHOT = 0;
	public static final int FILE_INDEX_EVENTLOG   = 1;
	public static final int FILE_INDEX_SYSTEMLOG  = 2;


	private static final String[] sFiles = {
		"screenshot.jpg", "event-log.txt", "system-log.txt"
	};

	private static final String[] sDisplayNames = {
		"ScreenShot.jpg", "EventLog.txt", "SystemLog.txt"
	};

	private static final String[] sMimeTypes = {
		"image/jpeg", "text/plain", "text/plain"
	};

	private static final String[] sPaths = new String[sFiles.length];


	public static Uri setFilePath(int fileIndex, String filePath)
	{
		sPaths[fileIndex] = filePath;
		return new Uri.Builder()
			.scheme(ContentResolver.SCHEME_CONTENT)
			.authority(AUTHORITY)
			.path(sFiles[fileIndex])
			.build();
	}

	@Override
	public boolean onCreate()
	{
		for(int i = 0; i < sFiles.length; i++){
			sUriMatcher.addURI(AUTHORITY, sFiles[i], i);
		}
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		int fileIndex = sUriMatcher.match(uri);
		if(fileIndex != -1 && sPaths[fileIndex] != null){
			return new FileCursor(sDisplayNames[fileIndex], new File(sPaths[fileIndex]));
		}
		return null;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
	{
		int fileIndex = sUriMatcher.match(uri);
		if(fileIndex != -1 && sPaths[fileIndex] != null){
			return ParcelFileDescriptor.open(new File(sPaths[fileIndex]),
				ParcelFileDescriptor.MODE_READ_ONLY);
		}
		return super.openFile(uri, mode);
	}

	@Override
	public String getType(Uri uri)
	{
		int fileIndex = sUriMatcher.match(uri);
		if(fileIndex != -1 && sPaths[fileIndex] != null){
			return sMimeTypes[fileIndex];
		}
		return null;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public String[] getStreamTypes(Uri uri, String mimeTypeFilter)
	{
		String type = getType(uri);
		return (type != null) ? new String[] { type } : null;
	}


	// -- provides extra information

	private static class FileCursor extends MatrixCursor
	{
		private static final String[] sColumns = { "_display_name", "_size" };

		public FileCursor(String displayName, File file)
		{
			super(sColumns, 1);
			RowBuilder row = newRow();
			row.add(displayName);
			row.add(file.length());
		}
	}


	// -- methods below are not required for our ContentProvider

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		return 0;
	}
}
