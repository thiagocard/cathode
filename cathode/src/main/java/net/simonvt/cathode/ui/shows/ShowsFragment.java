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
package net.simonvt.cathode.ui.shows;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;

public abstract class ShowsFragment<D extends Cursor>
    extends ToolbarSwipeRefreshRecyclerFragment<ShowsWithNextAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<D>, ShowClickListener {

  @Inject protected JobManager jobManager;

  protected RecyclerCursorAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  private Cursor cursor;

  private int columnCount;

  protected boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    getLoaderManager().initLoader(getLoaderId(), null, this);

    columnCount = getResources().getInteger(R.integer.showsColumns);
  }

  @Override public boolean displaysMenuIcon() {
    return amITopLevel();
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_shows);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_search:
        navigationListener.onSearchClicked();
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, getLibraryType());
  }

  protected ShowsWithNextAdapter getAdapter(Cursor cursor) {
    return new ShowsWithNextAdapter(getActivity(), this, cursor, getLibraryType());
  }

  protected void setCursor(Cursor cursor) {
    this.cursor = cursor;

    if (showsAdapter == null) {
      showsAdapter = getAdapter(cursor);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);

    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  protected abstract LibraryType getLibraryType();

  protected abstract int getLoaderId();

  @Override public void onLoadFinished(Loader<D> cursorLoader, D cursor) {
    setCursor(cursor);
  }

  @Override public void onLoaderReset(Loader<D> cursorLoader) {
  }
}
