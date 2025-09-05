package com.example.weather.data

import com.example.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CityRepository(private val dao: CityDao) {
    fun observeAll(): Flow<List<City>> = dao.observeAll()
    fun observePinned(): Flow<List<City>> = dao.observePinned()

    suspend fun insertAll(cities: List<City>) = dao.insertAll(cities)
    suspend fun togglePin(city: City) = dao.setPinned(city.id, !city.pinned)
    suspend fun updateOrder(id: Long, order: Int) = dao.updateOrder(id, order)
    suspend fun count(): Long = dao.count()

    /**
     * 입력으로 온 이름(한/영)을 기준으로 핀 토글.
     * - DB에 있으면 핀만 토글
     * - 없으면 OWM 조회 → 표준명(영문)으로 저장 + pinned=true
     * - 중복/충돌/네트워크 실패 케이스까지 방어
     */
    suspend fun togglePinByName(input: String) = withContext(Dispatchers.IO) {
        val key = input.trim()
        if (key.isEmpty()) return@withContext

        // 1) 한글/영문 어느 쪽이든 매치되면 토글
        dao.findByAnyName(key)?.let { existing ->
            dao.setPinned(existing.id, !existing.pinned)
            return@withContext
        }

        // 2) 외부 조회 (실패하면 조용히 종료)
        val (w, g) = try {
            WeatherRepository.getCurrentWithGeo(key)
        } catch (_: Exception) {
            return@withContext
        }

        val canonical = w.name.trim() // DB 고유키로 사용할 영문 표준명
        val nameKo    = g?.local_names?.get("ko") ?: g?.name ?: w.name
        val country   = g?.country

        // 3) 표준명으로 다시 한 번 존재 확인(고유키 충돌 방지)
        dao.findByName(canonical)?.let { exist ->
            if (!exist.pinned) dao.setPinned(exist.id, true)
            // 보강 업데이트 (필요 시)
            if (exist.nameKo == null || exist.country == null) {
                dao.updateKoAndCountry(exist.id, nameKo, country)
            }
            return@withContext
        }

        // 4) 새로 삽입
        val nextOrder = dao.getMaxOrder() + 1
        val insertedId = dao.insert(
            City(
                name      = canonical,
                nameKo    = nameKo,
                country   = country,
                pinned    = true,
                sortOrder = nextOrder
            )
        )

        // 5) IGNORE로 무시된 경우(-1L) → 다시 찾아서 보정
        if (insertedId == -1L) {
            dao.findByName(canonical)?.let { exist ->
                if (!exist.pinned) dao.setPinned(exist.id, true)
                if (exist.nameKo == null || exist.country == null) {
                    dao.updateKoAndCountry(exist.id, nameKo, country)
                }
            }
        }
    }
}
