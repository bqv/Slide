package me.ccrama.redditslide.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;

import me.ccrama.redditslide.Activities.BaseActivityAnim;
import ltd.ucode.slide.R;
import ltd.ucode.slide.SettingValues;


/**
 * Created by ccrama on 3/5/2015.
 */
public class SettingsViewType extends BaseActivityAnim {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyColorTheme();
        setContentView(R.layout.activity_settings_viewtype);
        setupAppBar(R.id.toolbar, R.string.settings_view_type, true, true);



        //View type multi choice
        ((TextView) findViewById(R.id.currentViewType)).setText(
                SettingValues.INSTANCE.getSingle()
                        ? (SettingValues.INSTANCE.getCommentPager()
                                ? getString(R.string.view_type_comments)
                                : getString(R.string.view_type_none))
                        : getString(R.string.view_type_tabs));

        findViewById(R.id.viewtype).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(SettingsViewType.this, v);
                popup.getMenuInflater().inflate(R.menu.view_type_settings, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.tabs:
                                SettingValues.INSTANCE.setSingle(false);
                                break;
                            case R.id.notabs:
                                SettingValues.INSTANCE.setSingle(true);
                                SettingValues.INSTANCE.setCommentPager(false);
                                break;
                            case R.id.comments:
                                SettingValues.INSTANCE.setSingle(true);
                                SettingValues.INSTANCE.setCommentPager(true);
                                break;
                        }
                        ((TextView) findViewById(R.id.currentViewType)).setText(
                                SettingValues.INSTANCE.getSingle()
                                        ? (SettingValues.INSTANCE.getCommentPager()
                                                ? getString(R.string.view_type_comments)
                                                : getString(R.string.view_type_none))
                                        : getString(R.string.view_type_tabs));
                        SettingsThemeFragment.changed = true;
                        return true;
                    }
                });

                popup.show();
            }
        });

    }
}
