package com.example.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale


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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = viewModel()) {
    val query by viewModel.query.observeAsState("")
    val data by viewModel.weather.observeAsState()
    val g by viewModel.geo.observeAsState()
    val error by viewModel.error.observeAsState()
    val cityDisplayMap by viewModel.cityDisplayMap.observeAsState(emptyMap())
    val cityWeatherMap by viewModel.cityWeatherMap.observeAsState(emptyMap())
    val pinnedCities by viewModel.pinnedCities.observeAsState(emptyList())

    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(pinnedCities, query) {
        if (query.isBlank() && pinnedCities.isNotEmpty()) {
            viewModel.preloadCitiesFromEntities(pinnedCities)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("현재 날씨", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.updateQuery(it) },
                        label = { Text("도시명 입력") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { viewModel.updateQuery("") }) {
                                    Text("지우기")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotBlank()) {
                                    viewModel.search()
                                    keyboard?.hide()
                                }
                            }
                        )
                    )
                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                viewModel.search()
                                keyboard?.hide()
                            }
                        }
                    ) { Text("검색") }
                }
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!error.isNullOrEmpty()) {
                    Text("에러: $error", color = MaterialTheme.colorScheme.error)
                }

                if (query.isBlank()) {
                    if (pinnedCities.isEmpty()) {
                        Text(
                            "관심 도시를 추가해주세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("관심 도시", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(pinnedCities, key = { _, city -> city.id }) { _, city ->
                                val name = city.name
                                val display = cityDisplayMap[name] ?: name
                                val w = cityWeatherMap[name]

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.togglePin(city) },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = display,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { viewModel.togglePin(city) }) {
                                                Icon(
                                                    imageVector = if (city.pinned) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = if (city.pinned) "고정 해제" else "고정",
                                                    tint = if (city.pinned) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        if (w != null) {
                                            Text(
                                                text = "${w.main.temp}°C • ${w.weather.firstOrNull()?.description.orEmpty()}",
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = "로딩 중…",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (data != null) {
                    val w = data!!
                    val cityKo = g?.local_names?.get("ko")
                    val cityDisplay = cityKo?.takeIf { it.isNotBlank() } ?: w.name
                    val countryKr = getCountryNameByLocale(g?.country)
                    val currentName = query.trim().ifBlank { w.name }
                    val isPinned = pinnedCities.any {
                        it.name.equals(currentName, ignoreCase = true) && it.pinned
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                viewModel.togglePinByName(currentName)
                                viewModel.updateQuery("")
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (countryKr.isNotEmpty()) "$cityDisplay, $countryKr" else cityDisplay,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { 
                                    viewModel.togglePinByName(currentName) 
                                    viewModel.updateQuery("")
                                }) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = if (isPinned) "고정 해제" else "고정",
                                        tint = if (isPinned) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${w.main.temp}°C • ${w.weather.firstOrNull()?.description.orEmpty()}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text("검색 중이거나 결과가 없습니다.")
                }
            }
        }
    )
}