package me.ccrama.redditslide.Activities;

/**
 * Created by carlo_000 on 1/20/2016.
 */

import android.os.Bundle;

import ltd.ucode.slide.ui.BaseActivity;
import ltd.ucode.slide.ui.main.MainActivity;
import ltd.ucode.slide.R;

/**
 * Created by ccrama on 9/17/2015.
 */
public class Loader extends BaseActivity {


    @Override
    public void onCreate(Bundle savedInstance) {
        disableSwipeBackLayout();
        super.onCreate(savedInstance);
        applyColorTheme();
        setContentView(R.layout.activity_loading);
        MainActivity.loader = this;
    }
}
