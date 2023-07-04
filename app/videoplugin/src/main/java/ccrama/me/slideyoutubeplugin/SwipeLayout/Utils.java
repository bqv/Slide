package ccrama.me.slideyoutubeplugin.SwipeLayout;

import android.app.Activity;
import android.app.ActivityOptions;
import android.os.Build;
import java.lang.reflect.Method;

public class Utils {
    private Utils() {
    }

    public static void convertActivityFromTranslucent(Activity activity) {
        try {
            Method method = Activity.class.getDeclaredMethod("convertFromTranslucent", new Class[0]);
            method.setAccessible(true);
            method.invoke(activity, new Object[0]);
        } catch (Throwable th) {
        }
    }

    public static void convertActivityToTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            convertActivityToTranslucentAfterL(activity);
        } else {
            convertActivityToTranslucentBeforeL(activity);
        }
    }

    public static void convertActivityToTranslucentBeforeL(Activity activity) {
        try {
            Class<?> translucentConversionListenerClazz = null;
            for (Class<?> clazz : Activity.class.getDeclaredClasses()) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method method = Activity.class.getDeclaredMethod("convertToTranslucent", new Class[]{translucentConversionListenerClazz});
            method.setAccessible(true);
            method.invoke(activity, new Object[]{null});
        } catch (Throwable th) {
        }
    }

    private static void convertActivityToTranslucentAfterL(Activity activity) {
        try {
            Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions", new Class[0]);
            getActivityOptions.setAccessible(true);
            Object options = getActivityOptions.invoke(activity, new Object[0]);
            Class<?> translucentConversionListenerClazz = null;
            for (Class<?> clazz : Activity.class.getDeclaredClasses()) {
                if (clazz.getSimpleName().contains("TranslucentConversionListener")) {
                    translucentConversionListenerClazz = clazz;
                }
            }
            Method convertToTranslucent = Activity.class.getDeclaredMethod("convertToTranslucent", new Class[]{translucentConversionListenerClazz, ActivityOptions.class});
            convertToTranslucent.setAccessible(true);
            convertToTranslucent.invoke(activity, new Object[]{null, options});
        } catch (Throwable th) {
        }
    }
}
