package co.pocket.companion;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

final class Ui {
    private Ui() {}

    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c,20), dp(c,18), dp(c,20), dp(c,24));
        return l;
    }

    static TextView title(Context c, String text) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(28); v.setTypeface(Typeface.DEFAULT_BOLD); return v;
    }
    static TextView h2(Context c, String text) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(19); v.setTypeface(Typeface.DEFAULT_BOLD); return v;
    }
    static TextView body(Context c, String text) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(15); v.setLineSpacing(0f,1.2f); return v;
    }
    static Button button(Context c, String text, View.OnClickListener click) {
        Button b = new Button(c); b.setText(text); b.setAllCaps(false); b.setOnClickListener(click); return b;
    }
    static Space gap(Context c, int dp) {
        Space s = new Space(c); s.setLayoutParams(new ViewGroup.LayoutParams(1, Ui.dp(c, dp))); return s;
    }
}
