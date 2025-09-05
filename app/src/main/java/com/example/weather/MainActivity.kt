package com.example.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

// Compose Runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList

// Compose UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

// Compose Foundation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

// Compose Material3
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star

// Reorderable
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

// Project
import com.example.weather.data.City
import com.example.weather.ui.theme.viewmodelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            viewmodelTheme { WeatherScreen() }
        }
    }
}

fun getCountryNameByLocale(code: String?): String {
    if (code.isNullOrEmpty()) return ""
    val locale = Locale.Builder().setRegion(code).build()
    return locale.getDisplayCountry(Locale.KOREAN)
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(if (to < size) to else size, item)
}

@Composable
fun PinnedReorderableList(
    pinned: List<City>,
    onCommitOrder: (List<City>) -> Unit
) {
    val local = remember(pinned) { pinned.toMutableStateList() }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to -> local.move(from.index, to.index) },
        onDragEnd = { _, _ -> onCommitOrder(local.toList()) }
    )

    LazyColumn(
        state = reorderState.listState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderState)
            .detectReorderAfterLongPress(reorderState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ){
        items(local, key = { it.id }) { city ->
            ReorderableItem(reorderState, key = city.id) { isDragging -> 
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDragging) 8.dp else 4.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "드래그",
                            modifier = Modifier.detectReorder(reorderState)
                        )
                    }
                }
            }
        }
    }
}