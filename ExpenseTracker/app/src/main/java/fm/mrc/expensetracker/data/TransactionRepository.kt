package fm.mrc.expensetracker.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions = transactionDao.getAllTransactions()
    val totalIncome = transactionDao.getTotalIncome()
    val totalExpenses = transactionDao.getTotalExpenses()
    val incomeByCategory = transactionDao.getIncomeByCategory()
    val expensesByCategory = transactionDao.getExpensesByCategory()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
} 