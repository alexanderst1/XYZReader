package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = ArticleListActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private AppBarLayout mAppBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        int columnCount = getResources().getInteger(R.integer.article_list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        mAdapter = new Adapter(this, null);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Log.i(LOG_TAG, "refresh called from SwipeRefreshLayout");
                refresh();
            }
        });

        mSwipeRefreshLayout.setColorSchemeResources(R.color.theme_accent_2,
                R.color.theme_accent_1, R.color.theme_primary);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                EnableOrDisableSwipeRefresh();
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                EnableOrDisableSwipeRefresh();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.refresh) {
            refresh();
        }
        return super.onOptionsItemSelected(item);
    }

    void EnableOrDisableSwipeRefresh() {
        boolean enabled = mAppBarLayout.getTop() >= 0;
        mSwipeRefreshLayout.setEnabled(enabled);
        //Log.i(LOG_TAG, "SwipeRefreshLayout " + (enabled ? "enabled" : "disabled"));
    }

    private void refresh() {
        if (mIsRefreshing)
            return;
        mIsRefreshing = true;
        updateRefreshingUI();
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.mCursorAdapter.swapCursor(cursor);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.mCursorAdapter.swapCursor(null);
        mAdapter.notifyDataSetChanged();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private CursorAdapter mCursorAdapter;
        Context mContext;

        public Adapter(Context context, Cursor c) {
            mContext = context;
            mCursorAdapter = new CursorAdapter(context, c, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    return null;
                }
                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                }
            };
        }

        @Override
        public long getItemId(int position) {
            if (mCursorAdapter == null)
                return 0;
            Cursor c = mCursorAdapter.getCursor();
            if (c == null)
                return 0;
            c.moveToPosition(position);
            return c.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final ViewHolder vh, int position) {

            Cursor c = mCursorAdapter.getCursor();
            c.moveToPosition(position);

            vh.titleView.setText(c.getString(ArticleLoader.Query.TITLE));
            CharSequence date = DateUtils.getRelativeTimeSpanString(
                    c.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            vh.subtitleView.setText(date + "\nby " + c.getString(ArticleLoader.Query.AUTHOR));

            ImageLoader imgLoader = ImageLoaderHelper.getInstance(ArticleListActivity.this)
                    .getImageLoader();
            String imgUrl = c.getString(ArticleLoader.Query.THUMB_URL);
            //vh.thumbnailView.setImageUrl(imgUrl, imgLoader);

            imgLoader.get(imgUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer cont, boolean b) {
                    Bitmap bm = cont.getBitmap();
                    if (bm != null) {
                        vh.thumbnailView.setImageBitmap(bm);
                        int color = Utils.getDarkMutedColor(bm);
                        vh.titleView.setBackgroundColor(color);
                        vh.subtitleView.setBackgroundColor(color);
                    }
                }
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                }
            });

            vh.thumbnailView.setAspectRatio(c.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            Cursor c = mCursorAdapter.getCursor();
            return c == null ? 0 : c.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
