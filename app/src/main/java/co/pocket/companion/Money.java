package co.pocket.companion;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    static long parse(String raw) {
        String value = raw.trim().replace(',', '.');
        if (!value.matches("[0-9]{1,9}(\\.[0-9]{1,2})?")) throw new IllegalArgumentException("Unesi iznos, npr. 12,50");
        return new BigDecimal(value).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }
    static String text(long cents) { return BigDecimal.valueOf(cents, 2).toPlainString() + " KM"; }
    static String input(long cents) { return BigDecimal.valueOf(cents, 2).toPlainString(); }
}
