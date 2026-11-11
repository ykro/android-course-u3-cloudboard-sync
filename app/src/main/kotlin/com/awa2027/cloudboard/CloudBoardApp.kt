package com.awa2027.cloudboard

import android.app.Application
import com.awa2027.cloudboard.di.appModules
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CloudBoardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Con google-services.json el SDK crea la app de Firebase al arrancar; sin él, modo demo.
        val cloudEnabled = FirebaseApp.getApps(this).isNotEmpty()
        startKoin {
            androidContext(this@CloudBoardApp)
            modules(appModules(cloudEnabled))
        }
    }
}
