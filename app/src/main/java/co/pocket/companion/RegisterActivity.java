package co.pocket.companion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public final class RegisterActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = Ui.column(this);
        l.addView(Ui.title(this, "Create Pocket account"));
        l.addView(Ui.body(this, "Local account. Pocket never needs your bank, PayPal, or card password."));
        EditText pass = new EditText(this); pass.setHint("Password (12+ characters)"); pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); l.addView(pass);
        EditText confirm = new EditText(this); confirm.setHint("Confirm password"); confirm.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); l.addView(confirm);
        l.addView(Ui.button(this, "Create account", v -> {
            String p = pass.getText().toString(); String c = confirm.getText().toString();
            if (p.length() < 12) { Toast.makeText(this,"Use at least 12 characters",Toast.LENGTH_LONG).show(); return; }
            if (!p.equals(c)) { Toast.makeText(this,"Passwords do not match",Toast.LENGTH_LONG).show(); return; }
            try {
                String hash = PasswordKdf.hash(p.toCharArray());
                new SecureStore(this).put("password_hash", hash);
                getSharedPreferences("pocket_settings", MODE_PRIVATE).edit().putBoolean("registered", true).apply();
                SecurityEvents.record(this,"account_created");
                startActivity(new Intent(this, SuccessActivity.class)); finish();
            } catch (Exception e) { Toast.makeText(this,"Secure account creation failed",Toast.LENGTH_LONG).show(); }
        }));
        setContentView(l);
    }
}
