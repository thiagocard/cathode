package net.simonvt.trakt.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.MovieWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncMoviesCollectionTask extends TraktTask {

  private static final String TAG = "SyncMoviesCollectionTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      Cursor c =
          service.getContentResolver().query(TraktContract.Movies.CONTENT_URI, new String[] {
              TraktContract.Movies._ID,
          }, TraktContract.Movies.IN_COLLECTION, null, null);

      List<Long> movieIds = new ArrayList<Long>(c.getCount());

      while (c.moveToNext()) {
        movieIds.add(c.getLong(0));
      }
      c.close();

      List<Movie> movies = userService.moviesCollection(DetailLevel.MIN);

      for (Movie movie : movies) {
        final Long tmdbId = movie.getTmdbId();
        final long movieId = MovieWrapper.getMovieId(service.getContentResolver(), tmdbId);

        if (movieId == -1) {
          queueTask(new SyncMovieTask(tmdbId));
        } else {
          MovieWrapper.setIsInCollection(service.getContentResolver(), movieId, true);
          movieIds.remove(movieId);
        }
      }

      for (Long movieId : movieIds) {
        MovieWrapper.setIsInCollection(service.getContentResolver(), movieId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      e.printStackTrace();
      postOnFailure();
    }
  }
}