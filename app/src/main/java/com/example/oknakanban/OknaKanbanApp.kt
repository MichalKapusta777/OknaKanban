package com.example.oknakanban

import android.app.Application
import com.example.oknakanban.data.AppDatabase

class OknaKanbanApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
