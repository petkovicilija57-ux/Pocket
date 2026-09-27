package co.pocket.companion;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.util.Base64;

final class CryptoBox {
    private static final String KS = "AndroidKeyStore";
    private static final String ALIAS = "Pocket.Local.AES.v1";
    private CryptoBox() {}

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance(KS); store.load(null);
        KeyStore.Entry e = store.getEntry(ALIAS, null);
        if (e instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry)e).getSecretKey();
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS);
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build());
        return kg.generateKey();
    }

    static String encrypt(String plaintext) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = c.getIV();
        byte[] out = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(iv, Base64.NO_WRAP) + "." + Base64.encodeToString(out, Base64.NO_WRAP);
    }

    static String decrypt(String packed) throws Exception {
        String[] p = packed.split("\\.", 2);
        if (p.length != 2) throw new IllegalArgumentException("Bad encrypted value");
        byte[] iv = Base64.decode(p[0], Base64.NO_WRAP);
        byte[] data = Base64.decode(p[1], Base64.NO_WRAP);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(c.doFinal(data), StandardCharsets.UTF_8);
    }
}
