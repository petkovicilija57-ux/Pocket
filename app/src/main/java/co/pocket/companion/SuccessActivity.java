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
        l.addView(Ui.body(this, "Your local Pocket profile was created successfully. You can now select your bank app and enable notification access. This preview does not execute payments."));
        l.addView(Ui.gap(this, 18));
        l.addView(Ui.button(this, "Continue to Pocket", v -> { startActivity(new Intent(this, MainActivity.class)); finish(); }));
        setContentView(l);
    }
}
