package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PayPalConnectActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this, "Connect PayPal"));
        l.addView(Ui.body(this, "Pocket never asks for your PayPal password or verification code. Sign-in happens on PayPal's page. Pocket stores only the app's own verified connection token. PayPal account eligibility is enforced by PayPal."));
        l.addView(Ui.gap(this, 16));
        l.addView(Ui.button(this, "Connect with PayPal", v -> startConnect()));
        l.addView(Ui.button(this, "Disconnect", v -> { new SecureStore(this).remove("paypal_session"); SecurityEvents.record(this,"paypal_disconnected"); Toast.makeText(this,"Disconnected",Toast.LENGTH_SHORT).show(); }));
        setContentView(l);
    }

    private void startConnect() {
        String base = BuildConfig.PAYPAL_BACKEND_BASE_URL;
        if (base == null || base.isBlank()) {
            Toast.makeText(this, "PayPal backend is not configured in this build.", Toast.LENGTH_LONG).show();
            return;
        }
        String ret = URLEncoder.encode("pocket://oauth/paypal", StandardCharsets.UTF_8);
        Uri u = Uri.parse(base.replaceAll("/$", "") + "/oauth/paypal/start?return_uri=" + ret);
        startActivity(new Intent(Intent.ACTION_VIEW, u));
        SecurityEvents.record(this,"paypal_connect_started");
    }
}
