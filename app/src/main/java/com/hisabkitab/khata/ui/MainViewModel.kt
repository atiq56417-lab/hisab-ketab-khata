package com.hisabkitab.khata.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabkitab.khata.KhataApp
import com.hisabkitab.khata.data.Customer
import com.hisabkitab.khata.data.CustomerBalance
import com.hisabkitab.khata.data.LedgerTransaction
import com.hisabkitab.khata.data.Shop
import com.hisabkitab.khata.data.TxType
import com.hisabkitab.khata.data.TxWithName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class Dashboard(
    val todayCredit: Long = 0,
    val todayCollections: Long = 0,
    val receivables: Long = 0,
    val payables: Long = 0,
    val recent: List<TxWithName> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val khata = app as KhataApp
    private val repo = khata.repository
    private val settings = khata.settings

    /** null while the first load is in progress. */
    val shops: StateFlow<List<Shop>?> =
        repo.shops().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 0 = all shops. Falls back to 0 if the stored shop no longer exists. */
    val selectedShopId: StateFlow<Long> =
        combine(settings.selectedShopId, repo.shops()) { sid, list ->
            if (sid != 0L && list.none { it.id == sid }) 0L else sid
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val theme: StateFlow<String> = settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val query = MutableStateFlow("")

    val customers: StateFlow<List<CustomerBalance>> =
        combine(selectedShopId, query) { s, q -> s to q.trim() }
            .flatMapLatest { (s, q) -> repo.customers(s, q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboard: StateFlow<Dashboard> =
        selectedShopId.flatMapLatest { sid ->
            val (from, to) = todayRange()
            combine(
                repo.sumByType(TxType.GIVE_CREDIT, from, to, sid),
                repo.sumByType(TxType.RECEIVE_PAYMENT, from, to, sid),
                repo.customers(sid, ""),
                repo.recent(sid)
            ) { credit, collected, custs, recent ->
                Dashboard(
                    todayCredit = credit,
                    todayCollections = collected,
                    receivables = custs.filter { it.balance > 0 }.sumOf { it.balance },
                    payables = custs.filter { it.balance < 0 }.sumOf { -it.balance },
                    recent = recent
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Dashboard())

    fun selectShop(id: Long) {
        viewModelScope.launch { settings.setSelectedShop(id) }
    }

    fun addShop(shop: Shop) {
        viewModelScope.launch {
            val id = repo.addShop(shop)
            settings.setSelectedShop(id)
        }
    }

    fun updateShop(shop: Shop) {
        viewModelScope.launch { repo.updateShop(shop) }
    }

    fun archiveShop(id: Long) {
        viewModelScope.launch { repo.archiveShop(id) }
    }

    fun addCustomer(c: Customer) {
        viewModelScope.launch { repo.addCustomer(c) }
    }

    fun updateCustomer(id: Long, name: String, phone: String, address: String) {
        viewModelScope.launch { repo.updateCustomer(id, name, phone, address) }
    }

    fun archiveCustomer(id: Long) {
        viewModelScope.launch { repo.archiveCustomer(id) }
    }

    fun addTransaction(customer: CustomerBalance, type: TxType, amount: Long, description: String, reference: String) {
        viewModelScope.launch {
            repo.addTransaction(
                LedgerTransaction(
                    shopId = customer.shopId,
                    customerId = customer.id,
                    type = type,
                    amount = amount,
                    description = description.trim(),
                    reference = reference.trim()
                )
            )
        }
    }

    fun voidTransaction(id: Long) {
        viewModelScope.launch { repo.voidTransaction(id) }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { settings.setTheme(value) }
    }

    fun customer(id: Long): Flow<CustomerBalance?> = repo.customer(id)
    fun transactions(customerId: Long): Flow<List<LedgerTransaction>> = repo.transactions(customerId)

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to (start + 24L * 60 * 60 * 1000 - 1)
    }
}
