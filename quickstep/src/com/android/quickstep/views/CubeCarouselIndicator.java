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
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

import com.android.launcher3.R;

/**
 * CubeOS app-switcher chrome drawn over the deck: an "N apps open · i of N" caption under the
 * status bar, and under the cards a page indicator whose capsule glides with the scroll.
 * Purely visual: it never takes touches. {@link RecentsView} feeds it the task count, the
 * centred card's fractional position and the y of the cards' bottom edge on every scroll frame.
 */
public class CubeCarouselIndicator extends View {

    private static final int MAX_DOTS = 9;

    private final float mDp;
    private final Paint mLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();

    private int mCount;
    private float mPosition;
    private float mDotsY = -1;

    public CubeCarouselIndicator(Context context) {
        this(context, null);
    }

    public CubeCarouselIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDp = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        mLabel.setColor(0xF2FFFFFF);
        mLabel.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        mLabel.setTypeface(medium);
        mLabel.setTextAlign(Paint.Align.CENTER);
        mLabel.setLetterSpacing(0.01f);
        mLabel.setFontFeatureSettings("tnum");
        mDot.setColor(0xFFFFFFFF);
    }

    /**
     * @param count    tasks in the carousel
     * @param position fractional index of the centred task (0 = first page)
     * @param dotsY    y (in this view) just below the centred card, or -1 to use the default
     */
    public void update(int count, float position, float dotsY) {
        if (count == mCount && position == mPosition && dotsY == mDotsY) return;
        mCount = count;
        mPosition = position;
        mDotsY = dotsY;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCount <= 0) return;
        float cx = getWidth() / 2f;

        // ── caption just under the status bar, above the cards' app icons: "6 apps open · 2 of 6" ──
        int top = 0;
        WindowInsets insets = getRootWindowInsets();
        if (insets != null) {
            Insets bars = insets.getInsets(WindowInsets.Type.statusBars());
            top = bars.top;
        }
        int current = Math.max(1, Math.min(mCount, Math.round(mPosition) + 1));
        String label = getResources().getQuantityString(R.plurals.cube_carousel_apps_open, mCount, mCount)
                + (mCount > 1 ? "  ·  " + current + " of " + mCount : "");
        Paint.FontMetrics fm = mLabel.getFontMetrics();
        canvas.drawText(label, cx, top + 6 * mDp - fm.ascent, mLabel);

        // ── page indicator: dots, the centred one stretched into a capsule ──
        float y = mDotsY > 0 ? mDotsY + 16 * mDp : getHeight() - 150 * mDp;
        int shown = Math.min(mCount, MAX_DOTS);
        int first = 0;
        if (mCount > MAX_DOTS) {
            first = Math.round(mPosition) - MAX_DOTS / 2;
            first = Math.max(0, Math.min(first, mCount - MAX_DOTS));
        }
        float dot = 6 * mDp, capsule = 20 * mDp, gap = 7 * mDp;
        float[] widths = new float[shown];
        float[] alphas = new float[shown];
        float total = 0;
        for (int k = 0; k < shown; k++) {
            int i = first + k;
            float near = Math.max(0f, 1f - Math.abs(mPosition - i));
            float w = dot + (capsule - dot) * ease(near);
            // More tasks beyond the window: the edge dots shrink, as iOS's page control does.
            boolean edge = (k == 0 && first > 0) || (k == shown - 1 && first + shown < mCount);
            if (edge) w *= 0.6f;
            widths[k] = w;
            alphas[k] = 0.32f + 0.68f * near;
            total += w;
        }
        total += gap * (shown - 1);
        float x = cx - total / 2;
        for (int k = 0; k < shown; k++) {
            float w = widths[k];
            float h = (k == 0 && first > 0) || (k == shown - 1 && first + shown < mCount) ? dot * 0.6f : dot;
            mRect.set(x, y - h / 2, x + w, y + h / 2);
            mDot.setAlpha(Math.round(255 * alphas[k]));
            canvas.drawRoundRect(mRect, h / 2, h / 2, mDot);
            x += w + gap;
        }

    }

    private static float ease(float t) {
        // Smoothstep: the capsule grows and shrinks without a linear "tick".
        return t * t * (3 - 2 * t);
    }
}