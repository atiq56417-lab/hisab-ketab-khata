# Architecture

## Layers (MVVM)
```
UI (Jetpack Compose screens)  ->  MainViewModel (StateFlow)  ->  Repository  ->  Room DAOs  ->  SQLite
                                                   \-> SettingsStore (DataStore: selected shop, theme)
```
- `KhataApp` creates the single database, repository, settings store and backup manager (simple manual DI).
- Language uses `AppCompatDelegate.setApplicationLocales` (default `ur`). Compose follows the locale, so RTL is automatic.
- Navigation Compose: tabs Home / Customers / Settings, plus Shops and Customer detail screens.

## Database (Room, version 1, file `hisab_kitab.db`)
| Table | Purpose | Key columns |
|---|---|---|
| shops | One row per shop | id, name, ownerName, phone, address, openingBalance, notes, createdAt, archived |
| customers | Customer account inside ONE shop | id, shopId (FK RESTRICT), name, phone, address, photoUri, openingBalance, archived |
| ledger_transactions | Udhaar history | id, shopId (FK), customerId (FK), type (GIVE_CREDIT / RECEIVE_PAYMENT), amount (>0), timestamp, description, reference, voided, voidedAt |

Indexes: customers(shopId), customers(name), ledger_transactions(shopId), (customerId), (timestamp).
Foreign keys use RESTRICT, and nothing is ever physically deleted by the app (archive / void instead).
The same real-world person can have separate customer rows in different shops.

## Money and balances
- Money is a `Long` number of paisa. Input is parsed with `BigDecimal` and rounded HALF_UP to 2 decimals.
- `balance = customer.openingBalance + SUM(GIVE_CREDIT) - SUM(RECEIVE_PAYMENT)` over non-voided transactions.
  Positive: customer owes the shop. Negative: shop owes the customer.
- Balance is computed in SQL on every read, so it cannot drift from the history.
- Editing a customer never changes its opening balance. Cancelling an entry sets `voided = 1`; the row stays visible.

## Backup and restore
Backup copies the SQLite file (after a WAL checkpoint) to a user-chosen location. Restore validates the chosen file
(SQLite header, `PRAGMA integrity_check`, required tables, version <= app version), keeps a copy of current data as
`pre_restore_backup.db` in app storage, replaces the database and asks the user to reopen the app.
The backup is a raw `.db` file (not ZIP/JSON).

## Adding features later
Add an entity + DAO + repository methods, bump `AppDatabase` version and add a `Migration` (schemas are exported to `app/schemas`),
then add a ViewModel and screen and a route in `AppRoot.kt`.
