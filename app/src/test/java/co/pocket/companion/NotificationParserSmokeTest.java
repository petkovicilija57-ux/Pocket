package co.pocket.companion;

public final class NotificationParserSmokeTest {
    public static void main(String[] args) {
        if (NotificationParser.parse("Store", "Payment 12.50 KM") == null) throw new AssertionError("payment should parse");
        if (NotificationParser.parse("Bank", "OTP code 123456") != null) throw new AssertionError("OTP must not parse");
        if (!Long.valueOf(1250).equals(NotificationParser.parseCents("12,50"))) throw new AssertionError("cents parse failed");
        System.out.println("NotificationParser smoke tests passed");
        if (NotificationParser.parseCents("9223372036854775807") != null) throw new AssertionError("overflow must be rejected");
        if (NotificationParser.parse("Bank", "Payment declined 12.50 KM") != null) throw new AssertionError("declined payment must be rejected");
    }
}
