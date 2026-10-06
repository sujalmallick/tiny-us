package com.example.data

import android.content.Context

/** The app's saved data ("tiny_us_prefs"), as `PreferencesManager(context)` always opened it. */
fun PreferencesManager(context: Context): PreferencesManager =
    PreferencesManager(SharedPreferencesStorage(context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE)))
