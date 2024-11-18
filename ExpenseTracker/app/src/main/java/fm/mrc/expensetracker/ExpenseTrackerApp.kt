package fm.mrc.expensetracker

import android.app.Application
import fm.mrc.expensetracker.data.TransactionDatabase

class ExpenseTrackerApp : Application() {
    val database: TransactionDatabase by lazy { TransactionDatabase.getDatabase(this) }
} 