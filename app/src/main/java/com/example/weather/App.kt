// App.kt
package com.example.weather

import android.app.Application
import com.example.weather.data.AppDatabase

class App : Application() {
    companion object { lateinit var db: AppDatabase; private set }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(this)   // ✅ 여기가 유일한 DB 생성 경로
    }
}
