package fm.mrc.expensetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.expensetracker.ExpenseTrackerApp
import fm.mrc.expensetracker.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    val allTransactions: Flow<List<Transaction>>
    val totalIncome: Flow<Double>
    val totalExpenses: Flow<Double>
    val totalBalance: Flow<Double>
    val incomeByCategory: Flow<List<CategorySum>>
    val expensesByCategory: Flow<List<CategorySum>>

    init {
        val dao = (application as ExpenseTrackerApp).database.transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
        totalIncome = repository.totalIncome.map { it ?: 0.0 }
        totalExpenses = repository.totalExpenses.map { it ?: 0.0 }
        
        totalBalance = combine(
            repository.totalIncome,
            repository.totalExpenses
        ) { income, expenses ->
            (income ?: 0.0) - (expenses ?: 0.0)
        }
        
        incomeByCategory = repository.incomeByCategory
        expensesByCategory = repository.expensesByCategory
    }

    fun addTransaction(
        description: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        date: LocalDateTime
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    description = description,
                    amount = amount,
                    isIncome = isIncome,
                    category = category,
                    date = date
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
} 