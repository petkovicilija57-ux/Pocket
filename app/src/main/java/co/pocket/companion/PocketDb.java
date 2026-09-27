package co.pocket.companion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class PocketDb extends SQLiteOpenHelper {
    PocketDb(Context c) { super(c, "pocket.db", null, 5); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE payments(id TEXT PRIMARY KEY, amount_cents INTEGER NOT NULL, currency TEXT NOT NULL, merchant TEXT NOT NULL, category TEXT NOT NULL, source TEXT NOT NULL, created INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE budgets(month TEXT PRIMARY KEY, limit_cents INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE subscriptions(id TEXT PRIMARY KEY, name TEXT NOT NULL, amount_cents INTEGER NOT NULL, next_due INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE goals(id TEXT PRIMARY KEY, name TEXT NOT NULL, target_cents INTEGER NOT NULL, saved_cents INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE notification_sources(package_name TEXT PRIMARY KEY, label TEXT, last_seen INTEGER NOT NULL DEFAULT 0, seen_count INTEGER NOT NULL DEFAULT 0)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notification_sources(package_name TEXT PRIMARY KEY, label TEXT, last_seen INTEGER NOT NULL DEFAULT 0, seen_count INTEGER NOT NULL DEFAULT 0)");
        }
    }

    void addPayment(long cents, String currency, String merchant, String category, String source, long time) {
        ContentValues v = new ContentValues();
        v.put("id", UUID.randomUUID().toString()); v.put("amount_cents", cents); v.put("currency", currency);
        v.put("merchant", merchant); v.put("category", category); v.put("source", source); v.put("created", time);
        getWritableDatabase().insertOrThrow("payments", null, v);
    }

    void noteSource(String pkg, String label, long time) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("INSERT OR IGNORE INTO notification_sources(package_name,label,last_seen,seen_count) VALUES(?,?,0,0)", new Object[]{pkg,label});
            db.execSQL("UPDATE notification_sources SET label=?,last_seen=?,seen_count=seen_count+1 WHERE package_name=?", new Object[]{label,time,pkg});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    long lastSeen(String pkg) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT last_seen FROM notification_sources WHERE package_name=?", new String[]{pkg})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    List<String> observedSources() {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT package_name,label,last_seen,seen_count FROM notification_sources ORDER BY last_seen DESC LIMIT 30", null)) {
            while (c.moveToNext()) out.add(c.getString(0) + "|" + c.getString(1) + "|" + c.getLong(2) + "|" + c.getLong(3));
        }
        return out;
    }
}
