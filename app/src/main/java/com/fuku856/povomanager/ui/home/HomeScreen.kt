package com.fuku856.povomanager.ui.home

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.ui.common.ArchiveGreen
import com.fuku856.povomanager.ui.common.ExpiryProgressBar
import com.fuku856.povomanager.ui.common.PurchaseSheet
import com.fuku856.povomanager.ui.common.RemainingDaysBadge
import com.fuku856.povomanager.ui.common.SwipeDismissSnackbarHost
import com.fuku856.povomanager.ui.common.SwipeToArchiveBox
import com.fuku856.povomanager.ui.common.displayName
import com.fuku856.povomanager.ui.common.formatPhoneNumber
import com.fuku856.povomanager.ui.common.showUndoSnackbar
import com.fuku856.povomanager.ui.common.toDisplayString
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLineClick: (Long) -> Unit,
    onAddLine: () -> Unit,
    onSettings: () -> Unit,
    onShowArchived: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var purchaseTargetLineId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.purchaseAdded.collect { purchase ->
            val result = snackbarHostState.showUndoSnackbar(
                message = "購入を記録しました",
                actionLabel = "取り消す",
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoPurchase(purchase)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.archivedEvent.collect { lineId ->
            val result = snackbarHostState.showUndoSnackbar(
                message = "アーカイブしました",
                actionLabel = "取り消す",
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.unarchive(lineId)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("povo Manager") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("回線を追加") },
            )
        },
        snackbarHost = { SwipeDismissSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.loaded && uiState.statuses.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                archivedCount = uiState.archivedCount,
                onShowArchived = onShowArchived,
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.statuses, key = { it.line.id }) { status ->
                    SwipeToArchiveBox(
                        onArchive = { viewModel.archiveLine(status.line.id) },
                        scrollInProgress = { listState.isScrollInProgress },
                    ) {
                        LineCard(
                            status = status,
                            expiryPeriodDays = uiState.expiryPeriodDays,
                            onClick = { onLineClick(status.line.id) },
                            onRecordPurchase = { purchaseTargetLineId = status.line.id },
                        )
                    }
                }
                if (uiState.archivedCount > 0) {
                    item(key = "archived-button") {
                        ShowArchivedButton(
                            count = uiState.archivedCount,
                            onClick = onShowArchived,
                        )
                    }
                }
            }
        }
    }

    purchaseTargetLineId?.let { lineId ->
        PurchaseSheet(
            title = "トッピング購入を記録",
            onConfirm = { date, name, validityEnd ->
                viewModel.recordPurchase(lineId, date, name, validityEnd)
                purchaseTargetLineId = null
            },
            onDismiss = { purchaseTargetLineId = null },
        )
    }
}

@Composable
private fun LineCard(
    status: LineStatus,
    expiryPeriodDays: Int,
    onClick: () -> Unit,
    onRecordPurchase: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.line.displayName, style = MaterialTheme.typography.titleMedium)
                    if (!status.line.name.isNullOrBlank()) {
                        Text(
                            formatPhoneNumber(status.line.phoneNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                RemainingDaysBadge(status.daysRemaining)
            }

            Spacer(Modifier.height(12.dp))

            if (status.expiryDate != null && status.daysRemaining != null) {
                ExpiryProgressBar(
                    daysRemaining = status.daysRemaining,
                    expiryPeriodDays = expiryPeriodDays,
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LabeledDate("最終購入", status.lastPurchaseDate)
                    LabeledDate("自動解約日", status.expiryDate)
                }
            } else {
                Text(
                    "購入履歴がありません。最初のトッピング購入を記録してください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            status.activeTopping?.let { topping ->
                val end = topping.validityEndDate ?: return@let
                val remaining = ChronoUnit.DAYS.between(LocalDate.now(), end)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${topping.toppingName}: ${end.toDisplayString()}まで(あと${remaining}日)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onRecordPurchase,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("購入を記録")
            }
        }
    }
}

@Composable
private fun ShowArchivedButton(count: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick) {
            Icon(Icons.Default.Archive, contentDescription = null, tint = ArchiveGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("アーカイブ($count)")
        }
    }
}

@Composable
private fun LabeledDate(label: String, date: LocalDate?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(date?.toDisplayString() ?: "-", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    archivedCount: Int = 0,
    onShowArchived: () -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.SimCard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("回線が登録されていません", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "「回線を追加」からpovo回線を登録して\n180日期限の管理を始めましょう",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // アクティブな回線が無くてもアーカイブ済みがあれば一覧への導線を残す
            if (archivedCount > 0) {
                Spacer(Modifier.height(16.dp))
                ShowArchivedButton(count = archivedCount, onClick = onShowArchived)
            }
        }
    }
}
