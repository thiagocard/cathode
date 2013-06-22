package net.simonvt.trakt.ui.fragment;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.sync.PriorityTraktTaskQueue;
import net.simonvt.trakt.sync.TraktTaskQueue;
import net.simonvt.trakt.sync.task.SyncTask;
import net.simonvt.trakt.ui.MoviesNavigationListener;
import net.simonvt.trakt.ui.adapter.MoviesAdapter;
import net.simonvt.trakt.util.LogWrapper;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SearchView;

import javax.inject.Inject;

public abstract class MoviesFragment extends AbsAdapterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MoviesFragment";

    @Inject TraktTaskQueue mQueue;

    @Inject PriorityTraktTaskQueue mPriorityQueue;

    private MoviesNavigationListener mNavigationListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mNavigationListener = (MoviesNavigationListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MoviesNavigationListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);
        setHasOptionsMenu(true);

        getLoaderManager().initLoader(getLoaderId(), null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies, container, false);
    }

    @Override
    public void onDestroy() {
        getLoaderManager().destroyLoader(getLoaderId());
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_movies, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                LogWrapper.v(TAG, "Query: " + query);
                mNavigationListener.onSearchMovie(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                mQueue.add(new SyncTask());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onItemClick(AdapterView l, View v, int position, long id) {
        mNavigationListener.onDisplayMovie(id);
    }

    void setCursor(Cursor cursor) {
        if (getAdapter() == null) {
            setAdapter(new MoviesAdapter(getActivity(), cursor));
        } else {
            ((MoviesAdapter) getAdapter()).changeCursor(cursor);
        }
    }

    protected abstract int getLoaderId();

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setAdapter(null);
    }
}
