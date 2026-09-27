package co.pocket.companion;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

final class NotificationHealth {
    record Status(boolean listenerEnabled, boolean appInstalled, long lastSeen, String label) {
        String summary() {
            if (!listenerEnabled) return "Needs notification access";
            if (!appInstalled) return "App not detected";
            if (lastSeen == 0) return "Ready; waiting for the first notification";
            long mins = Math.max(0, (System.currentTimeMillis()-lastSeen)/60000L);
            if (mins < 2) return "Working; notification seen just now";
            if (mins < 60) return "Working; last seen " + mins + " min ago";
            long hrs = mins / 60; if (hrs < 48) return "Working; last seen " + hrs + " h ago";
            return "Permission is ready; last seen " + (hrs/24) + " days ago";
        }
    }

    static boolean listenerEnabled(Context c) {
        String flat = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) return false;
        for (String s : flat.split(":")) {
            ComponentName n = ComponentName.unflattenFromString(s);
            if (n != null && c.getPackageName().equals(n.getPackageName())) return true;
        }
        return false;
    }

    static boolean installed(Context c, String pkg) {
        try { c.getPackageManager().getPackageInfo(pkg, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    static Status status(Context c, String pkg, String label) {
        return new Status(listenerEnabled(c), installed(c,pkg), new PocketDb(c).lastSeen(pkg), label);
    }
}
