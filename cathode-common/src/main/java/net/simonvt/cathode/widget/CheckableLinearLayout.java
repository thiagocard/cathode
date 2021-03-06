/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class CheckableLinearLayout extends LinearLayout implements Checkable {

  private boolean checked;
  private final List<Checkable> checkables = new ArrayList<>();

  public CheckableLinearLayout(Context context) {
    super(context);
  }

  public CheckableLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      findCheckable(getChildAt(i));
    }
  }

  private void findCheckable(View v) {
    if (v instanceof Checkable) {
      checkables.add((Checkable) v);
    }

    if (v instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) v;
      int childCount = vg.getChildCount();
      for (int i = 0; i < childCount; i++) {
        findCheckable(vg.getChildAt(i));
      }
    }
  }

  private static final int[] CHECKED_STATE_SET = {
      android.R.attr.state_checked,
  };

  @Override protected int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }

  @Override public boolean isChecked() {
    return checked;
  }

  @Override public void setChecked(boolean checked) {
    this.checked = checked;
    int checkCount = checkables.size();
    for (int i = 0; i < checkCount; i++) {
      checkables.get(i).setChecked(checked);
    }
    invalidate();
    refreshDrawableState();
  }

  @Override public void toggle() {
    setChecked(!checked);
  }
}
