package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

//import android.support.v7.graphics.Palette.Builder;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;
    private int mScreenWidth;

    private int mTopInset;
    private View mPhotoContainerView;
    private View mMetaBarView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);

        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_photo_height);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        mMetaBarView = mRootView.findViewById(R.id.meta_bar);
        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                // AlexSt: [original code, its purpose]:
                // when bottom edge of photograph comes close to left arrow button, move the button
                // up out of screen
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                // AlexSt: [original code, its purpose]:
                // ObservableScrollView takes care about scrolling photograph and article text.
                // The line below makes minor adjustment (decrease) to the value of scrolling
                // to create parallax effect
                float parallaxFactor = 1 + (mPhotoContainerView.getHeight() == 0 ?
                        0 : 1f * mMetaBarView.getHeight() / mPhotoContainerView.getHeight());
                // [original comment] Scroll photograph with parallax
                mPhotoContainerView.setTranslationY((int) mScrollY * (1 - 1 / parallaxFactor));
                updateOpacityAndColorOfStatusBar();
            }
        });

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mStatusBarColorDrawable = new ColorDrawable(0);

        bindViews(null);
        updateOpacityAndColorOfStatusBar();
        return mRootView;
    }

    // AlexSt: [original code, its purpose]:
    // On scrolling up, when bottom of photo comes close to status bar by the distance
    // of 3 status bar height, the status bar is gradually becoming opaque reaching
    // full opacity when bottom of the photo touches status bar
    private void updateOpacityAndColorOfStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private void bindViews(Cursor cursor) {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        final View metaBarView = mRootView.findViewById(R.id.meta_bar);
        final View photoContainerView = mRootView.findViewById(R.id.photo_container);
        final LinearLayout articleContainerView = (LinearLayout) mRootView.findViewById(R.id.article_container);
        final FrameLayout.LayoutParams articleViewLayoutParams =
                ((FrameLayout.LayoutParams)articleContainerView.getLayoutParams());

        if (cursor != null && cursor.getCount() > 0) {
            //AlexSt: make it visible with 0-Alpha (effectively invisible) so that user
            //would not see flickering while layout is being rendered
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);

            titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));
            // AlexSt: [original code, its purpose]:
            // Create string with published date printed in theme color and author name
            // in white color to accent author name.
            // If date text color is also white then having html font tag will not make any
            // difference of course...
            CharSequence subTitle = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + cursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>");
            bylineView.setText(subTitle);
            bodyView.setText(Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)));

            ImageLoader imgLoader = ImageLoaderHelper.getInstance(getActivity())
                    .getImageLoader();
            imgLoader.get(cursor.getString(ArticleLoader.Query.PHOTO_URL),
                    new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer cont, boolean b) {
                            Bitmap bm = cont.getBitmap();
                            if (bm != null) {
                                //AlexSt: Adjust height of photo container to fit image while keeping
                                //aspect ratio
                                int photoContainerWidth = mScreenWidth; //AlexSt: full-bleed images
                                float aspectRatio = (float)bm.getWidth() / (float)bm.getHeight();
                                int photoContainerHeight = (int)(photoContainerWidth / aspectRatio);

                                photoContainerView.getLayoutParams().height = photoContainerHeight;
                                articleViewLayoutParams.setMargins(0, photoContainerHeight, 0, 0);
                                articleContainerView.setLayoutParams(articleViewLayoutParams);
                                mRootView.invalidate();
                                mRootView.requestLayout();

                                mPhotoView.setImageBitmap(bm);

                                mMutedColor = Utils.getDarkMutedColor(bm);
                                metaBarView.setBackgroundColor(mMutedColor);

                                // AlexSt: [original code, its purpose]:
                                // this line probably needed if one changes design to NOT have
                                // photo to completely cover photo container
                                photoContainerView.setBackgroundColor(mMutedColor);

                                mStatusBarFullOpacityBottom = photoContainerHeight;
                                updateOpacityAndColorOfStatusBar();

                                // AlexSt: now layout is rendered and it is a good time
                                // to make it visible
                                mRootView.animate().alpha(1);

                            }
                        }
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            return;
        }
        if (cursor != null) {
            if (cursor.getCount() != 0 && !cursor.moveToFirst())
                Log.e(TAG, "Error reading item detail cursor");
        }

        bindViews(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        bindViews(null);
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }
        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }
}
