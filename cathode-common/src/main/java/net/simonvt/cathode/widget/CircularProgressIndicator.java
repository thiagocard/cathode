/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import java.util.Locale;
import net.simonvt.cathode.common.R;

public class CircularProgressIndicator extends View {

  private float minValue;

  private float maxValue;

  private float value;

  private String valueString;

  private int textSize;

  private int textColor;

  private int textBackgroundColor;

  private final Paint textPaint = new Paint();

  private final Rect textBounds = new Rect();

  private float textWidth;

  private final Rect tempBounds = new Rect();

  private int textMargin;

  private int strokeWidth;

  private final Paint circlePaint = new Paint();

  private final RectF circleBounds = new RectF();

  private int circleBackgroundColor;

  private int circleColor;

  private int minPadding;

  public CircularProgressIndicator(Context context) {
    this(context, null);
  }

  public CircularProgressIndicator(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CircularProgressIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressIndicator,
        R.attr.circularProgressIndicatorStyle, R.style.CircularProgressIndicator);

    final Resources res = getResources();
    final DisplayMetrics dm = res.getDisplayMetrics();

    final int defTextMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, dm);
    textMargin =
        a.getDimensionPixelSize(R.styleable.CircularProgressIndicator_textMargin, defTextMargin);

    final int defTextColor = 0xFF000000;
    textColor = a.getColor(R.styleable.CircularProgressIndicator_textColor, defTextColor);

    final int defTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, dm);
    textSize = a.getDimensionPixelSize(R.styleable.CircularProgressIndicator_textSize, defTextSize);

    textBackgroundColor = a.getColor(R.styleable.CircularProgressIndicator_textBackgroundColor, 0);

    final int defStrokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
    strokeWidth = a.getDimensionPixelSize(R.styleable.CircularProgressIndicator_circleStrokeWidth,
        defStrokeWidth);

    maxValue = a.getFloat(R.styleable.CircularProgressIndicator_maxValue, 10.0f);

    final int defCircleColor = res.getColor(android.R.color.holo_green_dark);
    circleColor = a.getColor(R.styleable.CircularProgressIndicator_circleColor, defCircleColor);

    circleBackgroundColor =
        a.getColor(R.styleable.CircularProgressIndicator_circleBackgroundColor, 0);

    a.recycle();

    minPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);

    textPaint.setAntiAlias(true);
    textPaint.setTextSize(textSize);
    textPaint.setColor(textColor);

    circlePaint.setAntiAlias(true);
    circlePaint.setStrokeWidth(strokeWidth);

    setValue(0.0f);
  }

  public void setValue(float value) {
    this.value = value;
    valueString = String.format(Locale.US, "%.1f", value);
    textPaint.getTextBounds(valueString, 0, valueString.length(), textBounds);
    textWidth = textPaint.measureText(valueString);
    invalidate();
    requestLayout();
  }

  @Override protected void onDraw(Canvas canvas) {
    final int width = getWidth();
    final int height = getHeight();

    circlePaint.setColor(textBackgroundColor);
    circlePaint.setStyle(Paint.Style.FILL);
    canvas.drawArc(circleBounds, 0, 360, false, circlePaint);

    circlePaint.setStyle(Paint.Style.STROKE);
    circlePaint.setColor(circleBackgroundColor);
    canvas.drawArc(circleBounds, 0, 360, false, circlePaint);

    circlePaint.setColor(circleColor);
    final int endAngle = (int) (360.0f / maxValue * value);
    canvas.drawArc(circleBounds, -90, endAngle, false, circlePaint);

    final float textX = width / 2 - textWidth / 2;
    final float textY = height / 2 + textBounds.height() / 2;
    canvas.drawText(valueString, textX, textY, textPaint);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int width = -1;
    int height = -1;

    if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
      width = widthSize;
      height = heightSize;
    } else if (widthMode == MeasureSpec.EXACTLY) {
      width = widthSize;
      height = widthSize;
    } else if (heightMode == MeasureSpec.EXACTLY) {
      width = heightSize;
      height = heightSize;
    }

    String maxValue = String.valueOf(this.maxValue);
    textPaint.getTextBounds(maxValue, 0, maxValue.length(), tempBounds);
    final int textWidth = tempBounds.width();
    final int textHeight = tempBounds.height();

    float c = (float) Math.sqrt(Math.pow(textWidth, 2) + Math.pow(textHeight, 2));

    if (width == -1) {
      width = (int) (c + textMargin + strokeWidth + 2 * minPadding);
      height = width;
    } else {
      width = Math.max(width, height);
      height = width;
    }

    final int halfStrokeWidth = strokeWidth / 2;
    final int circleSize = (int) (c + textMargin + halfStrokeWidth);
    final int halfCircleSize = circleSize / 2;
    final int halfWidth = width / 2;
    final int halfHeight = height / 2;

    circleBounds.left = halfWidth - halfCircleSize;
    circleBounds.top = halfHeight - halfCircleSize;
    circleBounds.right = halfWidth + halfCircleSize;
    circleBounds.bottom = halfHeight + halfCircleSize;

    setMeasuredDimension(width, height);
  }
}
