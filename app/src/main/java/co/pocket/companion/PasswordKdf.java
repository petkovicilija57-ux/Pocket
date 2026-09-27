package co.pocket.companion;

import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import android.util.Base64;

final class PasswordKdf {
    private static final int ITER = 210_000;
    private static final int BITS = 256;
    private PasswordKdf() {}

    static String hash(char[] password) throws Exception {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITER, BITS);
        byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword(); Arrays.fill(password, '\0');
        return ITER + "$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + Base64.encodeToString(h, Base64.NO_WRAP);
    }

    static boolean verify(char[] password, String packed) throws Exception {
        String[] p = packed.split("\\$", 3); if (p.length != 3) return false;
        int iter = Integer.parseInt(p[0]); byte[] salt = Base64.decode(p[1], Base64.NO_WRAP);
        byte[] expected = Base64.decode(p[2], Base64.NO_WRAP);
        PBEKeySpec spec = new PBEKeySpec(password, salt, iter, expected.length * 8);
        byte[] got = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword(); Arrays.fill(password, '\0');
        return constantTimeEquals(expected, got);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false; int x = 0;
        for (int i = 0; i < a.length; i++) x |= a[i] ^ b[i]; return x == 0;
    }
}
