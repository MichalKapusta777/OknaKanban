package com.example.oknakanban.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.oknakanban.data.OrderDao
import com.example.oknakanban.data.OrderEntity
import com.example.oknakanban.data.OrderStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrderViewModel(private val dao: OrderDao) : ViewModel() {
    val orders: StateFlow<List<OrderEntity>> = dao.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveOrder(
        existingId: Int?,
        clientName: String,
        address: String,
        realizationDate: String,
        description: String,
        status: OrderStatus
    ) {
        viewModelScope.launch {
            val order = OrderEntity(
                id = existingId ?: 0,
                clientName = clientName.trim(),
                address = address.trim(),
                realizationDate = realizationDate.trim(),
                description = description.trim(),
                status = status
            )
            if (existingId == null) dao.insert(order) else dao.update(order)
        }
    }

    fun updateStatus(order: OrderEntity, status: OrderStatus) {
        viewModelScope.launch { dao.update(order.copy(status = status)) }
    }

    fun delete(order: OrderEntity) {
        viewModelScope.launch { dao.delete(order) }
    }
}

class OrderViewModelFactory(private val dao: OrderDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
