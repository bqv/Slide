/*
 * Copyright 2014 Evgeny Shishkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devspark.robototextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utilities for working with roboto typefaces.
 *
 * @author Evgeny Shishkin
 */
public final class RobotoTypefaces {

    public static final int TYPEFACE_ROBOTO_LIGHT = 2;
    public static final int TYPEFACE_ROBOTO_REGULAR = 4;
    public static final int TYPEFACE_ROBOTO_MEDIUM = 6;
    public static final int TYPEFACE_ROBOTO_BOLD = 8;
    public static final int TYPEFACE_ROBOTO_CONDENSED_LIGHT = 12;
    public static final int TYPEFACE_ROBOTO_CONDENSED_REGULAR = 14;
    public static final int TYPEFACE_ROBOTO_CONDENSED_BOLD = 16;
    public static final int TYPEFACE_ROBOTO_SLAB_LIGHT = 19;
    public static final int TYPEFACE_ROBOTO_SLAB_REGULAR = 20;
    /**
     * Array of created typefaces for later reused.
     */
    private static final SparseArray<Typeface> typefacesCache = new SparseArray<>(32);

    private RobotoTypefaces() {
    }

    /**
     * Obtain typeface.
     *
     * @param context       The Context the widget is running in, through which it can access the current theme, resources, etc.
     * @param typefaceValue The value of "robotoTypeface" attribute
     * @return specify {@link Typeface} or throws IllegalArgumentException if unknown `robotoTypeface` attribute value.
     */
    @NonNull
    public static Typeface obtainTypeface(@NonNull Context context, @RobotoTypeface int typefaceValue) {
        Typeface typeface = typefacesCache.get(typefaceValue);
        if (typeface == null) {
            typeface = createTypeface(context, typefaceValue);
            typefacesCache.put(typefaceValue, typeface);
        }
        return typeface;
    }

    /**
     * Create typeface from assets.
     *
     * @param context  The Context the widget is running in, through which it can
     *                 access the current theme, resources, etc.
     * @param typeface The value of "robotoTypeface" attribute
     * @return Roboto {@link Typeface} or throws IllegalArgumentException if unknown `robotoTypeface` attribute value.
     */
    @NonNull
    private static Typeface createTypeface(@NonNull Context context, @RobotoTypeface int typeface) {
        String path;
        switch (typeface) {
            case TYPEFACE_ROBOTO_LIGHT:
                path = "fonts/Roboto-Light.ttf";
                break;
            case TYPEFACE_ROBOTO_REGULAR:
                path = "fonts/Roboto-Regular.ttf";
                break;
            case TYPEFACE_ROBOTO_MEDIUM:
                path = "fonts/Roboto-Medium.ttf";
                break;
            case TYPEFACE_ROBOTO_BOLD:
                path = "fonts/Roboto-Bold.ttf";
                break;
            case TYPEFACE_ROBOTO_CONDENSED_LIGHT:
                path = "fonts/RobotoCondensed-Light.ttf";
                break;
            case TYPEFACE_ROBOTO_CONDENSED_REGULAR:
                path = "fonts/RobotoCondensed-Regular.ttf";
                break;
            case TYPEFACE_ROBOTO_CONDENSED_BOLD:
                path = "fonts/RobotoCondensed-Bold.ttf";
                break;
            case TYPEFACE_ROBOTO_SLAB_LIGHT:
                path = "fonts/RobotoSlab-Light.ttf";
                break;
            case TYPEFACE_ROBOTO_SLAB_REGULAR:
                path = "fonts/RobotoSlab-Regular.ttf";
                break;
            default:
                throw new IllegalArgumentException("Unknown `robotoTypeface` attribute value " + typeface);
        }
        return Typeface.createFromAsset(context.getAssets(), path);
    }

    /**
     * Obtain typeface from attributes.
     *
     * @param context The Context the widget is running in, through which it can access the current theme, resources, etc.
     * @param attrs   The styled attribute values in this Context's theme.
     * @return specify {@link Typeface}
     */
    @NonNull
    public static Typeface obtainTypeface(@NonNull Context context, @NonNull TypedArray attrs) {
        @RobotoTypeface int typefaceValue = attrs.getInt(R.styleable.RobotoTextView_robotoTypeface, TYPEFACE_ROBOTO_REGULAR);
        return obtainTypeface(context, typefaceValue);
    }

    /**
     * Set up typeface for TextView from the attributes.
     *
     * @param textView The roboto text view
     * @param context  The context the widget is running in, through which it can
     *                 access the current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the widget.
     */
    public static void setUpTypeface(@NonNull TextView textView, @NonNull Context context, @Nullable AttributeSet attrs) {
        Typeface typeface;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RobotoTextView);
            try {
                typeface = obtainTypeface(context, a);
            } finally {
                a.recycle();
            }
        } else {
            typeface = obtainTypeface(context, TYPEFACE_ROBOTO_REGULAR);
        }
        setUpTypeface(textView, typeface);
    }

    /**
     * Set up typeface for TextView.
     *
     * @param textView The text view
     * @param typeface The value of "robotoTypeface" attribute
     */
    public static void setUpTypeface(@NonNull TextView textView, @RobotoTypeface int typeface) {
        setUpTypeface(textView, obtainTypeface(textView.getContext(), typeface));
    }

    /**
     * Set up typeface for TextView. Wrapper over {@link TextView#setTypeface(Typeface)}
     * for making the font anti-aliased.
     *
     * @param textView The text view
     * @param typeface The specify typeface
     */
    public static void setUpTypeface(@NonNull TextView textView, @NonNull Typeface typeface) {
        textView.setPaintFlags(textView.getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        textView.setTypeface(typeface);
    }

    /**
     * Set up typeface for Paint. Wrapper over {@link Paint#setTypeface(Typeface)}
     * for making the font anti-aliased.
     *
     * @param paint    The paint
     * @param typeface The specify typeface
     */
    public static void setUpTypeface(@NonNull Paint paint, @NonNull Typeface typeface) {
        paint.setFlags(paint.getFlags() | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setTypeface(typeface);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            TYPEFACE_ROBOTO_LIGHT,
            TYPEFACE_ROBOTO_REGULAR,
            TYPEFACE_ROBOTO_MEDIUM,
            TYPEFACE_ROBOTO_BOLD,
            TYPEFACE_ROBOTO_CONDENSED_LIGHT,
            TYPEFACE_ROBOTO_CONDENSED_REGULAR,
            TYPEFACE_ROBOTO_CONDENSED_BOLD,
            TYPEFACE_ROBOTO_SLAB_LIGHT,
            TYPEFACE_ROBOTO_SLAB_REGULAR,
    })
    public @interface RobotoTypeface {
    }
}
