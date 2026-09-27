package co.pocket.companion;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.Toast;

public final class NfcWalletActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); render();
    }

    private void render() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        boolean supported = nfc != null; boolean enabled = supported && nfc.isEnabled();
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this, "Contactless / NFC"));
        l.addView(Ui.body(this, "NFC hardware: " + (supported ? "available" : "not available") + "\nNFC: " + (enabled ? "on" : "off") + "\nCertified token provider: " + (BuildConfig.PAYMENT_PROVIDER_ENABLED ? "configured" : "not configured")));
        l.addView(Ui.gap(this, 14));
        l.addView(Ui.body(this, "Android can route payment-category NFC taps to a wallet app. Pocket only enables its payment service when a certified issuer/TSP integration is supplied. Without that token provider, the app must not pretend a raw card number is a contactless card."));
        l.addView(Ui.gap(this, 14));
        if (supported && !enabled) l.addView(Ui.button(this,"Turn on NFC", v -> startActivity(new Intent(Settings.ACTION_NFC_SETTINGS))));
        if (Build.VERSION.SDK_INT >= 35) l.addView(Ui.button(this,"Wallet role settings", v -> requestWalletRole()));
        else l.addView(Ui.button(this,"Contactless payment settings", v -> startActivity(new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS))));
        setContentView(l);
    }

    private void requestWalletRole() {
        if (!BuildConfig.PAYMENT_PROVIDER_ENABLED) { Toast.makeText(this,"Provider must be configured before Pocket can become a payment wallet.",Toast.LENGTH_LONG).show(); return; }
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_WALLET) && !rm.isRoleHeld(RoleManager.ROLE_WALLET)) {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_WALLET), 44);
        } else Toast.makeText(this,"Wallet role is already set or unavailable on this device.",Toast.LENGTH_LONG).show();
    }
}
