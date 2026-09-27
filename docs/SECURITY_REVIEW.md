# Pocket 0.5.0 security review notes

Implemented in this source:

- `FLAG_SECURE` on the main screen to reduce screenshots/screen capture.
- Android backup and device-transfer disabled for private Pocket data.
- cleartext HTTP disabled; OAuth/backend URLs must use HTTPS in production.
- Android Keystore AES-GCM wrapper for app secrets.
- local password KDF uses PBKDF2-HMAC-SHA256 with a random salt and a high iteration count.
- notification import ignores OTP/PIN/CVC/password/refund/failed/pending messages and only accepts recognizable BAM/KM purchase patterns.
- two-minute duplicate suppression for identical source/amount/merchant notifications.
- PayPal credentials are never collected by Pocket; sign-in happens on PayPal and the client secret stays on the backend.
- the Android app stores only a Pocket backend session handle, not a PayPal access token.
- contactless payment service is disabled unless a certified payment provider is explicitly enabled.
- no raw card number, CVC, PIN, or magnetic-stripe data is stored by the supplied source.

Still required before claiming a production payment wallet:

- external mobile-app penetration test and threat model review
- Play Integrity/device integrity policy as appropriate
- certificate pinning strategy only if the backend team can operate safe pin rotation
- production secrets in a managed secret store / HSM or KMS
- audited server-side session persistence instead of the sample in-memory OAuth session map
- rate limiting, structured security logs, alerting, and incident-response procedures
- payment network/TSP certification and PCI assessment based on the final architecture
- real-device testing across supported Android/NFC chipsets

“No bugs” or “100% secure” cannot be guaranteed by a code review. The correct release claim is that identified controls are implemented and the remaining provider/certification tests are explicit.
