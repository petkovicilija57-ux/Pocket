# Go-live checklist

## PayPal

The supplied mobile flow is designed around PayPal-hosted OAuth/OpenID Connect. Configure a PayPal developer application, keep the client secret server-side, use an HTTPS backend callback, and set `POCKET_PAYPAL_BACKEND_BASE_URL` for the Android build. The sample server uses environment-configurable PayPal authorize/token endpoints so the deployed values can follow the current PayPal dashboard documentation.

Important: linking a PayPal identity is not the same as receiving a universal consumer transaction-history API. Pocket can continue to import PayPal purchase notifications when notification access is enabled. Provider rules and account eligibility are still enforced by PayPal.

## Google Wallet / Google Pay notifications

Pocket can verify that notification-listener permission is enabled, detect whether Google Wallet is installed, and show when a Google Wallet notification was last seen. It cannot force Google Wallet to emit a notification or treat a missing notification as proof that no purchase occurred.

## Bank notifications

Pocket learns a bank source only after the user selects/configures its package or after it has been observed. Notification parsing is a convenience import, not a direct bank connection.

## NFC contactless

Set `POCKET_PAYMENT_PROVIDER_ENABLED=true` only in a build that contains the approved issuer/TSP payment module. Configure the provider’s payment AIDs and provisioning backend. Do not replace the bridge with code that derives payment credentials from a typed PAN/CVC.
