package com.devspark.robototextview.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.devspark.robototextview.inflater.RobotoInflater;

public class InflaterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        RobotoInflater.attach(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inflater);
    }
}
