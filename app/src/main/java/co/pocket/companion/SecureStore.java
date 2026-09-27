package co.pocket.companion;

import android.content.Context;
import android.content.SharedPreferences;

final class SecureStore {
    private final SharedPreferences p;
    SecureStore(Context c) { p = c.getSharedPreferences("pocket_secure", Context.MODE_PRIVATE); }

    void put(String k, String value) {
        try { p.edit().putString(k, CryptoBox.encrypt(value)).apply(); }
        catch (Exception e) { throw new IllegalStateException("Secure storage unavailable", e); }
    }
    String get(String k) {
        String v = p.getString(k, null); if (v == null) return null;
        try { return CryptoBox.decrypt(v); }
        catch (Exception e) { return null; }
    }
    void remove(String k) { p.edit().remove(k).apply(); }
}
