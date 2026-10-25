/*
 * Copyright (C) 2026 CubeOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;

/**
 * CubeOS app switcher: the name of each app beside its icon above its card, as iOS shows it.
 * Purely visual: it never takes touches. {@link RecentsView} hands it, on every scroll frame, where
 * each visible label starts (just right of the icon, vertically centred on it), how opaque it is
 * (names fade as their card tucks behind the one in front) and how wide it may run.
 */
public class CubeCarouselIndicator extends View {

    private final TextPaint mLabel = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    /** Names ellipsized to a width, so a frame never measures text it has measured before. */
    private final HashMap<String, CharSequence> mFitted = new HashMap<>();

    private int mCount;
    private float[] mX, mY, mAlpha, mWidth;
    private CharSequence[] mText;

    public CubeCarouselIndicator(Context context) {
        this(context, null);
    }

    public CubeCarouselIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        mLabel.setColor(0xFFFFFFFF);
        mLabel.setTextSize(15 * getResources().getDisplayMetrics().scaledDensity);
        mLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mLabel.setShadowLayer(6 * getResources().getDisplayMetrics().density, 0, 0, 0x66000000);
    }

    /** The arrays are owned by the caller and read on the next draw; [count] entries are used. */
    public void setLabels(int count, float[] x, float[] y, float[] alpha, float[] width,
            CharSequence[] text) {
        mCount = count;
        mX = x;
        mY = y;
        mAlpha = alpha;
        mWidth = width;
        mText = text;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint.FontMetrics fm = mLabel.getFontMetrics();
        float centre = -(fm.ascent + fm.descent) / 2f;
        for (int i = 0; i < mCount; i++) {
            if (mText[i] == null || mAlpha[i] <= 0f) continue;
            mLabel.setAlpha(Math.round(255 * Math.min(1f, mAlpha[i])));
            canvas.drawText(fit(mText[i], mWidth[i]).toString(), mX[i], mY[i] + centre, mLabel);
        }
    }

    private CharSequence fit(CharSequence text, float width) {
        // Widths move by a pixel or two as cards shrink; bucket them so the cache stays small.
        int bucket = Math.round(width / 8f) * 8;
        String key = bucket + "\u0000" + text;
        CharSequence fitted = mFitted.get(key);
        if (fitted == null) {
            if (mFitted.size() > 64) mFitted.clear();
            fitted = TextUtils.ellipsize(text, mLabel, bucket, TextUtils.TruncateAt.END);
            mFitted.put(key, fitted);
        }
        return fitted;
    }
}
