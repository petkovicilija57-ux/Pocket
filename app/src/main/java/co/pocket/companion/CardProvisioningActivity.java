package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

public final class CardProvisioningActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this, "Add a card"));
        l.addView(Ui.body(this, "Pocket does not store a card number, CVC, PIN, or magnetic-stripe data. A live contactless card must be provisioned as a network/device token by an approved issuer or token-service provider. That provider returns tokenized credentials to the wallet; the original card number stays out of Pocket."));
        l.addView(Ui.gap(this, 16));
        l.addView(Ui.button(this, "Start secure provisioning", v -> {
            String url = BuildConfig.CARD_PROVISIONING_URL;
            if (url == null || url.isBlank()) { Toast.makeText(this,"No certified card-provisioning provider is configured in this build.",Toast.LENGTH_LONG).show(); return; }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }));
        l.addView(Ui.button(this, "NFC wallet status", v -> startActivity(new Intent(this,NfcWalletActivity.class))));
        setContentView(l);
    }
}
