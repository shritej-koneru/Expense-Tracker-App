package fm.mrc.expensetracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import fm.mrc.expensetracker.data.CategorySum
import fm.mrc.expensetracker.data.LocalDateTime
import fm.mrc.expensetracker.data.Transaction
import fm.mrc.expensetracker.data.ZoneOffset
import fm.mrc.expensetracker.ui.theme.ExpenseTrackerTheme
import fm.mrc.expensetracker.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted, you can show a toast or snackbar here
            Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            // Show a message that the app needs permissions
            Toast.makeText(
                this,
                "Storage permissions are required for saving data",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermissions()
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                HomeScreen()
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            }
        } else {
            // For Android 10 and below
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val viewModel: TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val totalBalance by viewModel.totalBalance.collectAsState(initial = 0.0)
    val totalIncome by viewModel.totalIncome.collectAsState(initial = 0.0)
    val totalExpenses by viewModel.totalExpenses.collectAsState(initial = 0.0)
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance Summary Section
                item {
                    BalanceSummaryCard(
                        totalBalance = totalBalance,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses
                    )
                }

                // Action Buttons Section
                item {
                    ActionButtonsRow(
                        onIncomeClick = { showIncomeDialog = true },
                        onExpenseClick = { showExpenseDialog = true },
                        onAnalyticsClick = { showAnalyticsDialog = true }
                    )
                }

                // Transaction History
                item {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No transactions yet",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(transactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(transaction) }
                        )
                    }
                }

                // Add project credit at the bottom
                item {
                    Text(
                        text = "Project by Design Thinking Group 1",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

        }
    }
    
    if (showIncomeDialog) {
        TransactionDialog(
            isIncome = true,
            onDismiss = { showIncomeDialog = false },
            onSubmit = { description, amount, date, category ->
                viewModel.addTransaction(
                    description = description,
                    amount = amount,
                    isIncome = true,
                    category = category,
                    date = date
                )
                showIncomeDialog = false
            }
        )
    }
    
    if (showExpenseDialog) {
        TransactionDialog(
            isIncome = false,
            onDismiss = { showExpenseDialog = false },
            onSubmit = { description, amount, date, category ->
                viewModel.addTransaction(
                    description = description,
                    amount = amount,
                    isIncome = false,
                    category = category,
                    date = date
                )
                showExpenseDialog = false
            }
        )
    }
    
    if (showAnalyticsDialog) {
        AnalyticsDialog(
            onDismiss = { showAnalyticsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, LocalDateTime, String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDateTime.now()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

    val categories = if (isIncome) {
        listOf("Salary", "Freelance", "Investment", "Other")
    } else {
        listOf("Food", "Transport", "Bills", "Shopping", "Other")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIncome) "Add Income" else "Add Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedDate.format(fm.mrc.expensetracker.data.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    onValueChange = {},
                    label = { Text("Date & Time") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { amountValue ->
                        onSubmit(description, amountValue, selectedDate, selectedCategory)
                    }
                },
                enabled = description.isNotBlank() && 
                         amount.isNotBlank() && 
                         selectedCategory.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = LocalDateTime.ofEpochSecond(
                            it / 1000,
                            0,
                            ZoneOffset.UTC
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AnalyticsDialog(
    onDismiss: () -> Unit
) {
    val viewModel: TransactionViewModel = viewModel()
    val incomeByCategory by viewModel.incomeByCategory.collectAsState(initial = emptyList())
    val expensesByCategory by viewModel.expensesByCategory.collectAsState(initial = emptyList())
    val totalIncome by viewModel.totalIncome.collectAsState(initial = 0.0)
    val totalExpenses by viewModel.totalExpenses.collectAsState(initial = 0.0)
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analytics") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    // Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Summary",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Total Income",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatCurrency(totalIncome),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF00C853)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Total Expenses",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatCurrency(totalExpenses),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                            
                            // Savings Rate
                            if (totalIncome > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val savingsRate = ((totalIncome - totalExpenses) / totalIncome * 100)
                                    .coerceIn(0.0, 100.0)
                                Text(
                                    "Savings Rate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        String.format("%.1f%%", savingsRate),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (savingsRate > 20) Color(0xFF00C853) else Color(0xFFD32F2F)
                                    )
                                    LinearProgressIndicator(
                                        progress = (savingsRate / 100).toFloat(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .height(8.dp),
                                        color = if (savingsRate > 20) Color(0xFF00C853) else Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }

                    // Income Categories Chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Income by Category",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CategoryChart(
                                categories = incomeByCategory,
                                total = totalIncome,
                                isIncome = true
                            )
                        }
                    }

                    // Expenses Categories Chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Expenses by Category",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CategoryChart(
                                categories = expensesByCategory,
                                total = totalExpenses,
                                isIncome = false
                            )
                        }
                    }

                    // Income Pie Chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Income Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PieChart(
                                data = incomeByCategory,
                                total = totalIncome,
                                isIncome = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    // Expense Pie Chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Expense Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PieChart(
                                data = expensesByCategory,
                                total = totalExpenses,
                                isIncome = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    // Transaction Timeline
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Transaction Timeline",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TransactionLineChart(
                                transactions = transactions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }

            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CategoryChart(
    categories: List<CategorySum>,
    total: Double,
    isIncome: Boolean
) {
    Column {
        categories.forEach { category ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(category.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isIncome) Color(0xFF00C853) else Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                        if (total > 0) {
                            Text(
                                text = String.format("%.1f%%", (category.total / total * 100)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                LinearProgressIndicator(
                    progress = calculateProgress(category.total, categories.maxByOrNull { it.total }?.total ?: 1.0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(top = 4.dp),
                    color = if (isIncome) Color(0xFF00C853) else Color(0xFFD32F2F),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        
        if (categories.isEmpty()) {
            Text(
                text = "No ${if (isIncome) "income" else "expenses"} recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateProgress(value: Double, max: Double): Float {
    return if (max > 0) (value / max).toFloat() else 0f
}

@Composable
fun BalanceSummaryCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatCurrency(totalBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalBalance >= 0) Color(0xFF00C853) else Color(0xFFD32F2F),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = formatCurrency(totalIncome),
                            color = Color(0xFF00C853),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = formatCurrency(totalExpenses),
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onIncomeClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00C853)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Income")
            Spacer(Modifier.width(4.dp))
            Text("Income")
        }
        
        Button(
            onClick = onExpenseClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
            Spacer(Modifier.width(4.dp))
            Text("Expense")
        }
        
        Button(
            onClick = onAnalyticsClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Analytics"
            )
            Spacer(Modifier.width(4.dp))
            Text("Analytics")
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (transaction.isIncome) 
                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.date.format(
                        fm.mrc.expensetracker.data.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (transaction.isIncome) Color(0xFF00C853) else Color(0xFFD32F2F)
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
}

@Composable
fun PieChart(
    data: List<CategorySum>,
    total: Double,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || total == 0.0) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No ${if (isIncome) "income" else "expenses"} data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val context = LocalContext.current
    val pieColors = if (isIncome) {
        listOf(
            android.graphics.Color.parseColor("#00C853"),
            android.graphics.Color.parseColor("#43A047"),
            android.graphics.Color.parseColor("#66BB6A"),
            android.graphics.Color.parseColor("#81C784"),
            android.graphics.Color.parseColor("#A5D6A7")
        )
    } else {
        listOf(
            android.graphics.Color.parseColor("#D32F2F"),
            android.graphics.Color.parseColor("#E53935"),
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#EF5350"),
            android.graphics.Color.parseColor("#E57373")
        )
    }

    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                legend.orientation = Legend.LegendOrientation.VERTICAL
                legend.setDrawInside(false)
            }
        },
        modifier = modifier,
        update = { chart ->
            val entries = data.map { category ->
                PieEntry(
                    category.total.toFloat(),
                    category.category
                )
            }

            val dataSet = PieDataSet(entries, "").apply {
                colors = pieColors
                valueTextSize = 12f
                valueFormatter = PercentFormatter(chart)
            }

            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun TransactionLineChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transaction data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val chartEntryModel = remember(transactions) {
        val sortedTransactions = transactions.sortedBy { it.date.toEpochSecond(fm.mrc.expensetracker.data.ZoneOffset.UTC) }
        val entries = sortedTransactions.mapIndexed { index, transaction ->
            val amount = if (transaction.isIncome) transaction.amount else -transaction.amount
            FloatEntry(index.toFloat(), amount.toFloat())
        }
        entryModelOf(entries)
    }

    Chart(
        chart = lineChart(),
        model = chartEntryModel,
        modifier = modifier
    )
}