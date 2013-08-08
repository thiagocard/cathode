package net.simonvt.trakt.remote.sync;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.service.UserService;
import net.simonvt.trakt.provider.ShowWrapper;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.provider.TraktDatabase;
import net.simonvt.trakt.remote.TraktTask;
import net.simonvt.trakt.util.LogWrapper;
import retrofit.RetrofitError;

public class SyncShowsWatchlistTask extends TraktTask {

  private static final String TAG = "SyncShowsWatchlistTask";

  @Inject transient UserService userService;

  @Override
  protected void doTask() {
    LogWrapper.v(TAG, "[doTask]");

    try {
      Cursor c =
          service.getContentResolver().query(TraktContract.Shows.SHOWS_WATCHLIST, new String[] {
              TraktDatabase.Tables.SHOWS + "." + TraktContract.Shows._ID,
          }, null, null, null);

      List<Long> showIds = new ArrayList<Long>();

      while (c.moveToNext()) {
        showIds.add(c.getLong(c.getColumnIndex(TraktContract.Shows._ID)));
      }
      c.close();

      List<TvShow> shows = userService.watchlistShows();

      for (TvShow show : shows) {
        final int tvdbId = show.getTvdbId();
        final long showId = ShowWrapper.getShowId(service.getContentResolver(), tvdbId);

        if (showId != -1) {
          ShowWrapper.setIsInWatchlist(service.getContentResolver(), tvdbId, true);
          showIds.remove(showId);
        } else {
          queueTask(new SyncShowTask(tvdbId));
        }
      }

      for (Long showId : showIds) {
        ShowWrapper.setIsInWatchlist(service.getContentResolver(), showId, false);
      }

      postOnSuccess();
    } catch (RetrofitError e) {
      postOnFailure();
      e.printStackTrace();
    }
  }
}