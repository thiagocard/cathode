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

package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.HiddenItem;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.HiddenSection;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.PagedCallJob;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncHiddenCalendar extends PagedCallJob<HiddenItem> {

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient MovieDatabaseHelper movieHelper;

  public SyncHiddenCalendar() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncHiddenCalendar";
  }

  @Override public int getPriority() {
    return 0;
  }

  @Override public Call<List<HiddenItem>> getCall(int page) {
    return usersService.getHiddenItems(HiddenSection.CALENDAR, page, 25);
  }

  @Override public boolean handleResponse(List<HiddenItem> items) {
    List<Long> unhandledShows = new ArrayList<>();
    Cursor hiddenShows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.HIDDEN_CALENDAR + "=1", null, null);
    while (hiddenShows.moveToNext()) {
      final long id = Cursors.getLong(hiddenShows, ShowColumns.ID);
      unhandledShows.add(id);
    }
    hiddenShows.close();

    List<Long> unhandledMovies = new ArrayList<>();
    Cursor hiddenMovies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, MovieColumns.HIDDEN_CALENDAR + "=1", null, null);
    while (hiddenMovies.moveToNext()) {
      final long id = Cursors.getLong(hiddenMovies, MovieColumns.ID);
      unhandledMovies.add(id);
    }
    hiddenMovies.close();
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (HiddenItem item : items) {
      switch (item.getType()) {
        case SHOW: {
          Show show = item.getShow();
          final long traktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
          final long showId = showResult.showId;
          if (showResult.didCreate) {
            queue(new SyncShow(traktId));
          }

          if (!unhandledShows.remove(showId)) {
            ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
                .withValue(ShowColumns.HIDDEN_CALENDAR, 1)
                .build();
            ops.add(op);
          }
          break;
        }

        case MOVIE: {
          Movie movie = item.getMovie();
          final long traktId = movie.getIds().getTrakt();
          MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
          final long movieId = result.movieId;
          if (result.didCreate) {
            queue(new SyncMovie(traktId));
          }

          if (!unhandledMovies.remove(movieId)) {
            ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                .withValue(MovieColumns.HIDDEN_CALENDAR, 1)
                .build();
            ops.add(op);
          }
          break;
        }
      }
    }

    for (long showId : unhandledShows) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(ShowColumns.HIDDEN_CALENDAR, 0)
          .build();
      ops.add(op);
    }

    for (long movieId : unhandledMovies) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(MovieColumns.HIDDEN_CALENDAR, 0)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
