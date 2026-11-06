package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
