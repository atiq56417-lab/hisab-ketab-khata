package com.hisabkitab.khata

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hisabkitab.khata.data.AppDatabase
import com.hisabkitab.khata.data.BackupManager
import com.hisabkitab.khata.data.Repository
import com.hisabkitab.khata.data.SettingsStore

class KhataApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: Repository by lazy { Repository(database) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val backup: BackupManager by lazy { BackupManager(this, database) }

    override fun onCreate() {
        super.onCreate()
        // Urdu is the default language until the user picks another one.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ur"))
        }
    }
}
