package com.wiotracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wiotracker.R
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.repository.WifiScanRepository
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel = viewModel ?: remember {
        val database = AppDatabase.getDatabase(context)
        CalendarViewModel(WifiScanRepository(database.wifiScanDao()))
    }
    val uiState by actualViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Navigation
            MonthNavigation(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                onPreviousMonth = {
                    val calendar = Calendar.getInstance()
                    calendar.set(uiState.currentYear, uiState.currentMonth, 1)
                    calendar.add(Calendar.MONTH, -1)
                    actualViewModel.changeMonth(
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.YEAR)
                    )
                },
                onNextMonth = {
                    val calendar = Calendar.getInstance()
                    calendar.set(uiState.currentYear, uiState.currentMonth, 1)
                    calendar.add(Calendar.MONTH, 1)
                    actualViewModel.changeMonth(
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.YEAR)
                    )
                }
            )

            // Calendar Grid
            CalendarGrid(
                month = uiState.currentMonth,
                year = uiState.currentYear,
                dailyStats = uiState.dailyStats,
                viewModel = actualViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MonthNavigation(
    currentMonth: Int,
    currentYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Text("<")
        }
        Text(
            text = "${currentYear}年${currentMonth + 1}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Text(">")
        }
    }
}

@Composable
fun CalendarGrid(
    month: Int,
    year: Int,
    dailyStats: Map<String, Pair<Int, Int>>,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Adjust first day to match week start (Sunday = 1, Monday = 2, etc.)
    val startOffset = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7

    Column(modifier = modifier.padding(16.dp)) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        var dayIndex = 0
        while (dayIndex < startOffset + daysInMonth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { weekDay ->
                    if (dayIndex < startOffset) {
                        // Empty cell before first day
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else if (dayIndex < startOffset + daysInMonth) {
                        val day = dayIndex - startOffset + 1
                        val date = viewModel.formatDate(day, month, year)
                        val stats = dailyStats[date] ?: Pair(0, 0)
                        val periodicCount = stats.first
                        val totalCount = stats.second
                        val isSuccess = periodicCount >= 3

                        CalendarDayCell(
                            day = day,
                            periodicCount = periodicCount,
                            totalCount = totalCount,
                            isSuccess = isSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty cell after last day
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                    dayIndex++
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    periodicCount: Int,
    totalCount: Int,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isSuccess) Color(0xFF4CAF50) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (totalCount > 0) {
                val displayText = if (periodicCount > 0 && totalCount > periodicCount) {
                    "$periodicCount/$totalCount"
                } else if (periodicCount > 0) {
                    periodicCount.toString()
                } else {
                    totalCount.toString()
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSuccess) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
