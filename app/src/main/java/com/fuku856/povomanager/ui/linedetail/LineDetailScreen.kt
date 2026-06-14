package com.fuku856.povomanager.ui.linedetail

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuku856.povomanager.data.db.ToppingPurchase
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.ui.common.PurchaseSheet
import com.fuku856.povomanager.ui.common.RemainingDaysBadge
import com.fuku856.povomanager.ui.common.displayName
import com.fuku856.povomanager.ui.common.formatPhoneNumber
import com.fuku856.povomanager.ui.common.toDisplayString
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineDetailScreen(
    lineId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: LineDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<ToppingPurchase?>(null) }

    // 回線が削除された場合は自動で戻る
    LaunchedEffect(uiState.loaded, uiState.status) {
        if (uiState.loaded && uiState.status == null) onBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PurchaseEvent.Added -> {
                    val result = snackbarHostState.showSnackbar("購入を記録しました", actionLabel = "取り消す")
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoPurchase(event.purchase)
                }
                is PurchaseEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar("履歴を削除しました", actionLabel = "元に戻す")
                    if (result == SnackbarResult.ActionPerformed) viewModel.restorePurchase(event.purchase)
                }
            }
        }
    }

    val status = uiState.status

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(status?.line?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "編集")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (status == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                if (!uiState.loaded) CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { StatusCard(status) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("購入を記録")
                    }
                    if (status.expiryDate != null) {
                        OutlinedButton(onClick = { insertCalendarEvent(context, status) }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("カレンダーに登録")
                        }
                    }
                }
            }
            item {
                Text("購入履歴", style = MaterialTheme.typography.titleMedium)
            }
            if (status.purchases.isEmpty()) {
                item {
                    Text(
                        "購入履歴がありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(status.purchases, key = { it.id }) { purchase ->
                PurchaseRow(
                    purchase = purchase,
                    onEdit = { editingPurchase = purchase },
                    onDelete = { viewModel.deletePurchase(purchase) },
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddSheet) {
        PurchaseSheet(
            title = "トッピング購入を記録",
            onConfirm = { date, name, validityEnd ->
                viewModel.recordPurchase(date, name, validityEnd)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }

    editingPurchase?.let { purchase ->
        PurchaseSheet(
            title = "購入履歴を編集",
            initialDate = purchase.purchaseDate,
            initialName = purchase.toppingName,
            initialValidityEnd = purchase.validityEndDate,
            onConfirm = { date, name, validityEnd ->
                viewModel.updatePurchase(purchase, date, name, validityEnd)
                editingPurchase = null
            },
            onDismiss = { editingPurchase = null },
        )
    }
}

@Composable
private fun StatusCard(status: LineStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        formatPhoneNumber(status.line.phoneNumber),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    status.line.memo?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                RemainingDaysBadge(status.daysRemaining)
            }
            Spacer(Modifier.height(12.dp))
            if (status.expiryDate != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("最終購入日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(status.lastPurchaseDate?.toDisplayString() ?: "-", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Text("自動解約日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(status.expiryDate.toDisplayString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Text(
                    "購入履歴を記録すると180日期限を計算します",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PurchaseRow(
    purchase: ToppingPurchase,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(purchase.toppingName, style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append("購入: ${purchase.purchaseDate.toDisplayString()}")
                    purchase.validityEndDate?.let { append("  有効期限: ${it.toDisplayString()}") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("編集") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(
                    text = { Text("削除", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

private fun insertCalendarEvent(context: android.content.Context, status: LineStatus) {
    val expiry = status.expiryDate ?: return
    val zone = ZoneId.systemDefault()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, expiry.atStartOfDay(zone).toInstant().toEpochMilli())
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, expiry.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        putExtra(CalendarContract.Events.TITLE, "povo期限: ${status.line.displayName}")
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            "povo回線(${formatPhoneNumber(status.line.phoneNumber)})の自動解約日です。トッピングを購入してください。",
        )
    }
    runCatching { context.startActivity(intent) }
}
