package fm.mrc.expensetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isIncome = 1 ORDER BY date DESC")
    fun getAllIncome(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isIncome = 0 ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpenses(): Flow<Double?>

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE isIncome = 1 GROUP BY category")
    fun getIncomeByCategory(): Flow<List<CategorySum>>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE isIncome = 0 GROUP BY category")
    fun getExpensesByCategory(): Flow<List<CategorySum>>
}

data class CategorySum(
    val category: String,
    val total: Double
) 