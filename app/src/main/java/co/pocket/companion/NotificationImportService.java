package co.pocket.companion;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class NotificationImportService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        String pkg = sbn.getPackageName();
        if (!SourceRules.isEnabledSource(this, pkg)) return;
        Notification n = sbn.getNotification();
        if (n.extras == null) return;
        CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence x = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        String title = t == null ? "" : t.toString();
        String text = x == null ? "" : x.toString();
        long when = sbn.getPostTime();
        try (PocketDb db = new PocketDb(this)) {
        db.noteSource(pkg, SourceRules.label(pkg), when);

        if (!SourceRules.isEnabledSource(this, pkg)) return;
        NotificationParser.Parsed p = NotificationParser.parse(title, text);
        if (p == null) return;
        if (DuplicateGuard.seenRecently(this, pkg, p.cents(), p.merchant(), when)) return;
        db.addPayment(p.cents(), p.currency(), p.merchant(), "Other", SourceRules.label(pkg), when);
        SecurityEvents.record(this, "notification_import:" + pkg);
        }
    }
}
