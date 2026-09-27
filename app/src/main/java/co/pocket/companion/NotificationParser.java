package co.pocket.companion;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NotificationParser {
    private static final Pattern BAM = Pattern.compile("(?iu)(?:(BAM|KM)\\s*([0-9][0-9., ]*)|([0-9][0-9., ]*)\\s*(BAM|KM))\\b");
    private static final Pattern BAD = Pattern.compile("(?iu)\\b(otp|pin|cvv|cvc|password|lozinka|code|kod|declined|failed|odbijen|refund|povrat|storno|reversal|pending|na cekanju|podizanje|cash withdrawal)\\b");
    private static final Pattern PURCHASE = Pattern.compile("(?iu)\\b(paid|payment|purchase|spent|platili|placanje|placeno|kupovina|kupljeno|potroseno|pos)\\b");

    private NotificationParser() {}

    public static Parsed parse(String title, String text) {
        String all = ((title == null ? "" : title) + " " + (text == null ? "" : text)).trim();
        if (all.isEmpty() || BAD.matcher(all).find() || !PURCHASE.matcher(all).find()) return null;
        Matcher m = BAM.matcher(all); if (!m.find()) return null;
        String raw = m.group(2) != null ? m.group(2) : m.group(3);
        Long cents = parseCents(raw); if (cents == null || cents <= 0) return null;
        String merchant = title == null || title.isBlank() ? "Card purchase" : title.trim();
        return new Parsed(cents, "BAM", merchant);
    }

    static Long parseCents(String raw) {
        if (raw == null) return null;
        String s = raw.replace(" ", "").trim();
        int comma = s.lastIndexOf(','); int dot = s.lastIndexOf('.'); int sep = Math.max(comma, dot);
        try {
            if (sep >= 0 && s.length() - sep - 1 <= 2) {
                String whole = s.substring(0, sep).replace(",", "").replace(".", "");
                String frac = s.substring(sep + 1);
                if (frac.length() == 1) frac += "0";
                return Math.addExact(Math.multiplyExact(Long.parseLong(whole), 100L), Long.parseLong(frac));
            }
            return Math.multiplyExact(Long.parseLong(s.replace(",", "").replace(".", "")), 100L);
        } catch (NumberFormatException | ArithmeticException e) { return null; }
    }

    public record Parsed(long cents, String currency, String merchant) {}
}
