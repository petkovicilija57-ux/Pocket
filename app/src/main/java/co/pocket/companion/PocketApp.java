package co.pocket.companion;

import android.app.Application;

public final class PocketApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        SecurityEvents.record(this, "app_start");
    }
}
