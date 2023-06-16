package me.ccrama.redditslide.ui.settings;

import android.app.Activity;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;

import ltd.ucode.slide.R;
import ltd.ucode.slide.SettingValues;

public class SettingsDataFragment {

    private final Activity context;

    public SettingsDataFragment(Activity context) {
        this.context = context;
    }

    public void Bind() {
        final RelativeLayout datasavingDataSaveTypeLayout = context.findViewById(R.id.settings_datasaving_datasavetype);
        final TextView datasavingLowQualityView = context.findViewById(R.id.settings_datasaving_lowquality);

        final RelativeLayout datasavingDataSaveQualityLayout = context.findViewById(R.id.settings_datasaving_datasavequality);
        final TextView datasavingCurrentModeView = context.findViewById(R.id.settings_datasaving_currentmode);

        final SwitchCompat datasavingVideoQualitySwitch = context.findViewById(R.id.settings_datasaving_videoquality);

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//* Data saving mode */
        datasavingDataSaveTypeLayout.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, v);
            popup.getMenuInflater().inflate(R.menu.imagequality_settings, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.never:
                        setImageQualitySettings(false, false);
                        break;
                    case R.id.mobile:
                        setImageQualitySettings(true, false);
                        break;
                    case R.id.always:
                        setImageQualitySettings(true, true);
                        break;
                }
                datasavingLowQualityView.setText(
                        SettingValues.lowResMobile ? SettingValues.lowResAlways ? context.getString(R.string.datasave_always)
                                : context.getString(R.string.datasave_mobile) : context.getString(R.string.never));
                if (datasavingLowQualityView.getText().equals(context.getString(R.string.never))) {
                    datasavingDataSaveQualityLayout.setAlpha(0.25f);
                    datasavingCurrentModeView.setText("Enable datasaving mode");
                    datasavingVideoQualitySwitch.setEnabled(false);
                } else {
                    datasavingDataSaveQualityLayout.setAlpha(1f);
                    datasavingCurrentModeView.setText(
                            SettingValues.noImages ? context.getString(R.string.never_load_images)
                                    : SettingValues.lqLow ? context.getString(R.string.load_low_quality)
                                    : SettingValues.lqMid ? context.getString(R.string.load_medium_quality)
                                    : context.getString(R.string.load_high_quality));
                    datasavingVideoQualitySwitch.setEnabled(true);
                }
                return true;
            });
            popup.show();
        });
        if (datasavingLowQualityView.getText().equals(context.getString(R.string.never))) {
            datasavingDataSaveQualityLayout.setAlpha(0.25f);
            datasavingCurrentModeView.setText("Enable datasaving mode");
        }
        //Datasaving type multi choice
        datasavingLowQualityView.setText(
                SettingValues.lowResMobile ? SettingValues.lowResAlways ? context.getString(R.string.datasave_always)
                        : context.getString(R.string.datasave_mobile)
                        : context.getString(R.string.never));

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//* Data saving quality */
        datasavingDataSaveQualityLayout.setOnClickListener(v -> {
            if (!datasavingLowQualityView.getText().equals(context.getString(R.string.never))) {
                PopupMenu popup = new PopupMenu(context, v);
                popup.getMenuInflater().inflate(R.menu.imagequality_mode, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.never:
                            setImageQualityMode(true, true, false, false, false);
                            break;
                        case R.id.low:
                            setImageQualityMode(false, true, true, false, false);
                            break;
                        case R.id.medium:
                            setImageQualityMode(false, true, false, true, false);
                            break;
                        case R.id.high:
                            setImageQualityMode(false, true, false, false, true);
                            break;
                    }
                    datasavingCurrentModeView.setText(
                            SettingValues.noImages ? context.getString(R.string.never_load_images)
                                    : SettingValues.lqLow ? context.getString(R.string.load_low_quality)
                                    : SettingValues.lqMid ? context.getString(R.string.load_medium_quality)
                                    : context.getString(R.string.load_high_quality));
                    return true;
                });
                popup.show();
            }
        });
        //Image mode multi choice
        datasavingCurrentModeView.setText(SettingValues.noImages ? context.getString(R.string.never_load_images)
                : SettingValues.lqLow ? context.getString(R.string.load_low_quality)
                : SettingValues.lqMid ? context.getString(R.string.load_medium_quality)
                : context.getString(R.string.load_high_quality));

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        datasavingVideoQualitySwitch.setChecked(SettingValues.lqVideos);
        datasavingVideoQualitySwitch.setEnabled(SettingValues.lowResMobile || SettingValues.lowResAlways);
        datasavingVideoQualitySwitch.setOnCheckedChangeListener((v, checked) -> {
            SettingValues.lqVideos = checked;
            SettingValues.editBoolean(SettingValues.PREF_LQ_VIDEOS, checked);
        });
    }

    private void setImageQualitySettings(boolean mobile, boolean always) {
        SettingValues.lowResMobile = mobile;
        SettingValues.lowResAlways = always;
        SettingValues.editBoolean(SettingValues.PREF_LOW_RES_MOBILE, mobile);
        SettingValues.editBoolean(SettingValues.PREF_LOW_RES_ALWAYS, always);
    }

    private void setImageQualityMode(boolean noImages, boolean loadImageLq, boolean lqLow, boolean lqMid, boolean lqHigh) {
        SettingValues.noImages = noImages;
        SettingValues.loadImageLq = loadImageLq;
        SettingValues.lqLow = lqLow;
        SettingValues.lqMid = lqMid;
        SettingValues.lqHigh = lqHigh;
        SettingValues.editBoolean(SettingValues.PREF_NO_IMAGES, noImages);
        SettingValues.editBoolean(SettingValues.PREF_IMAGE_LQ, loadImageLq);
        SettingValues.editBoolean(SettingValues.PREF_LQ_LOW, lqLow);
        SettingValues.editBoolean(SettingValues.PREF_LQ_MID, lqMid);
        SettingValues.editBoolean(SettingValues.PREF_LQ_HIGH, lqHigh);
    }
}
