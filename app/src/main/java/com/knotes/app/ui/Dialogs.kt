package com.knotes.app.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.knotes.app.R

/* Diálogo SIEMPRE claro — el del sistema puede estar en modo oscuro y
   con textos grises no se entiende nada. */
object Dialogs {
    fun light(ctx: Context): AlertDialog.Builder =
        AlertDialog.Builder(ctx, R.style.Theme_KNotes_LightDialog)
}
