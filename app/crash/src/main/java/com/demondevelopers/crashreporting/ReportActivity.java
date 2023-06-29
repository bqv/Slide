package com.demondevelopers.crashreporting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;


/**
 * Builds the report and creates a ACTION_SEND_MULTIPLE intent with attachments
 *
 * Note: Check the AndroidManifest as this is started in a new process which
 * also must be where the ContentProvider is hosted!
 *
 */
public class ReportActivity extends Activity
{
	private static final String EXTRA_THROWABLE  = "extraThrowable";
	private static final String EXTRA_SCREENSHOT = "extraScreenshot";


	public static Intent createIntent(Context context, Throwable throwable,
		String screenshotPath)
	{
		return new Intent(context.getApplicationContext(), ReportActivity.class)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.putExtra(EXTRA_THROWABLE,  throwable)
			.putExtra(EXTRA_SCREENSHOT, screenshotPath);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// TODO: Make use of a ProgressDialog while generating the asynchronously

		/*
		// Note: Must be used to debug, because this activity starts
		// in a new process which is not being debugged before.
		if(!Debug.isDebuggerConnected()){
			Debug.waitForDebugger();
		}
		*/

		Throwable ex = (Throwable)getIntent()
			.getSerializableExtra(EXTRA_THROWABLE);

		StringWriter report = new StringWriter();
		PrintWriter  writer = new PrintWriter(report);

		writer.println("CRASH REPORT\n");

		try{
			PackageInfo info = getPackageManager()
				.getPackageInfo(getPackageName(), 0);
			writer.println("Application:");
			writer.printf("Package: %s\nVersion Name:%s\nVersion Code: %d\n\n",
				info.packageName, info.versionName, info.versionCode);
		}
		catch(NameNotFoundException e){
			// Ignored, this shouldn't happen
		}

		if((ex.getMessage() != null && ex.getMessage().length() != 0)){
			writer.printf("Reason: %s\n", ex.getMessage());
		}

		StackTraceElement[] stack = ex.getStackTrace();
		if(stack.length > 0){
			writer.println("Stack Trace:");
			for(int i = 0; i < stack.length; i++){
				writer.println(stack[i]);
			}
		}

		Intent target = new Intent(Intent.ACTION_SEND_MULTIPLE);
		target.setType("text/plain");
		target.putExtra(Intent.EXTRA_EMAIL, new String[] {
			CrashReportHandler.getEmailAddress()
		});
		target.putExtra(Intent.EXTRA_SUBJECT, "Crash Report");
        ArrayList<String> extra_text = new ArrayList<>();
        extra_text.add(report.toString());
		target.putStringArrayListExtra(Intent.EXTRA_TEXT, extra_text);

		ArrayList<Uri> attachments = new ArrayList<>();

		String screenshot = getIntent().getStringExtra(EXTRA_SCREENSHOT);
		if(screenshot != null){
			attachments.add(ReportFilesProvider.setFilePath(
				ReportFilesProvider.FILE_INDEX_SCREENSHOT, screenshot));
		}

		String eventLogPath = CrashReportHandler.saveEventLog();
		if(eventLogPath != null){
			attachments.add(ReportFilesProvider.setFilePath(
				ReportFilesProvider.FILE_INDEX_EVENTLOG, eventLogPath));
		}

		String systemLogPath = CrashReportHandler.saveSystemLog();
		if(systemLogPath != null){
			attachments.add(ReportFilesProvider.setFilePath(
				ReportFilesProvider.FILE_INDEX_SYSTEMLOG, systemLogPath));
		}

		target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);

		startActivity(Intent.createChooser(target, "Send Crash Report Using"));
		finish();
	}
}
