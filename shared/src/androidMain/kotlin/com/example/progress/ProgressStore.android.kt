package com.example.progress

import android.content.SharedPreferences
import com.example.data.SharedPreferencesStorage

/** [ProgressStore] on Android preferences (tiny_us_prefs), as it was always opened. */
fun ProgressStore(prefs: SharedPreferences): ProgressStore = ProgressStore(SharedPreferencesStorage(prefs))
