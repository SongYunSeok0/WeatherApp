package com.example.weather

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.weather.data.AppDatabase
import com.example.weather.data.City
import com.example.weather.data.CityRepository
import com.example.weather.data.CitySeedProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val cityDao = AppDatabase.get(app).cityDao()
    private val cityRepo = CityRepository(cityDao)

    val pinnedCities: LiveData<List<City>> = cityRepo.observePinned().asLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) { ensureSeeded() }
    }

    private suspend fun ensureSeeded() {
        try {
            if (cityDao.count() > 0) return
            val provider = CitySeedProvider()
            if (provider.isEnabled()) {
                val seed = provider.fetch()
                val list = seed.cities
                    .map { City(name = it.name.trim(), pinned = it.pinned ?: false) }
                    .distinctBy { it.name.lowercase() }
                cityDao.insertAll(list)
                Log.d("SEED", "Seeded ${list.size} cities from URL")
            } else {
                Log.w("SEED", "CITY_SEED_URL is empty; skip seeding")
            }
        } catch (e: Exception) {
            Log.e("SEED", "Seeding failed", e)
        }
    }

    fun togglePin(city: City) = viewModelScope.launch(Dispatchers.IO) {
        cityRepo.togglePin(city)
    }

    fun togglePinByName(name: String) = viewModelScope.launch(Dispatchers.IO) {
        cityRepo.togglePinByName(name)
    }

    // ✅ 싱글턴 레포지토리 사용
    private val repo = WeatherRepository

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
        }
    }

    private var searchJob: Job? = null

    fun search() {
        val q = _query.value?.trim().orEmpty()
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _error.value = null
            try {
                val (w, g) = repo.getCurrentWithGeo(q)
                _weather.value = w
                _geo.value = g
                _error.value = null
            } catch (e: Exception) {
                if (_weather.value == null && _geo.value == null) {
                    _error.value = e.message ?: "요청 실패"
                } else {
                    _error.value = null
                }
            }
        }
    }

    fun preloadCitiesFromEntities(cities: List<City>) {
        preloadCities(cities.map { it.name })
    }

    // 병렬 프리로드 + 동시 4개 제한
    private fun preloadCities(cities: List<String>) {
        if (cities.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val disp = _cityDisplayMap.value?.toMutableMap() ?: mutableMapOf()
            val wmap = _cityWeatherMap.value?.toMutableMap() ?: mutableMapOf()

            val targets = cities.filter { disp[it] == null || wmap[it] == null }
            if (targets.isEmpty()) return@launch

            val sem = Semaphore(4)
            coroutineScope {
                targets.map { c ->
                    async {
                        sem.withPermit {
                            try {
                                val (w, g) = repo.getCurrentWithGeo(c)
                                val cityKo = g?.local_names?.get("ko") ?: g?.name ?: w.name
                                val countryKr = getCountryNameByLocale(g?.country)
                                synchronized(disp) {
                                    disp[c] = if (!countryKr.isNullOrEmpty()) "$cityKo, $countryKr" else cityKo
                                }
                                synchronized(wmap) { wmap[c] = w }
                            } catch (_: Exception) {
                                synchronized(disp) { if (disp[c] == null) disp[c] = c }
                            }
                        }
                    }
                }.awaitAll()
            }
            _cityDisplayMap.postValue(disp)
            _cityWeatherMap.postValue(wmap)
        }
    }
}
