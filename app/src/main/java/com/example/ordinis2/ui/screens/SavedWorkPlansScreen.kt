package com.example.ordinis2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ordinis2.data.local.WorkPlanEntity
import com.example.ordinis2.viewmodel.WorkPlanViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.text.format


@Composable
fun SavedWorkPlansScreen(
    workPlanViewModel: WorkPlanViewModel,
    onNavigateToDisplayPlan: (WorkPlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allWorkPlans by workPlanViewModel.allWorkPlans.collectAsState(initial = emptyList())


        if (allWorkPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No saved work plans yet.",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allWorkPlans, key = { it.id }) { workPlanEntity ->
                    WorkPlanListItem(
                        workPlanEntity = workPlanEntity,
                        onClick = { onNavigateToDisplayPlan(workPlanEntity) }
                    )
                }
            }
        }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPlanListItem(
    workPlanEntity: WorkPlanEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workPlanEntity.projectTitle ?: "Untitled Plan",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type: ${workPlanEntity.workPlanType}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Generated: ${dateFormatter.format(workPlanEntity.generatedDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "View Plan Details",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
