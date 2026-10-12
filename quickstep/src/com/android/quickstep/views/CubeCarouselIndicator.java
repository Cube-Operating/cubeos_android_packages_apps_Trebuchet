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
 * CubeOS app-switcher chrome drawn over the carousel: a frosted "N apps open" pill at the top,
 * and under the cards a page indicator whose capsule glides with the carousel, with "i / n".
 * Purely visual: it never takes touches. {@link RecentsView} feeds it the task count, the
 * centred card's fractional position and the y of the cards' bottom edge on every scroll frame.
 */
public class CubeCarouselIndicator extends View {

    private static final int MAX_DOTS = 9;

    private final float mDp;
    private final Paint mGlass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlassEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCounter = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        mGlass.setColor(0x2EFFFFFF);
        mGlassEdge.setStyle(Paint.Style.STROKE);
        mGlassEdge.setStrokeWidth(mDp);
        mGlassEdge.setColor(0x26FFFFFF);
        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        mLabel.setColor(0xF2FFFFFF);
        mLabel.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
        mLabel.setTypeface(medium);
        mLabel.setTextAlign(Paint.Align.CENTER);
        mLabel.setLetterSpacing(0.01f);
        mCounter.setColor(0x99FFFFFF);
        mCounter.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        mCounter.setTypeface(medium);
        mCounter.setTextAlign(Paint.Align.CENTER);
        mCounter.setFontFeatureSettings("tnum");
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

        // ── "N apps open" pill ──
        int top = 0;
        WindowInsets insets = getRootWindowInsets();
        if (insets != null) {
            Insets bars = insets.getInsets(WindowInsets.Type.statusBars());
            top = bars.top;
        }
        String label = getResources().getQuantityString(R.plurals.cube_carousel_apps_open, mCount, mCount);
        float textW = mLabel.measureText(label);
        float pillH = 34 * mDp;
        float pillW = textW + 32 * mDp;
        float pillTop = top + 14 * mDp;
        mRect.set(cx - pillW / 2, pillTop, cx + pillW / 2, pillTop + pillH);
        canvas.drawRoundRect(mRect, pillH / 2, pillH / 2, mGlass);
        canvas.drawRoundRect(mRect, pillH / 2, pillH / 2, mGlassEdge);
        Paint.FontMetrics fm = mLabel.getFontMetrics();
        canvas.drawText(label, cx, mRect.centerY() - (fm.ascent + fm.descent) / 2, mLabel);

        // ── page indicator: dots, the centred one stretched into a capsule ──
        float y = mDotsY > 0 ? mDotsY + 22 * mDp : getHeight() - 150 * mDp;
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

        // ── "i / n" ──
        int current = Math.max(1, Math.min(mCount, Math.round(mPosition) + 1));
        canvas.drawText(current + " / " + mCount, cx, y + 26 * mDp, mCounter);
    }

    private static float ease(float t) {
        // Smoothstep: the capsule grows and shrinks without a linear "tick".
        return t * t * (3 - 2 * t);
    }
}