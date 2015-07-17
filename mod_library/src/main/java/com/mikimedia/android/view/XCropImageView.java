/*
 *
 *  * Copyright (c) 2015. The MikiMedia Inc
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.mikimedia.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.mikimedia.android.R;

/**
 * XCropImageView provides 11 configurations
 *
 * FitWidth  - 3 configurations - TOP/BOTTOM/CENTER
 * FitHeight - 3 configurations - CENTER/LEFT/RIGHT
 * FitBest   - 5 configurations - TOP/BOTTOM/CENTER/LEFT/RIGHT depending on which scale is used.
 *
 * The configurations for FitWidth + LEFT/RIGHT are correct, but they doesn't show any visual difference.
 * This is because when an image is fit to width, the full width is already within the visible left and right bounds.
 * They are effectively ALWAYS aligned LEFT and RIGHT.
 *
 * Similarly for FitHeight, it is effectively ALWAYS aligned TOP and BOTTOM
 *
 * THe configurations are more visible with FitBest, which will decide which bound to fit to.
 * The default android scaleType=centerCrop is effectively 1 of the above configurations FitBest + CENTER
 *
 */
public class XCropImageView extends ImageView {

    // Keep this here for reference
//    public static final class ScaleCropType {
//        public static final int FIT_WIDTH = 0;
//        public static final int FIT_FILL = 1;
//        public static final int FIT_HEIGHT = 2;
//    }
//
//    public static final class AlignTo {
//        public static final int ALIGN_TOP = 1;
//        public static final int ALIGN_BOTTOM = 2;
//        public static final int ALIGN_CENTER = 3;
//        public static final int ALIGN_LEFT = 4;
//        public static final int ALIGN_RIGHT = 5;
//    }

    private int mScaleCropType = -1;
    private int mAlignment = 0;

    public XCropImageView(Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public XCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public XCropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromAttributes(context, attrs, defStyle, 0);
    }

    /**
     * Initialize the attributes for the XCropImageView
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     * @see #View(Context, AttributeSet, int)
     */
    private void initFromAttributes(Context context, AttributeSet attrs,
                               int defStyleAttr, int defStyleRes) {
        // Read and apply provided attributes
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.XCropImageView, defStyleAttr, defStyleRes);
        mScaleCropType = a.getInt(R.styleable.XCropImageView_scaleCropType, mScaleCropType);
        mAlignment = a.getInt(R.styleable.XCropImageView_alignTo, mAlignment);
        a.recycle();

        if (mScaleCropType > -1) setScaleType(ScaleType.MATRIX);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getDrawable();
        if (d != null && mScaleCropType > -1) {

            final int dw = d.getIntrinsicWidth();
            final int dh = d.getIntrinsicHeight();
            int dx = 0, dy = 0;
            int msWidth = 0, msHeight = 0;
            int theoryw = 0, theoryh = 0;
            float scalew = 0, scaleh = 0;
            float scale = 0;

            if (mScaleCropType < 2) { // fit width || bestfit
                // 1. get anchor by width. constrain to drawablewidth if wrap-content
                msWidth = MeasureSpec.getSize(widthMeasureSpec); // the view width
                if (getLayoutParams().width == -2) { // wrap
                    msWidth = dw < msWidth ? dw : msWidth;
                }

                // 2. compute scale and theoretical height
                scalew = (float) msWidth / dw; // scale_via_width
                theoryh = (int) (dh * scalew); // theoretical = height x scale_via_width
            }

            if (mScaleCropType > 0) {// fit bestfit || height
                // 1. get anchor by height. constrain to drawableheight if wrap-content
                msHeight = MeasureSpec.getSize(heightMeasureSpec); // the view height
                if (getLayoutParams().height == -2) { // wrap
                    msHeight = dh < msHeight ? dh : msHeight;
                }

                // 2. compute scale and theoretical width
                scaleh = (float) msHeight / dh; // scale_via_height
                theoryw = (int) (dw * scaleh); // theoretical = width x scale_via_height
            }

            if (scalew > scaleh) { // fit width
                scale = scalew;

                // 3. constrain by maxheight
                // if match_parent then additional constraint if viewport < maxheight
                // if wrap_content then anything works even if viewport < or >  maxheight
                int maxHeight = getMaxHeight();
                msHeight = getLayoutParams().height;
                if (msHeight > -2) { // match parent
                    if (msHeight == -1) msHeight = MeasureSpec.getSize(heightMeasureSpec);
                    maxHeight = msHeight < maxHeight ? msHeight : maxHeight;
                }
                msHeight = theoryh > maxHeight ? maxHeight : theoryh; // limited height

                // 4. translate

                if (mAlignment > 2) { // AlignTo.ALIGN_CENTER || AlignTo.ALIGN_LEFT || AlignTo.ALIGN_RIGHT
                    // if you want center crop shift it up by 50% aka 0.5f
                    dy = (int)( (msHeight - theoryh) * 0.5f + 0.5f ); // + 0.5f for rounding
                } else if (mAlignment == 2) { // AlignTo.ALIGN_BOTTOM
                    // if you want bottom crop shift it up by 100% aka 1.0f
                    dy = (int)( (msHeight - theoryh) * 1.0f + 0.5f ); // + 0.5f for rounding
                }

            } else { // fit height
                scale = scaleh;

                // 3. constrain by maxwidth
                // if match_parent then additional constraint if viewport < maxwidth
                // if wrap_content then anything works even if viewport < or >  maxwidth
                int maxWidth = getMaxWidth();
                msWidth = getLayoutParams().width;
                if (msWidth > -2) { // match parent or is set
                    if (msWidth == -1) msWidth = MeasureSpec.getSize(widthMeasureSpec);
                    maxWidth = msWidth < maxWidth ? msWidth : maxWidth;
                }
                msWidth = theoryw > maxWidth ? maxWidth : theoryw; // limited width

                if (mAlignment < 4) { // AlignTo.ALIGN_CENTER || AlignTo.ALIGN_TOP || AlignTo.ALIGN_BOTTOM
                    // if you want center crop shift it left by 50% aka 0.5f
                    dx = (int)( (msWidth - theoryw) * 0.5f + 0.5f ); // + 0.5f for rounding
                } else if (mAlignment == 5) { //AlignTo.ALIGN_RIGHT
                    // if you want bottom crop shift it up by 100% aka 1.0f
                    dx = (int)( (msWidth - theoryw) * 1.0f + 0.5f ); // + 0.5f for rounding
                }
            }

            // this is to scale it only by width
            // - the pivot point is at (0,0)
            // for top crop we dont need to translate it
            Matrix matrix = getImageMatrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            setImageMatrix(matrix);
            setMeasuredDimension(msWidth, msHeight);
        }
        else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}