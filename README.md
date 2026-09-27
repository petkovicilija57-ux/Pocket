# Pocket 0.5.0

Android package: `co.pocket.companion`

This source keeps the Pocket payment-tracker concept and adds a real connection-health layer, PayPal OAuth handoff, secure card-provisioning entry point, and Android NFC-wallet readiness checks.

## New in 0.5.0

- app-entry health check for notification listener, Google Wallet, PayPal, and configured bank source
- last-seen time/count for notification sources
- real PayPal hosted-login architecture through a backend; Pocket never asks for the PayPal password or verification code
- `Successful` screen after Pocket account creation
- secure card-provisioning entry point that never saves PAN/CVC/PIN
- NFC capability screen and Android 15 Wallet-role integration
- HCE payment service scaffold disabled by default until a certified issuer/TSP provider is configured
- Android Keystore AES-GCM secure store
- PBKDF2 local password hashing
- backup/data-transfer protections and cleartext-network blocking
- purchase-only notification parser with OTP/PIN/refund/failed/pending exclusions and duplicate suppression

## Build configuration

Set these as Gradle project properties or environment variables:

- `POCKET_PAYPAL_BACKEND_BASE_URL=https://...`
- `POCKET_CARD_PROVISIONING_URL=https://...`
- `POCKET_PAYMENT_PROVIDER_ENABLED=false` (leave false until the certified payment module exists)

Then open the project in Android Studio and build the `app` module.

## Signing

To update an already installed Pocket build, sign the APK with the same existing Pocket signing identity. Do not commit the private keystore or its password to this project or a public repository.

## What is and is not live

Notification health and notification imports are native Android functionality and become live after the user grants notification access.

PayPal linking becomes live when the backend URL points to a deployed server configured with valid PayPal app credentials.

NFC contactless card payments cannot be honestly marked live until an approved issuer/TSP tokenization module is connected and certified. The source intentionally fails closed instead of pretending that a typed card number can be used as a contactless payment credential.
