# How real NFC contactless payment works in Pocket

Pocket must **not** turn a raw card number into an emulated payment card. A production wallet needs an issuer/card-network/token-service-provider (TSP) integration.

High-level flow:

1. **Card provisioning** – the user starts “Add card”. Pocket sends the request to the approved issuer/TSP flow. The issuer verifies the cardholder and creates a device/network token. Pocket never stores the CVC/PIN and should avoid storing the original PAN.
2. **Wallet routing** – on Android 15+, the user can choose a wallet app for the system Wallet role. Payment-category NFC taps are routed to that wallet. Older Android releases use the contactless payment setting.
3. **Tap** – the terminal starts an NFC ISO-DEP exchange. Android routes the payment AID to the configured payment service.
4. **Tokenized response** – an approved provider SDK/service answers using tokenized credentials and transaction-specific cryptographic material. The default Pocket source deliberately does not implement or fake this layer.
5. **Authorization** – the terminal sends the transaction through the acquirer/card network. The token is resolved by the network/TSP and the issuer decides whether to approve it.
6. **Receipt / tracking** – Pocket can record its own confirmed payment event and may also import a bank/Google Wallet/PayPal notification. The bank statement remains the authoritative record.

## Why the default build keeps the payment service disabled

`TokenizedPaymentService` is controlled by `POCKET_PAYMENT_PROVIDER_ENABLED=false`. Enabling it without a certified provider would create a UI that looks like a wallet but cannot perform a real card-network transaction. The placeholder HCE AID is for development only and must be replaced by provider/network configuration.

## Required production inputs

- approved issuer/card-program or TSP relationship
- provider SDK / payment AIDs and token lifecycle APIs
- risk/device authentication requirements
- PCI/compliance scope and security review
- test credentials and certification test cases
- production backend and key-management setup
