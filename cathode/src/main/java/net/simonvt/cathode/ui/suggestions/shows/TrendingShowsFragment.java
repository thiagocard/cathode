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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.remote.sync.shows.SyncTrendingShows;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.settings.SuggestionsTimestamps;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.fragment.SwipeRefreshRecyclerFragment;
import net.simonvt.cathode.ui.lists.ListDialog;
import net.simonvt.cathode.ui.shows.ShowClickListener;
import net.simonvt.cathode.ui.shows.ShowDescriptionAdapter;

public class TrendingShowsFragment
    extends SwipeRefreshRecyclerFragment<ShowDescriptionAdapter.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor>, ListDialog.Callback, ShowClickListener {

  private enum SortBy {
    VIEWERS("viewers", Shows.SORT_VIEWERS), RATING("rating", Shows.SORT_RATING);

    private String key;

    private String sortOrder;

    SortBy(String key, String sortOrder) {
      this.key = key;
      this.sortOrder = sortOrder;
    }

    public String getKey() {
      return key;
    }

    public String getSortOrder() {
      return sortOrder;
    }

    @Override public String toString() {
      return key;
    }

    private static final Map<String, SortBy> STRING_MAPPING = new HashMap<>();

    static {
      for (SortBy via : SortBy.values()) {
        STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
      }
    }

    public static SortBy fromValue(String value) {
      SortBy sortBy = STRING_MAPPING.get(value.toUpperCase(Locale.US));
      if (sortBy == null) {
        sortBy = VIEWERS;
      }
      return sortBy;
    }
  }

  private static final String DIALOG_SORT =
      "net.simonvt.cathode.ui.suggestions.shows.TrendingShowsFragment.sortDialog";

  private static final int LOADER_SHOWS_TRENDING = 1;

  private ShowDescriptionAdapter showsAdapter;

  private ShowsNavigationListener navigationListener;

  @Inject JobManager jobManager;

  private SortBy sortBy;

  private Cursor cursor;

  private int columnCount;

  private boolean scrollToTop;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    sortBy = SortBy.fromValue(
        Settings.get(getContext()).getString(Settings.Sort.SHOW_TRENDING, SortBy.VIEWERS.getKey()));

    getLoaderManager().initLoader(LOADER_SHOWS_TRENDING, null, this);

    columnCount = getResources().getInteger(R.integer.showsColumns);
    setTitle(R.string.title_shows_trending);
    setEmptyText(R.string.shows_loading_trending);

    if (SuggestionsTimestamps.suggestionsNeedsUpdate(getActivity(),
        SuggestionsTimestamps.SHOWS_TRENDING)) {
      jobManager.addJob(new SyncTrendingShows());
      SuggestionsTimestamps.updateSuggestions(getActivity(), SuggestionsTimestamps.SHOWS_TRENDING);
    }
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncTrendingShows();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort_by:
        ArrayList<ListDialog.Item> items = new ArrayList<>();
        items.add(new ListDialog.Item(R.id.sort_viewers, R.string.sort_viewers));
        items.add(new ListDialog.Item(R.id.sort_rating, R.string.sort_rating));
        ListDialog.newInstance(R.string.action_sort_by, items, this)
            .show(getFragmentManager(), DIALOG_SORT);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  @Override public void onItemSelected(int id) {
    switch (id) {
      case R.id.sort_viewers:
        if (sortBy != SortBy.VIEWERS) {
          sortBy = SortBy.VIEWERS;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.SHOW_TRENDING, SortBy.VIEWERS.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_SHOWS_TRENDING, null, this);
          scrollToTop = true;
        }
        break;

      case R.id.sort_rating:
        if (sortBy != SortBy.RATING) {
          sortBy = SortBy.RATING;
          Settings.get(getContext())
              .edit()
              .putString(Settings.Sort.SHOW_TRENDING, SortBy.RATING.getKey())
              .apply();
          getLoaderManager().restartLoader(LOADER_SHOWS_TRENDING, null, this);
          scrollToTop = true;
        }
        break;
    }
  }

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  private void setCursor(Cursor cursor) {
    this.cursor = cursor;

    if (showsAdapter == null) {
      showsAdapter = new ShowDescriptionAdapter(getActivity(), this, cursor);
      setAdapter(showsAdapter);
      return;
    }

    showsAdapter.changeCursor(cursor);
    if (scrollToTop) {
      getRecyclerView().scrollToPosition(0);
      scrollToTop = false;
    }
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Shows.SHOWS_TRENDING,
        ShowDescriptionAdapter.PROJECTION, null, null, sortBy.getSortOrder());
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
  }
}
