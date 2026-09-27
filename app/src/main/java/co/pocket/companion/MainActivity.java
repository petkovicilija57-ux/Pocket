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
        l.addView(Ui.button(this,"Select bank app", v -> selectBank()));

        if (!listener) l.addView(Ui.button(this,"Enable notification access", v -> startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))));
        l.addView(Ui.body(this,"PayPal account connection is unavailable. PayPal accounts require adult eligibility."));
        l.addView(Ui.button(this,"Add card securely", v -> startActivity(new Intent(this,CardProvisioningActivity.class))));
        l.addView(Ui.button(this,"NFC / contactless status", v -> startActivity(new Intent(this,NfcWalletActivity.class))));

        l.addView(Ui.gap(this,18)); l.addView(Ui.h2(this,"Spending tools"));
        try (PocketDb db = new PocketDb(this)) {
            java.util.List<String> payments = db.recentPayments();
            if (payments.isEmpty()) l.addView(Ui.body(this,"No imported payments yet."));
            for (String payment : payments) l.addView(Ui.body(this, payment));
        }
        l.addView(Ui.gap(this,12));
        l.addView(Ui.h2(this,"Important"));
        l.addView(Ui.body(this,"A notification permission check can prove Pocket is allowed to listen and whether a source has been seen. It cannot force a bank, Google Wallet or PayPal to generate a notification. A real transaction remains the final end-to-end test."));
        setContentView(s);
    }

    private void selectBank() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        java.util.List<android.content.pm.ResolveInfo> apps = getPackageManager().queryIntentActivities(launcher, 0);
        java.util.TreeMap<String,String> choices = new java.util.TreeMap<>();
        for (android.content.pm.ResolveInfo app : apps) {
            String pkg = app.activityInfo.packageName;
            if (!pkg.equals(getPackageName())) choices.put(app.loadLabel(getPackageManager()) + " (" + pkg + ")", pkg);
        }
        String[] labels = choices.keySet().toArray(new String[0]);
        new android.app.AlertDialog.Builder(this).setTitle("Select your bank application")
            .setItems(labels, (dialog, index) -> {
                getSharedPreferences("pocket_settings", MODE_PRIVATE).edit().putString("bank_package", choices.get(labels[index])).apply();
                render();
            }).setNeutralButton("Clear selection", (dialog, which) -> {
                getSharedPreferences("pocket_settings", MODE_PRIVATE).edit().remove("bank_package").apply(); render();
            }).setNegativeButton("Cancel", null).show();
    }
}
