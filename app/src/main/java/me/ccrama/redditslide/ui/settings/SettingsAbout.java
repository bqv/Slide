package me.ccrama.redditslide.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import ltd.ucode.slide.ui.BaseActivityAnim;
import ltd.ucode.slide.BuildConfig;
import me.ccrama.redditslide.OpenRedditLink;
import ltd.ucode.slide.R;
import me.ccrama.redditslide.util.ClipboardUtil;
import me.ccrama.redditslide.util.LinkUtil;


/**
 * Created by l3d00m on 11/12/2015.
 */
public class SettingsAbout extends BaseActivityAnim {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_about);
        setupAppBar(R.id.toolbar, R.string.settings_title_about, true, true);

        View report = findViewById(R.id.report);
        View libs = findViewById(R.id.libs);
        View changelog = findViewById(R.id.changelog);
        final TextView version = findViewById(R.id.version);

        version.setText("Slide v" + BuildConfig.VERSION_NAME);

        //Copy the latest stacktrace with a long click on the version number
        if (BuildConfig.DEBUG) {
            version.setOnLongClickListener(view -> {
                SharedPreferences prefs = getSharedPreferences(
                        "STACKTRACE", Context.MODE_PRIVATE);
                String stacktrace = prefs.getString("stacktrace", null);
                if (stacktrace != null) {
                    ClipboardUtil.copyToClipboard(SettingsAbout.this, "Stacktrace", stacktrace);
                }
                prefs.edit().clear().apply();
                return true;
            });

        }
        version.setOnClickListener(v -> {
            String versionNumber = version.getText().toString();
            ClipboardUtil.copyToClipboard(SettingsAbout.this, "Version", versionNumber);
            Toast.makeText(SettingsAbout.this, R.string.settings_about_version_copied_toast, Toast.LENGTH_SHORT).show();

        });

        report.setOnClickListener(v -> LinkUtil.openExternally("https://github.com/bqv/slide/issues"));
        findViewById(R.id.sub).setOnClickListener(v -> OpenRedditLink.openUrl(SettingsAbout.this, "https://feddit.uk/c/slide", true));
        findViewById(R.id.rate).setOnClickListener(v -> LinkUtil.launchMarketUri(SettingsAbout.this, R.string.app_package));
        changelog.setOnClickListener(v -> LinkUtil.openExternally("https://github.com/bqv/slide/blob/master/CHANGELOG.md"));

        libs.setOnClickListener(v -> {
            Intent i = new Intent(SettingsAbout.this, SettingsLibs.class);
            startActivity(i);
        });
    }


}
