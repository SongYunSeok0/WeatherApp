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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.ui.theme.viewmodelTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            viewmodelTheme { WeatherScreen() }
        }
    }
} // end MainActivity

/* ------------ ViewModel (LiveData 바인딩) ------------ */
class WeatherViewModel : ViewModel() {
    private val repo = WeatherRepository()

    private val _query = MutableLiveData("")
    val query: LiveData<String> = _query

    private val _weather = MutableLiveData<WeatherDTO?>(null)
    val weather: LiveData<WeatherDTO?> = _weather
    private val _geo = MutableLiveData<GeoResult?>(null)
    val geo: LiveData<GeoResult?> = _geo

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _cityDisplayMap = MutableLiveData<Map<String, String>>(emptyMap())
    val cityDisplayMap: LiveData<Map<String, String>> = _cityDisplayMap
    private val _cityWeatherMap = MutableLiveData<Map<String, WeatherDTO>>(emptyMap())
    val cityWeatherMap: LiveData<Map<String, WeatherDTO>> = _cityWeatherMap

    fun updateQuery(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _weather.value = null
            _geo.value = null
            _error.value = null
        } else {
            fetchWeather(q)
        }
    }

    private fun fetchWeather(city: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val (w, g) = repo.getCurrentWithGeo(city)
                _weather.value = w
                _geo.value = g
            } catch (e: Exception) {
                _weather.value = null
                _geo.value = null
                _error.value = e.message ?: "요청 실패"
            }
        }
    }

    /** 리스트에서 보이는 도시들만 지오+날씨 미리 불러와 캐시 */
    fun preloadCities(cities: List<String>) {
        if (cities.isEmpty()) return
        viewModelScope.launch {
            val disp = _cityDisplayMap.value?.toMutableMap() ?: mutableMapOf()
            val wmap = _cityWeatherMap.value?.toMutableMap() ?: mutableMapOf()

            for (c in cities) {
                val needDisplay = disp[c] == null
                val needWeather = wmap[c] == null
                if (!needDisplay && !needWeather) continue

                try {
                    val (w, g) = repo.getCurrentWithGeo(c)
                    if (needDisplay) {
                        val cityKo = g?.local_names?.get("ko") ?: g?.name ?: w.name
                        val countryKr = getCountryNameByLocale(g?.country)
                        disp[c] = if (!countryKr.isNullOrEmpty()) "$cityKo, $countryKr" else cityKo
                    }
                    if (needWeather) {
                        wmap[c] = w
                    }
                } catch (_: Exception) {
                    if (needDisplay) disp[c] = c
                }
            }
            _cityDisplayMap.value = disp
            _cityWeatherMap.value = wmap
        }
    }
} // end WeatherViewModel

/* ------------ ISO 국가코드 → 한국어 국가명 ------------ */
fun getCountryNameByLocale(code: String?): String {
    if (code.isNullOrEmpty()) return ""
    val locale = Locale.Builder().setRegion(code).build()
    return locale.getDisplayCountry(Locale.KOREAN)
} // end getCountryNameByLocale

/* ------------ 화면 ------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = viewModel()) {
    val query by viewModel.query.observeAsState("")
    val data by viewModel.weather.observeAsState()
    val g by viewModel.geo.observeAsState()
    val error by viewModel.error.observeAsState()
    val cityDisplayMap by viewModel.cityDisplayMap.observeAsState(emptyMap())
    val cityWeatherMap by viewModel.cityWeatherMap.observeAsState(emptyMap())

    // 메인 화면 도시 리스트(예시)
    val cityList = remember {
        listOf(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "제주",
            "도쿄", "오사카", "삿포로", "후쿠오카",
            "Beijing", "Shanghai", "Hong Kong",
            "New York", "Los Angeles", "Chicago", "San Francisco", "Seattle",
            "London", "Paris", "Berlin", "Rome", "Madrid",
            "Bangkok", "Singapore", "Sydney", "Taipei"
        ).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }

    // 페이지네이션(10개씩)
    var page by remember { mutableStateOf(1) }
    val pageSize = 10
    val end = (page * pageSize).coerceAtMost(cityList.size)
    val visibleRaw = remember(cityList, page) { cityList.subList(0, end) }
    val hasMore = end < cityList.size

    // 보이는 도시에 대해서만 미리 로드
    LaunchedEffect(visibleRaw, query) {
        if (query.isBlank()) viewModel.preloadCities(visibleRaw)
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
                                TextButton(onClick = {
                                    viewModel.updateQuery("")
                                    page = 1
                                }) { Text("지우기") }
                            }
                        }
                    )
                    Button(
                        onClick = {
                            if (query.isNotBlank()) viewModel.updateQuery(query.trim())
                        }
                    ) { Text("검색") }
                }
            }
        },
        content = { innerPadding: PaddingValues ->
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
                    Text("도시 리스트", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(visibleRaw, key = { _, raw -> raw }) { _, raw ->
                            val display = cityDisplayMap[raw] ?: raw
                            val w = cityWeatherMap[raw]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateQuery(raw) },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        text = display,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
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
                        item("footer") {
                            Spacer(Modifier.height(4.dp))
                            if (hasMore) {
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { page += 1 }
                                ) { Text("더보기 (+$pageSize)") }
                            } else {
                                Text(
                                    text = "모든 도시를 다 봤습니다 (${cityList.size}개)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (data != null) {
                    val w = data!!
                    val cityKo = g?.local_names?.get("ko")
                    val cityDisplay = cityKo?.takeIf { it.isNotBlank() } ?: w.name
                    val countryKr = getCountryNameByLocale(g?.country)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = if (countryKr.isNotEmpty()) "$cityDisplay, $countryKr" else cityDisplay,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
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
            } // Column 닫음
        } // content 람다 닫음
    ) // Scaffold 호출 닫음
} // WeatherScreen 닫음
