package com.fuku856.povomanager.ui.archived

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuku856.povomanager.domain.LineStatus
import com.fuku856.povomanager.ui.common.RemainingDaysBadge
import com.fuku856.povomanager.ui.common.SimTypeChip
import com.fuku856.povomanager.ui.common.SwipeToActionBox
import com.fuku856.povomanager.ui.common.displayName
import com.fuku856.povomanager.ui.common.formatPhoneNumber
import com.fuku856.povomanager.ui.common.toDisplayString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedLinesScreen(
    onBack: () -> Unit,
    onLineClick: (Long) -> Unit,
    viewModel: ArchivedLinesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("アーカイブ済み回線") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.loaded && uiState.statuses.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.statuses, key = { it.line.id }) { status ->
                    SwipeToActionBox(
                        onAction = { viewModel.unarchive(status.line.id) },
                        actionIcon = Icons.Default.Unarchive,
                        actionLabel = "解除",
                        scrollInProgress = { listState.isScrollInProgress },
                    ) {
                        ArchivedLineCard(
                            status = status,
                            onClick = { onLineClick(status.line.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedLineCard(
    status: LineStatus,
    onClick: () -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            status.line.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        status.line.simType?.let {
                            Spacer(Modifier.width(8.dp))
                            SimTypeChip(it)
                        }
                    }
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

            if (status.expiryDate != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "自動解約日: ${status.expiryDate.toDisplayString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Archive,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("アーカイブ済みの回線はありません", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "ホームで回線を左にスワイプすると\nアーカイブできます",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
