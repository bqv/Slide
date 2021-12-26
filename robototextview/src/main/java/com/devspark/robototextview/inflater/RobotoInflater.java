package com.devspark.robototextview.inflater;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.devspark.robototextview.RobotoTypefaces;

public class RobotoInflater implements LayoutInflater.Factory2 {
    private final RobotoCompatInflater mCompatInflater = new RobotoCompatInflater();
    private final AppCompatDelegate mAppCompatDelegate;

    private RobotoInflater(@NonNull AppCompatDelegate delegate) {
        mAppCompatDelegate = delegate;
    }

    public static void attach(@NonNull AppCompatActivity activity) {
        final RobotoInflater factory = new RobotoInflater(activity.getDelegate());
        activity.getLayoutInflater().setFactory2(factory);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        try {
            View view = mAppCompatDelegate.createView(parent, name, context, attrs);
            if (view == null) {
                view = mCompatInflater.createView(name, context, attrs);
            }

            if (view instanceof TextView) {
                RobotoTypefaces.setUpTypeface((TextView) view, context, attrs);
            }
            return view;
        } catch (Exception e) {
            //if something went wrong
            return null;
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }
}
