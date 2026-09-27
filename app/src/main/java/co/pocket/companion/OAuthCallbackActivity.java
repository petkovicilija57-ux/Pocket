package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public final class OAuthCallbackActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Uri u = getIntent().getData(); String session = u == null ? null : u.getQueryParameter("session");
        if (session == null || session.length() < 16) { fail("Invalid PayPal callback"); return; }
        verify(session);
    }

    private void verify(String session) {
        String base = BuildConfig.PAYPAL_BACKEND_BASE_URL;
        if (base == null || base.isBlank()) { fail("PayPal backend not configured"); return; }
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean ok = false;
            try {
                URL url = new URL(base.replaceAll("/$", "") + "/oauth/paypal/session/" + java.net.URLEncoder.encode(session, java.nio.charset.StandardCharsets.UTF_8));
                HttpURLConnection c = (HttpURLConnection) url.openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setRequestMethod("GET");
                ok = c.getResponseCode() == 200;
            } catch (Exception ignored) {}
            boolean finalOk = ok;
            runOnUiThread(() -> {
                if (finalOk) {
                    new SecureStore(this).put("paypal_session", session);
                    SecurityEvents.record(this,"paypal_connected");
                    Toast.makeText(this,"PayPal connected successfully",Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class)); finish();
                } else fail("Could not verify PayPal connection");
            });
        });
    }

    private void fail(String msg) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show(); finish(); }
}
