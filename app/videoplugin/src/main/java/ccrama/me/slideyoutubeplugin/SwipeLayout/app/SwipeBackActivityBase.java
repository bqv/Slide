package ccrama.me.slideyoutubeplugin.SwipeLayout.app;

import ccrama.me.slideyoutubeplugin.SwipeLayout.SwipeBackLayout;

public interface SwipeBackActivityBase {
    SwipeBackLayout getSwipeBackLayout();

    void scrollToFinishActivity();

    void setSwipeBackEnable(boolean z);
}
