package co.pocket.companion;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayDeque;
import java.util.Deque;

final class SecurityEvents {
    private static final String PREF = "pocket_security_events";
    private SecurityEvents() {}

    static synchronized void record(Context c, String event) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = p.getString("events", "");
        Deque<String> q = new ArrayDeque<>();
        if (!raw.isEmpty()) for (String s : raw.split("\\n")) if (!s.isEmpty()) q.addLast(s);
        q.addLast(System.currentTimeMillis() + "|" + event.replace("\n", " "));
        while (q.size() > 100) q.removeFirst();
        p.edit().putString("events", String.join("\n", q)).apply();
    }
}
