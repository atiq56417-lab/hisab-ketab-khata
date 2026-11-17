package com.hisabkitab.khata.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {
    private val shopDao = db.shopDao()
    private val customerDao = db.customerDao()
    private val ledgerDao = db.ledgerDao()

    fun shops(): Flow<List<Shop>> = shopDao.observeAll()
    suspend fun addShop(shop: Shop): Long = shopDao.insert(shop)
    suspend fun updateShop(shop: Shop) = shopDao.update(shop)
    suspend fun archiveShop(id: Long) = shopDao.archive(id)

    fun customers(shopId: Long, query: String): Flow<List<CustomerBalance>> = customerDao.observe(shopId, query)
    fun customer(id: Long): Flow<CustomerBalance?> = customerDao.observeById(id)
    suspend fun addCustomer(c: Customer): Long = customerDao.insert(c)
    suspend fun updateCustomer(id: Long, name: String, phone: String, address: String) =
        customerDao.updateDetails(id, name, phone, address)
    suspend fun archiveCustomer(id: Long) = customerDao.archive(id)

    fun transactions(customerId: Long): Flow<List<LedgerTransaction>> = ledgerDao.observeForCustomer(customerId)

    suspend fun addTransaction(tx: LedgerTransaction): Long {
        require(tx.amount > 0) { "Amount must be positive" }
        return ledgerDao.insert(tx)
    }

    suspend fun voidTransaction(id: Long) = ledgerDao.markVoided(id, System.currentTimeMillis())
    fun sumByType(type: TxType, from: Long, to: Long, shopId: Long): Flow<Long> = ledgerDao.sumByType(type, from, to, shopId)
    fun recent(shopId: Long): Flow<List<TxWithName>> = ledgerDao.recent(shopId)
}
