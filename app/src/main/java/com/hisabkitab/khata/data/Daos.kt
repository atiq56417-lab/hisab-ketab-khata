package com.hisabkitab.khata.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val CUSTOMER_SELECT = """
SELECT c.id AS id, c.shopId AS shopId, s.name AS shopName, c.name AS name, c.phone AS phone,
       c.address AS address, c.openingBalance AS openingBalance,
       (c.openingBalance + COALESCE((
            SELECT SUM(CASE WHEN t.type = 'GIVE_CREDIT' THEN t.amount ELSE -t.amount END)
            FROM ledger_transactions t
            WHERE t.customerId = c.id AND t.voided = 0), 0)) AS balance
FROM customers c
JOIN shops s ON s.id = c.shopId
"""

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Shop>>

    @Insert
    suspend fun insert(shop: Shop): Long

    @Update
    suspend fun update(shop: Shop)

    @Query("UPDATE shops SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface CustomerDao {
    @Query(
        CUSTOMER_SELECT +
            " WHERE c.archived = 0 AND (:shopId = 0 OR c.shopId = :shopId)" +
            " AND (c.name LIKE '%' || :q || '%' OR c.phone LIKE '%' || :q || '%')" +
            " ORDER BY c.name COLLATE NOCASE ASC"
    )
    fun observe(shopId: Long, q: String): Flow<List<CustomerBalance>>

    @Query(CUSTOMER_SELECT + " WHERE c.id = :id")
    fun observeById(id: Long): Flow<CustomerBalance?>

    @Insert
    suspend fun insert(customer: Customer): Long

    /** The opening balance is deliberately not editable after creation. */
    @Query("UPDATE customers SET name = :name, phone = :phone, address = :address WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, phone: String, address: String)

    @Query("UPDATE customers SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_transactions WHERE customerId = :customerId ORDER BY timestamp ASC, id ASC")
    fun observeForCustomer(customerId: Long): Flow<List<LedgerTransaction>>

    @Insert
    suspend fun insert(tx: LedgerTransaction): Long

    @Query("UPDATE ledger_transactions SET voided = 1, voidedAt = :now WHERE id = :id AND voided = 0")
    suspend fun markVoided(id: Long, now: Long)

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM ledger_transactions " +
            "WHERE voided = 0 AND type = :type AND timestamp BETWEEN :from AND :to " +
            "AND (:shopId = 0 OR shopId = :shopId)"
    )
    fun sumByType(type: TxType, from: Long, to: Long, shopId: Long): Flow<Long>

    @Query(
        "SELECT t.id AS id, c.name AS customerName, t.type AS type, t.amount AS amount, " +
            "t.timestamp AS timestamp, t.description AS description " +
            "FROM ledger_transactions t JOIN customers c ON c.id = t.customerId " +
            "WHERE t.voided = 0 AND (:shopId = 0 OR t.shopId = :shopId) " +
            "ORDER BY t.timestamp DESC, t.id DESC LIMIT 10"
    )
    fun recent(shopId: Long): Flow<List<TxWithName>>
}
