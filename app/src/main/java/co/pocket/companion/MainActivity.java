package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (!getSharedPreferences("pocket_settings", MODE_PRIVATE).getBoolean("registered", false)) {
            startActivity(new Intent(this, RegisterActivity.class)); finish(); return;
        }
        render();
    }
    @Override protected void onResume() { super.onResume(); if (!isFinishing()) render(); }

    private void render() {
        ScrollView s = new ScrollView(this); LinearLayout l = Ui.column(this); s.addView(l);
        l.addView(Ui.title(this,"Pocket"));
        l.addView(Ui.body(this,"A little clarity for your money."));
        l.addView(Ui.gap(this,16));

        l.addView(Ui.h2(this,"Connection health"));
        NotificationHealth.Status wallet = NotificationHealth.status(this, SourceRules.GOOGLE_WALLET, "Google Wallet");
        NotificationHealth.Status paypal = NotificationHealth.status(this, SourceRules.PAYPAL, "PayPal");
        String bankPkg = getSharedPreferences("pocket_settings",MODE_PRIVATE).getString("bank_package","");
        boolean listener = NotificationHealth.listenerEnabled(this);
        l.addView(Ui.body(this,"Notification access: " + (listener ? "enabled" : "needs permission")));
        l.addView(Ui.body(this,"Google Wallet: " + wallet.summary()));
        String ppLink = "connection unavailable in this build";
        l.addView(Ui.body(this,"PayPal: " + ppLink + "; notifications: " + paypal.summary()));
        if (bankPkg.isBlank()) l.addView(Ui.body(this,"Bank: no notification source selected yet"));
        else l.addView(Ui.body(this,"Bank: " + NotificationHealth.status(this, bankPkg, "Bank").summary()));

        if (!listener) l.addView(Ui.button(this,"Enable notification access", v -> startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        l.addView(Ui.body(this,"PayPal account connection is unavailable. PayPal accounts require adult eligibility."));
        l.addView(Ui.button(this,"Add card securely", v -> startActivity(new Intent(this,CardProvisioningActivity.class))));
        l.addView(Ui.button(this,"NFC / contactless status", v -> startActivity(new Intent(this,NfcWalletActivity.class))));

        l.addView(Ui.gap(this,18)); l.addView(Ui.h2(this,"Spending tools"));
        l.addView(Ui.body(this,"Test build: notification checks and local registration. Budgets, reports and payment execution are not available yet."));
        l.addView(Ui.gap(this,12));
        l.addView(Ui.h2(this,"Important"));
        l.addView(Ui.body(this,"A notification permission check can prove Pocket is allowed to listen and whether a source has been seen. It cannot force a bank, Google Wallet or PayPal to generate a notification. A real transaction remains the final end-to-end test."));
        setContentView(s);
    }
}
