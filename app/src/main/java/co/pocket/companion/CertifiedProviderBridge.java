package co.pocket.companion;

import android.content.Context;

final class CertifiedProviderBridge {
    private CertifiedProviderBridge() {}

    static byte[] processApdu(Context c, byte[] commandApdu) {
        // Intentionally no EMV/payment implementation here. Production builds must replace
        // this bridge with an issuer/TSP-approved SDK that supplies tokenized credentials,
        // transaction keys and network-required cryptograms. Never derive them from a raw PAN/CVC.
        return new byte[]{(byte)0x6F, (byte)0x00};
    }
}
