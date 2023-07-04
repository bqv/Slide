package ccrama.me.slideyoutubeplugin.SwipeLayout.app;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ccrama.me.slideyoutubeplugin.SwipeLayout.SwipeBackLayout;
import ccrama.me.slideyoutubeplugin.SwipeLayout.Utils;
import ccrama.me.slidevideoplugin.R;

public class SwipeBackActivityHelper {
    /* access modifiers changed from: private */
    public Activity mActivity;
    private SwipeBackLayout mSwipeBackLayout;

    public SwipeBackActivityHelper(Activity activity) {
        this.mActivity = activity;
    }

    public void onActivityCreate() {
        this.mActivity.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        this.mActivity.getWindow().getDecorView().setBackgroundDrawable((Drawable) null);
        this.mSwipeBackLayout = (SwipeBackLayout) LayoutInflater.from(this.mActivity).inflate(R.layout.swipeback_layout, (ViewGroup) null);
        this.mSwipeBackLayout.addSwipeListener(new SwipeBackLayout.SwipeListener() {
            public void onScrollStateChange(int state, float scrollPercent) {
            }

            public void onEdgeTouch(int edgeFlag) {
                Utils.convertActivityToTranslucent(SwipeBackActivityHelper.this.mActivity);
            }

            public void onScrollOverThreshold() {
            }
        });
    }

    public void onPostCreate() {
        this.mSwipeBackLayout.attachToActivity(this.mActivity);
    }

    public View findViewById(int id) {
        if (this.mSwipeBackLayout != null) {
            return this.mSwipeBackLayout.findViewById(id);
        }
        return null;
    }

    public SwipeBackLayout getSwipeBackLayout() {
        return this.mSwipeBackLayout;
    }
}
