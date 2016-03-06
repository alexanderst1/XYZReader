package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowInsets;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;
    private View mFab;
    private int mIsFabVisible = -1; //AlexSt: undetermined state, 1-visible, 0-invisible
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        // AlexSt: [original code, its purpose]:
        // http://developer.android.com/training/system-ui/status.html
        // "On Android 4.1 and higher, you can set your application's content to appear behind
        // the status bar, so that the content doesn't resize as the status bar hides and shows"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        getLoaderManager().initLoader(0, null, this);

        // AlexSt: [original code, its purpose]:
        // Set pager adapter which internally will use ArticleDetailFragment for every page
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        // AlexSt: [original code, its purpose]:
        // Could not figure out what is it for, didn't notice any difference after commenting out
        // this code
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // AlexSt: [original code, its purpose]:
                // Fade out left arrow button while scrolling pages and fade in when the scrolling is completed
                float animState = (state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f;
                mUpButton.animate().alpha(animState).setDuration(300);

                // AlexSt: make FAB invisible while scrolling pages using animation
                //isFabVisible check added to avoid flickering while scrolling
                int isFabVisible = (state == ViewPager.SCROLL_STATE_IDLE) ? 1 : 0;
                if (mIsFabVisible != isFabVisible) {
                    mIsFabVisible = isFabVisible;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        animateViewToVisibility(mFab, isFabVisible == 1);
                    } else {
                        // AlexSt: Fade out FAB button while scrolling pages and fade in when the scrolling is completed
                        mFab.animate().alpha(animState).setDuration(300);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    // AlexSt: [original code, its purpose]:
                    // Move cursor to new page data to show new page
                    mCursor.moveToPosition(position);

                    mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                }
                updateUpButtonPosition();
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        // AlexSt: [original code, its purpose]:
        // Return to previous activity when touching up button
        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //onSupportNavigateUp(); //AlexSt: commented this out
                //call below restores prev activity to the same state where it
                //was left off: scrolled to the same position and no redundant refreshing
                onBackPressed();
            }
        });

        mFab = findViewById(R.id.share_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //AlexSt: share text of article title, data and author
                String shareText = "I would like to share an article \"" +
                        mCursor.getString(ArticleLoader.Query.TITLE) + "\", " +
                        DateUtils.formatDateTime(mContext,
                                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                DateUtils.FORMAT_NUMERIC_DATE)
                        + " by " + mCursor.getString(ArticleLoader.Query.AUTHOR) +
                        ".\nHope you'll enjoy it...";

                startActivity(Intent.createChooser(ShareCompat.IntentBuilder
                        .from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText(shareText)
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        setOnApplyWindowInsetsListener();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateViewToVisibility(final View view, boolean visible) {
        //http://developer.android.com/training/material/animations.html#Reveal

        // get the center for the clipping circle

        //AlexSt: offset 10% to the right to get approximately to the center of share sign
        int cx = (int)(view.getWidth() * 1.1f / 2);
        int cy = view.getHeight() / 2;

        // get the final radius for the clipping circle
        float radius = (float) Math.hypot(cx, cy);

        Animator anim;

        if (visible) {
            // create the animator for this view (the start radius is zero)
            anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, radius);
            // make the view visible and start the animation
            view.setVisibility(View.VISIBLE);
        } else {
            // create the animation (the final radius is zero)
            anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0);
            // make the view invisible when the animation is done
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.INVISIBLE);
                }
            });
        }
        anim.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setOnApplyWindowInsetsListener()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    // AlexSt: [original code, its purpose]:
                    // Move left arrow container down by the height of system bar, otherwise the
                    // arrow will be too close to system bar
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // AlexSt: [original code, its purpose]:
        // Find cursor row index which corresponds to ID of the article which
        // user wants to see and tell the pager know that row index so that pager could
        // show the article

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        // AlexSt: [original code, its purpose]:
        // when bottom edge of photograph comes close to left arrow button, move the button up
        // out of screen
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;
            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
