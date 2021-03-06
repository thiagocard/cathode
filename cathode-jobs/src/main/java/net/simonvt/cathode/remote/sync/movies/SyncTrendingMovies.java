/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.sync.movies;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.TrendingItem;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncTrendingMovies extends CallJob<List<TrendingItem>> {

  private static final int LIMIT = 50;

  @Inject transient MoviesService moviesService;

  @Inject transient MovieDatabaseHelper movieHelper;

  @Override public String key() {
    return "SyncTrendingMovies";
  }

  @Override public int getPriority() {
    return JobPriority.SUGGESTIONS;
  }

  @Override public Call<List<TrendingItem>> getCall() {
    return moviesService.getTrendingMovies(LIMIT, Extended.FULL);
  }

  @Override public boolean handleResponse(List<TrendingItem> movies) {
    ContentResolver resolver = getContentResolver();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    Cursor c = resolver.query(Movies.TRENDING, new String[] {
        MovieColumns.ID,
    }, null, null, null);
    List<Long> trendingIds = new ArrayList<>();
    while (c.moveToNext()) {
      final long movieId = Cursors.getLong(c, MovieColumns.ID);
      trendingIds.add(movieId);
    }
    c.close();

    for (int i = 0, count = movies.size(); i < count; i++) {
      TrendingItem item = movies.get(i);
      Movie movie = item.getMovie();
      final long movieId = movieHelper.partialUpdate(movie);

      trendingIds.remove(movieId);

      ContentValues values = new ContentValues();
      values.put(MovieColumns.TRENDING_INDEX, i);
      ContentProviderOperation op =
          ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build();
      ops.add(op);
    }

    for (Long movieId : trendingIds) {
      ContentValues values = new ContentValues();
      values.put(MovieColumns.TRENDING_INDEX, -1);
      ContentProviderOperation op =
          ContentProviderOperation.newUpdate(Movies.withId(movieId)).withValues(values).build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
