package fm.mrc.expensetracker.data

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import android.os.Build

class ExternalStorageHelper(private val context: Context) {
    private val gson = Gson()
    private val fileName = "expense_tracker_data.json"

    private fun getStorageDir(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.also {
            if (!it.exists()) {
                it.mkdirs()
            }
        } ?: context.filesDir
    }

    fun saveTransactions(transactions: List<Transaction>) {
        try {
            val file = File(getStorageDir(), fileName)
            FileWriter(file).use { writer ->
                gson.toJson(transactions, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val file = File(context.filesDir, fileName)
            FileWriter(file).use { writer ->
                gson.toJson(transactions, writer)
            }
        }
    }

    fun loadTransactions(): List<Transaction> {
        return try {
            val file = File(getStorageDir(), fileName)
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<List<Transaction>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    FileReader(file).use { reader ->
                        val type = object : TypeToken<List<Transaction>>() {}.type
                        gson.fromJson(reader, type) ?: emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
} 