package ccrama.me.slideyoutubeplugin;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;

import ccrama.me.slideyoutubeplugin.SwipeLayout.SwipeBackLayout;
import ccrama.me.slideyoutubeplugin.SwipeLayout.Utils;
import ccrama.me.slideyoutubeplugin.SwipeLayout.app.SwipeBackActivityBase;
import ccrama.me.slideyoutubeplugin.SwipeLayout.app.SwipeBackActivityHelper;
import ccrama.me.slidevideoplugin.R;

import com.google.android.youtube.player.YouTubeBaseActivity;

public class BaseYouTubeView extends YouTubeBaseActivity implements SwipeBackActivityBase {
    protected boolean enableSwipeBackLayout = true;
    @Nullable
    protected SwipeBackActivityHelper mHelper;
    protected boolean overrideRedditSwipeAnywhere = false;
    protected boolean overrideSwipeFromAnywhere = false;

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.fade_out);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slideright, 0);
        if (this.enableSwipeBackLayout) {
            this.mHelper = new SwipeBackActivityHelper(this);
            this.mHelper.onActivityCreate();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            this.mHelper.getSwipeBackLayout().mDragHelper.override = true;
            this.mHelper.getSwipeBackLayout().setEdgeSize(metrics.widthPixels);
        }
    }

    /* access modifiers changed from: protected */
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.enableSwipeBackLayout) {
            this.mHelper.onPostCreate();
        }
    }

    public View findViewById(int id) {
        View v = super.findViewById(id);
        if (v != null || this.mHelper == null) {
            return v;
        }
        return this.mHelper.findViewById(id);
    }

    public SwipeBackLayout getSwipeBackLayout() {
        if (this.enableSwipeBackLayout) {
            return this.mHelper.getSwipeBackLayout();
        }
        return null;
    }

    public void setSwipeBackEnable(boolean enable) {
        if (this.enableSwipeBackLayout) {
            getSwipeBackLayout().setEnableGesture(enable);
        }
    }

    public void scrollToFinishActivity() {
        if (this.enableSwipeBackLayout) {
            Utils.convertActivityToTranslucent(this);
            getSwipeBackLayout().scrollToFinishActivity();
        }
    }

    /* access modifiers changed from: protected */
    public void disableSwipeBackLayout() {
        this.enableSwipeBackLayout = false;
    }

    /* access modifiers changed from: protected */
    public void overrideSwipeFromAnywhere() {
        this.overrideSwipeFromAnywhere = true;
    }

    /* access modifiers changed from: protected */
    public void overrideRedditSwipeAnywhere() {
        this.overrideRedditSwipeAnywhere = true;
    }
}
