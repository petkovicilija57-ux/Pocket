# Pocket 0.6.0

Local Android expense tracker. Package: `co.pocket.tracker`. Android 8 or newer.

## Implemented

- Local registration with a success screen; password verification on entry and after backgrounding.
- Add, edit and delete BAM expenses; date and category selection; monthly search/filter.
- Monthly budget, remaining amount and remaining daily allowance.
- Savings goals with editable targets and saved amounts.
- Monthly subscriptions with due dates and a confirmed action to record an expense and advance the due date. This does not transfer money.
- Monthly totals by category and CSV export through Android's document picker.
- Bank app selection, notification permission status and last observed notifications. Import recognizes a limited set of BAM purchase notifications; users must review entries.

## Verification

GitHub Actions builds and lints the app, validates its APK signature, installs it on an Android 35 emulator, exercises registration/unlock/expense/background locking, and tests database persistence and CRUD operations. Tests do not certify all devices or every bank notification format.

## Security and limits

Passwords use PBKDF2; their hashes are wrapped with an Android Keystore key. Screenshots and Android backups are disabled. Expenses are stored in app-private SQLite, not an independently encrypted database. There is no cloud account, recovery service or guarantee against rooted-device access. No bank passwords or card details should be entered.

CI currently signs with a development key generated on each runner. This is not a production signing setup: future APKs may not update an existing installation. A private persistent signing identity and a securely configured build secret are still required for a stable production release. Never publish a signing private key. This package installs alongside previous Pocket packages and does not migrate their data.

Live PayPal login, card provisioning and NFC payments are disabled. This is not a payment wallet. Receipt attachments, scheduled system reminders, data import/restore, and bank-provider integrations are not implemented. CSV export is a report, not a complete backup.

## Build

Use JDK 17, Gradle 8.9 and Android SDK 35. Run `gradle assembleDebug assembleDebugAndroidTest lintDebug`. The CI installation script also requires the Android emulator, command-line tools, KVM and Python 3.
