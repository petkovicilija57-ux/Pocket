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
    PocketDb(Context c, String name) { super(c, name, null, 5); }

    record Payment(String id, long cents, String merchant, String category, long time) {}
    record Goal(String id, String name, long target, long saved) {}
    record Subscription(String id, String name, long cents, long due) {}

    List<Payment> payments() {
        List<Payment> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,amount_cents,merchant,category,created FROM payments WHERE currency='BAM' ORDER BY created DESC", null)) {
            while (c.moveToNext()) rows.add(new Payment(c.getString(0), c.getLong(1), c.getString(2), c.getString(3), c.getLong(4)));
        }
        return rows;
    }

    void savePayment(String id, long cents, String merchant, String category, long time) {
        if (cents <= 0 || merchant.trim().isEmpty()) throw new IllegalArgumentException("Naziv i pozitivan iznos su obavezni");
        if (id == null) { addPayment(cents, "BAM", merchant, category, "Manual", time); return; }
        ContentValues v = new ContentValues(); v.put("amount_cents", cents); v.put("merchant", merchant); v.put("category", category); v.put("created", time);
        getWritableDatabase().update("payments", v, "id=?", new String[]{id});
    }

    long budget(String month) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT limit_cents FROM budgets WHERE month=?", new String[]{month})) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }
    void setBudget(String month, long cents) {
        ContentValues v = new ContentValues(); v.put("month", month); v.put("limit_cents", cents);
        getWritableDatabase().insertWithOnConflict("budgets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
    List<Goal> goals() {
        List<Goal> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,target_cents,saved_cents FROM goals ORDER BY name", null)) {
            while (c.moveToNext()) rows.add(new Goal(c.getString(0),c.getString(1),c.getLong(2),c.getLong(3)));
        } return rows;
    }
    void saveGoal(String id, String name, long target, long saved) {
        if (name.trim().isEmpty() || target <= 0 || saved < 0) throw new IllegalArgumentException("Provjeri naziv i iznose");
        ContentValues v = new ContentValues(); v.put("id", id == null ? UUID.randomUUID().toString() : id); v.put("name",name); v.put("target_cents",target); v.put("saved_cents",saved);
        getWritableDatabase().insertWithOnConflict("goals", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
    List<Subscription> subscriptions() {
        List<Subscription> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,amount_cents,next_due FROM subscriptions WHERE active=1 ORDER BY next_due", null)) {
            while (c.moveToNext()) rows.add(new Subscription(c.getString(0),c.getString(1),c.getLong(2),c.getLong(3)));
        } return rows;
    }
    void saveSubscription(String id, String name, long cents, long due) {
        if (name.trim().isEmpty() || cents <= 0) throw new IllegalArgumentException("Provjeri naziv i iznos");
        ContentValues v = new ContentValues(); v.put("id",id == null ? UUID.randomUUID().toString() : id); v.put("name",name); v.put("amount_cents",cents); v.put("next_due",due);
        getWritableDatabase().insertWithOnConflict("subscriptions",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    void delete(String table, String id) {
        if (!java.util.Arrays.asList("payments","goals","subscriptions").contains(table)) throw new IllegalArgumentException("Unknown table");
        getWritableDatabase().delete(table,"id=?",new String[]{id});
    }

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

    List<String> recentPayments() {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT amount_cents,currency,merchant,created FROM payments ORDER BY created DESC LIMIT 50", null)) {
            while (c.moveToNext()) out.add(String.format(java.util.Locale.getDefault(), "%s  %d.%02d %s  %s", c.getString(2), c.getLong(0) / 100, c.getLong(0) % 100, c.getString(1), java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(c.getLong(3)))));
        }
        return out;
    }
}
