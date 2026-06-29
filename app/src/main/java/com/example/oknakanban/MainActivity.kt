package com.example.oknakanban

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.oknakanban.data.OrderEntity
import com.example.oknakanban.data.OrderStatus
import com.example.oknakanban.viewmodel.OrderViewModel
import com.example.oknakanban.viewmodel.OrderViewModelFactory
import java.time.LocalDate

private val Purple = Color(0xFF6750A4)
private val DarkPurple = Color(0xFF3D256C)
private val SoftPurple = Color(0xFFF7F1FF)
private val ColumnBackground = Color(0xFFF3F5F8)
private val Danger = Color(0xFFB3261E)
private val Warning = Color(0xFF8A5A00)
private val Success = Color(0xFF146C43)
private val Blue = Color(0xFF1D5D9B)

class MainActivity : ComponentActivity() {
    private val viewModel: OrderViewModel by viewModels {
        OrderViewModelFactory((application as OknaKanbanApp).database.orderDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Purple,
                    secondary = DarkPurple,
                    background = SoftPurple,
                    surface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OrdersScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: OrderViewModel) {
    val orders by viewModel.orders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editedOrder by remember { mutableStateOf<OrderEntity?>(null) }
    var query by remember { mutableStateOf("") }

    val filteredOrders = remember(orders, query) {
        if (query.isBlank()) orders else orders.filter {
            it.clientName.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Zlecenia montażu okien", fontWeight = FontWeight.Bold)
                        Text(
                            "Tablica Kanban",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6F6478)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftPurple)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editedOrder = null; showDialog = true },
                text = { Text("Dodaj zlecenie") },
                icon = { Text("+") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFBFF), Color(0xFFF3ECFF), Color(0xFFF7F7FB))
                    )
                )
        ) {
            val configuration = LocalConfiguration.current
            val boardHeight = if (configuration.screenWidthDp > configuration.screenHeightDp) {
                // Tryb poziomy: ekran ma mało wysokości, więc całość można przewijać w dół.
                420.dp
            } else {
                // Tryb pionowy: tablica zajmuje większość ekranu.
                (configuration.screenHeightDp.dp * 0.68f)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderPanel(orders = orders, query = query, onQueryChange = { query = it })
                KanbanBoard(
                    orders = filteredOrders,
                    onEdit = { editedOrder = it; showDialog = true },
                    onDelete = viewModel::delete,
                    onStatusChange = viewModel::updateStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boardHeight)
                )
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showDialog) {
        OrderDialog(
            order = editedOrder,
            onDismiss = { showDialog = false },
            onSave = { id, client, address, date, description, status ->
                viewModel.saveOrder(id, client, address, date, description, status)
                showDialog = false
            }
        )
    }
}

@Composable
fun HeaderPanel(
    orders: List<OrderEntity>,
    query: String,
    onQueryChange: (String) -> Unit
) {
    val todayCount = orders.count { it.columnName() == "Na dziś" }
    val overdueCount = orders.count { it.columnName() == "Po terminie" }
    val doneCount = orders.count { it.columnName() == "Zakończone" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Wszystkie", orders.size.toString(), "łącznie", Purple, Modifier.weight(1f))
            SummaryCard("Na dziś", todayCount.toString(), "pilne", Blue, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Po terminie", overdueCount.toString(), "do reakcji", Danger, Modifier.weight(1f))
            SummaryCard("Zakończone", doneCount.toString(), "gotowe", Success, Modifier.weight(1f))
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Szukaj po kliencie, adresie lub opisie") },
            leadingIcon = { Text("⌕") },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF625B71))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A7280))
        }
    }
}

@Composable
fun KanbanBoard(
    orders: List<OrderEntity>,
    onEdit: (OrderEntity) -> Unit,
    onDelete: (OrderEntity) -> Unit,
    onStatusChange: (OrderEntity, OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = listOf("Nowe", "Zaplanowane", "Na dziś", "W trakcie", "Po terminie", "Zakończone")

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 96.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(columns) { column ->
            val columnOrders = orders.filter { order -> order.columnName() == column }
            KanbanColumn(
                title = column,
                orders = columnOrders,
                onEdit = onEdit,
                onDelete = onDelete,
                onStatusChange = onStatusChange
            )
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    orders: List<OrderEntity>,
    onEdit: (OrderEntity) -> Unit,
    onDelete: (OrderEntity) -> Unit,
    onStatusChange: (OrderEntity, OrderStatus) -> Unit
) {
    val accent = columnAccent(title)

    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ColumnBackground),
        border = BorderStroke(1.dp, Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D1B20))
                    Text("${orders.size} zleceń", style = MaterialTheme.typography.bodySmall, color = Color(0xFF625B71))
                }
                StatusPill(text = title, color = accent)
            }
            Spacer(Modifier.height(10.dp))
            if (orders.isEmpty()) {
                EmptyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(order, onEdit, onDelete, onStatusChange)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyColumn(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Brak zleceń", fontWeight = FontWeight.Bold, color = Color(0xFF625B71))
            Text("Dodaj albo przenieś kartę", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A7280))
        }
    }
}

@Composable
fun OrderCard(
    order: OrderEntity,
    onEdit: (OrderEntity) -> Unit,
    onDelete: (OrderEntity) -> Unit,
    onStatusChange: (OrderEntity, OrderStatus) -> Unit
) {
    val column = order.columnName()
    val accent = columnAccent(column)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accent)
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(order.clientName, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StatusPill(text = column, color = accent)
                }
                Text("📍 ${order.address}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("📅 Termin: ${order.realizationDate}", style = MaterialTheme.typography.bodyMedium, color = accent, fontWeight = FontWeight.SemiBold)
                if (order.description.isNotBlank()) {
                    Text(order.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B4453), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onEdit(order) }) { Text("Edytuj") }
                    TextButton(onClick = { onDelete(order) }) { Text("Usuń") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status != OrderStatus.IN_PROGRESS && order.status != OrderStatus.DONE) {
                        OutlinedButton(onClick = { onStatusChange(order, OrderStatus.IN_PROGRESS) }) { Text("W trakcie") }
                    }
                    if (order.status != OrderStatus.DONE) {
                        Button(onClick = { onStatusChange(order, OrderStatus.DONE) }) { Text("Zakończ") }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDialog(
    order: OrderEntity?,
    onDismiss: () -> Unit,
    onSave: (Int?, String, String, String, String, OrderStatus) -> Unit
) {
    var client by remember { mutableStateOf(order?.clientName ?: "") }
    var address by remember { mutableStateOf(order?.address ?: "") }
    var date by remember { mutableStateOf(order?.realizationDate ?: LocalDate.now().toString()) }
    var description by remember { mutableStateOf(order?.description ?: "") }
    var status by remember { mutableStateOf(order?.status ?: OrderStatus.NEW) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (order == null) "Dodaj zlecenie" else "Edytuj zlecenie", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Klient") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adres") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data YYYY-MM-DD") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis prac") }, shape = RoundedCornerShape(14.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = status.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        OrderStatus.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = { status = item; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = client.isNotBlank() && address.isNotBlank() && date.isNotBlank(),
                onClick = { onSave(order?.id, client, address, date, description, status) }
            ) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

fun columnAccent(column: String): Color = when (column) {
    "Nowe" -> Purple
    "Zaplanowane" -> Blue
    "Na dziś" -> Warning
    "W trakcie" -> Color(0xFF006A6A)
    "Po terminie" -> Danger
    "Zakończone" -> Success
    else -> Purple
}

fun OrderEntity.columnName(): String {
    // Statusy ustawiane ręcznie mają pierwszeństwo.
    // Dzięki temu użytkownik może sam przenieść zlecenie do konkretnej kolumny.
    when (status) {
        OrderStatus.TODAY -> return "Na dziś"
        OrderStatus.IN_PROGRESS -> return "W trakcie"
        OrderStatus.OVERDUE -> return "Po terminie"
        OrderStatus.DONE -> return "Zakończone"
        else -> Unit
    }

    // Dla zleceń nowych i zaplanowanych kolumna może wynikać automatycznie z daty.
    // Wymagany format daty: RRRR-MM-DD, np. 2026-06-05.
    val today = LocalDate.now()
    val parsedDate = runCatching { LocalDate.parse(realizationDate) }.getOrNull()

    return when {
        parsedDate == null -> if (status == OrderStatus.NEW) "Nowe" else "Zaplanowane"
        parsedDate.isBefore(today) -> "Po terminie"
        parsedDate.isEqual(today) -> "Na dziś"
        status == OrderStatus.NEW -> "Nowe"
        else -> "Zaplanowane"
    }
}
