package com.jeremy.lumi.utils

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun wrapContext(context: Context, languageCode: String): Context {
        val targetLocale = getTargetLocale(languageCode)
        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(targetLocale)
        return context.createConfigurationContext(config)
    }

    fun applyLocale(activity: Activity, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = activity.getSystemService(LocaleManager::class.java)
            if (languageCode == "system") {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }
        }

        val targetLocale = getTargetLocale(languageCode)
        Locale.setDefault(targetLocale)

        val config = Configuration(activity.resources.configuration)
        config.setLocale(targetLocale)
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

        activity.recreate()
    }

    fun getTargetLocale(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "es" -> Locale("es")
            else -> {
                val systemLocales = Resources.getSystem().configuration.locales
                if (!systemLocales.isEmpty) systemLocales[0] else Locale.getDefault()
            }
        }
    }

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }
}
