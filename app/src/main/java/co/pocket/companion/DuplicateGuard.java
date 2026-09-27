package co.pocket.companion;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class DuplicateGuard {
    private DuplicateGuard() {}
    static boolean seenRecently(Context c, String pkg, long cents, String merchant, long when) {
        try {
            String key = hex(MessageDigest.getInstance("SHA-256").digest((pkg+"|"+cents+"|"+merchant.toLowerCase()).getBytes(StandardCharsets.UTF_8)));
            SharedPreferences p = c.getSharedPreferences("pocket_dupes", Context.MODE_PRIVATE);
            long last = p.getLong(key, 0L); p.edit().putLong(key, when).apply();
            return last > 0 && Math.abs(when - last) < 120_000L;
        } catch (Exception e) { return false; }
    }
    private static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x:b) s.append(String.format("%02x", x)); return s.toString(); }
}
