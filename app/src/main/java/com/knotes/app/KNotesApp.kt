package com.knotes.app

import android.app.Application
import com.knotes.app.data.Prefs
import com.knotes.app.data.Store
import com.knotes.app.data.ThemeKit
import com.knotes.app.ui.Gender

class KNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Store.init(this)
        // tema claro/oscuro/automático recordado aunque se cierre la app
        ThemeKit.apply()
        // ícono del lanzador según el modo (mujer rosa / hombre azul)
        Gender.syncLauncher(this)
    }
}
