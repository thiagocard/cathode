/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.suggestions.shows;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.suggestions.SuggestionsFragment;

public class ShowSuggestionsFragment extends SuggestionsFragment {

  public static final String TAG =
      "net.simonvt.cathode.ui.suggestions.shows.ShowSuggestionsFragment";

  private ShowSuggestionsPagerAdapter adapter;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.title_suggestions);
    adapter = new ShowSuggestionsPagerAdapter(getContext(), getChildFragmentManager());
  }

  @Override protected PagerAdapter getAdapter() {
    return adapter;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows);
    toolbar.inflateMenu(R.menu.fragment_shows_recommended);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        return adapter.getItem(pager.getCurrentItem()).onMenuItemClick(item);

      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }
}
