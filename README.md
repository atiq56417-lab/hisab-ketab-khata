# حساب کتاب کھاتہ — Hisab Kitab Khata

Offline Android app (Kotlin, Jetpack Compose, Material 3, Room, MVVM) for shopkeepers to manage
multiple shops, customers and udhaar (credit) accounts. Default language is Urdu (RTL), English is also available.

> **Status: Phase 1 of 3. This source was written without access to an Android SDK, so it has NOT been compiled
> or run by the author.** Expect that the first Gradle sync/build may show a few small compile errors that
> need fixing. See "What is and is not done" below.

## What is and is not done

### Implemented (Phase 1)
- Multi-shop: add, edit, remove (archive, data kept), switch shop, "All shops" combined view; data never mixed between shops.
- Customers per shop: add, edit, remove (archive, history kept), search by name or phone, running balance.
- Udhaar ledger: give credit, receive payment (partial or full), description and reference, previous and new balance on every row,
  status (customer owes / partially paid / settled / shop owes customer).
- Entries are never erased or edited: a wrong entry is cancelled (kept in history, struck through) and the balance is recomputed.
- Balances are computed from the history (never stored), and all money is a `Long` number of paisa, so there is no floating-point error.
- Dashboard: shop selector, total shops, credit given today, collected today, total receivable, owed to customers, recent transactions, quick actions.
- Customer statement as PDF (Urdu or English, mirrored columns for RTL) shared through the system share sheet (WhatsApp, email, Drive...).
- Full database backup to a file and restore from a file (validated with SQLite integrity check before anything is replaced;
  a safety copy of current data is kept). The file can be moved to another phone and restored there.
- Urdu / English switch, light / dark / system theme, About and version.
- Gradle set up for debug APK, release APK and AAB.

### NOT implemented yet (planned for Phase 2 and 3)
Suppliers, sales and purchases with invoices, products and inventory, expenses, daily cashbook, income and profit reports,
period reports (daily to yearly), CSV export, global search (only customer search exists), PIN lock and biometrics,
currency and date-format settings (PKR and fixed format only), customer photo, printing, date/time picker for entries
(entries use the current time), editing shop opening balance after creation, Room migrations (database is version 1).
Also not done: automated UI tests, and the RTL/PDF layouts have not been visually verified on a device.

## Requirements
- Android Studio Koala (2024.1) or newer, JDK 17 (bundled with Android Studio).
- Internet on first sync to download Gradle 8.7 and dependencies.

## Open and run
1. Extract the ZIP, then in Android Studio choose **File > Open** and select the `HisabKitabKhata` folder.
2. Let Gradle sync finish. (If `gradlew` is missing from the project, Android Studio uses the wrapper settings in
   `gradle/wrapper/gradle-wrapper.properties`. For the command line run `gradle wrapper --gradle-version 8.7` once.)
3. Press Run on a device or emulator (Android 8.0+).

## Build APK and AAB
```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/ (unsigned unless keystore.properties exists)
./gradlew bundleRelease        # app/build/outputs/bundle/release/app-release.aab
```
Signing for release: create a keystore (`keytool -genkey -v -keystore release.jks -alias khata -keyalg RSA -keysize 2048 -validity 10000`)
and a file `keystore.properties` in the project root (it is git-ignored):
```
storeFile=release.jks
storePassword=YOUR_PASSWORD
keyAlias=khata
keyPassword=YOUR_PASSWORD
```
Keep the keystore safe; you need the same one for every future update.

## Tests
`./gradlew test` runs `MoneyTest` (parsing and formatting of money). Ledger and multi-shop behaviour tests are not written yet.

## Docs
See `docs/ARCHITECTURE.md`.
