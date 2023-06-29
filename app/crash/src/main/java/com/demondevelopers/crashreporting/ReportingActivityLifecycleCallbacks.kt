package com.demondevelopers.crashreporting

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Only required if you want the capability to capture screen-shots during the crash.
 */
class ReportingActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    private var currentActivity: Activity? = null

    companion object {
        val instance: ReportingActivityLifecycleCallbacks = ReportingActivityLifecycleCallbacks()

        val currentActivity: Activity?
            get() = instance.currentActivity
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}
