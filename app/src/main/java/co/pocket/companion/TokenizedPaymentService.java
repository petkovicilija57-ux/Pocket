package co.pocket.companion;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

public final class TokenizedPaymentService extends HostApduService {
    @Override public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (!BuildConfig.PAYMENT_PROVIDER_ENABLED) return new byte[]{(byte)0x69,(byte)0x85};
        return CertifiedProviderBridge.processApdu(this, commandApdu);
    }
    @Override public void onDeactivated(int reason) { SecurityEvents.record(this,"nfc_deactivated:"+reason); }
}
