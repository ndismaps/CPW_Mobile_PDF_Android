package com.dnrcpw.cpwmobilepdf.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
public class ToastUtils {
    /**
     * Shows a Toast message that stays on screen for an extended period.
     * Automatically handles background thread safety.
     *
     * @param context        The Android Context (Activity, Service, or Application)
     * @param message        The string text to display
     */

    public static void showExtendedToast(final Context context, final String message) {
        final int durationMillis = 7000; // Total duration in milliseconds (e.g., 7000 for 7 seconds)

        // Safe check to prevent NullPointerExceptions if context is missing
        if (context == null || message == null) return;

        // Force execution onto Android's main UI Thread (safe to call from dbExecutor background threads!)
        new Handler(Looper.getMainLooper()).post(() -> {
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.show();

            // Calculate how many times to re-trigger the toast (LENGTH_LONG lasts ~3.5 seconds)
            int iterations = durationMillis / 3500;

            for (int i = 1; i <= iterations; i++) {
                new Handler(Looper.getMainLooper()).postDelayed(toast::show, i * 3500);
            }
        });
    }
}