package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public final class SuccessActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this, "Successful"));
        l.addView(Ui.gap(this, 12));
        l.addView(Ui.body(this, "Your Pocket account was created successfully. Next you can enable payment notifications, connect an eligible PayPal account, or configure tokenized contactless payments."));
        l.addView(Ui.gap(this, 18));
        l.addView(Ui.button(this, "Continue to Pocket", v -> { startActivity(new Intent(this, MainActivity.class)); finish(); }));
        setContentView(l);
    }
}
