package co.pocket.companion;

import android.content.Context;
import android.content.SharedPreferences;

final class SourceRules {
    static final String GOOGLE_WALLET = "com.google.android.apps.walletnfcrel";
    static final String PAYPAL = "com.paypal.android.p2pmobile";
    private SourceRules() {}

    static boolean isEnabledSource(Context c, String pkg) {
        if (GOOGLE_WALLET.equals(pkg) || PAYPAL.equals(pkg)) return true;
        String bank = c.getSharedPreferences("pocket_settings", Context.MODE_PRIVATE).getString("bank_package", "");
        return !bank.isEmpty() && bank.equals(pkg);
    }
    static String label(String pkg) {
        if (GOOGLE_WALLET.equals(pkg)) return "Google Wallet";
        if (PAYPAL.equals(pkg)) return "PayPal";
        return "Bank";
    }
}
