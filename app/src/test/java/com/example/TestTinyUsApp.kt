package com.example

import android.content.ContentProvider
import android.content.pm.ProviderInfo

/**
 * The app as Robolectric runs it. On a device, Compose resources (the shared text) get the app's
 * context from a content provider Android starts before the Application; Robolectric starts no
 * providers, so this attaches it first. The provider class is internal to the library in Kotlin
 * terms, hence the lookup by name.
 */
class TestTinyUsApp : TinyUsApp() {
    override fun onCreate() {
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
            .getDeclaredConstructor().newInstance() as ContentProvider
        provider.attachInfo(this, ProviderInfo().apply { authority = "$packageName.resources.AndroidContextProvider" })
        super.onCreate()
    }
}
