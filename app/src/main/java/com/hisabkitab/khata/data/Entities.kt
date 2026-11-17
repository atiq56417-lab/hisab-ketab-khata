package com.hisabkitab.khata.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    /** Opening balance in paisa. */
    val openingBalance: Long = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Shops are never physically deleted; "removing" a shop archives it. */
    val archived: Boolean = false
)

@Entity(
    tableName = "customers",
    foreignKeys = [ForeignKey(
        entity = Shop::class,
        parentColumns = ["id"],
        childColumns = ["shopId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("shopId"), Index("name")]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val photoUri: String? = null,
    /** Positive = customer owes the shop, negative = shop owes the customer. In paisa. */
    val openingBalance: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)

enum class TxType {
    /** Udhaar: shop gives goods/money on credit. Balance goes UP (customer owes more). */
    GIVE_CREDIT,

    /** Customer pays. Balance goes DOWN. */
    RECEIVE_PAYMENT
}

@Entity(
    tableName = "ledger_transactions",
    foreignKeys = [
        ForeignKey(entity = Shop::class, parentColumns = ["id"], childColumns = ["shopId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("shopId"), Index("customerId"), Index("timestamp")]
)
data class LedgerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val customerId: Long,
    val type: TxType,
    /** Always positive, in paisa. The direction comes from [type]. */
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = "",
    val reference: String = "",
    /** Entries are never erased; cancelling marks them voided and they are ignored in balances. */
    val voided: Boolean = false,
    val voidedAt: Long? = null
)

/** Read model: a customer together with the computed running balance. */
data class CustomerBalance(
    val id: Long,
    val shopId: Long,
    val shopName: String,
    val name: String,
    val phone: String,
    val address: String,
    val openingBalance: Long,
    val balance: Long
)

/** Read model for the recent-transactions list. */
data class TxWithName(
    val id: Long,
    val customerName: String,
    val type: TxType,
    val amount: Long,
    val timestamp: Long,
    val description: String
)
