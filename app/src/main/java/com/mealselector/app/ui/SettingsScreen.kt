package com.mealselector.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mealselector.app.R
import com.mealselector.app.data.MealState
import com.mealselector.app.ui.theme.MutedText
import com.mealselector.app.ui.theme.OrangePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mealState: MealState,
    onAddMeal: (String) -> Unit,
    onRemoveMeal: (Int) -> Unit,
    onMoveMealUp: (Int) -> Unit,
    onMoveMealDown: (Int) -> Unit,
    onLockByDefaultChange: (Boolean) -> Unit,
    onLockOnRotateChange: (Boolean) -> Unit,
    onLeaveGroup: () -> Unit,
    onBack: () -> Unit
) {
    var newMealName by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            mealState.groupCode?.let { code ->
                Text(
                    text = stringResource(R.string.shared_group),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.group_code_share, code),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onLeaveGroup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.leave_group))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = stringResource(R.string.lock_settings_title),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            LockSettingRow(
                title = stringResource(R.string.lock_by_default_title),
                summary = stringResource(R.string.lock_by_default_summary),
                checked = mealState.lockByDefault,
                onCheckedChange = onLockByDefaultChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            LockSettingRow(
                title = stringResource(R.string.lock_on_rotate_title),
                summary = stringResource(R.string.lock_on_rotate_summary),
                checked = mealState.lockOnRotate,
                onCheckedChange = onLockOnRotateChange
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.add_meal),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newMealName,
                    onValueChange = { newMealName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.meal_name_hint)) },
                    singleLine = true
                )

                Button(
                    onClick = {
                        onAddMeal(newMealName)
                        newMealName = ""
                    },
                    enabled = newMealName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text(stringResource(R.string.add_meal))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.your_meals),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.reorder_meals_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(mealState.meals) { index, meal ->
                    val isCurrent = index == mealState.currentIndex
                    val isFirst = index == 0
                    val isLast = index == mealState.meals.lastIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. $meal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCurrent) OrangePrimary else MaterialTheme.colorScheme.onBackground
                            )
                            if (isCurrent) {
                                Text(
                                    text = stringResource(R.string.current_meal_marker),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MutedText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveMealUp(index) },
                                enabled = !isFirst
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.move_meal_up)
                                )
                            }
                            IconButton(
                                onClick = { onMoveMealDown(index) },
                                enabled = !isLast
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.move_meal_down)
                                )
                            }
                            IconButton(onClick = { onRemoveMeal(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove_meal),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockSettingRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
