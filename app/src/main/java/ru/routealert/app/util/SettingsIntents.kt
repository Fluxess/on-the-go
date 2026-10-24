package ru.routealert.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object SettingsIntents {

    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    fun openLocation(context: Context) {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun openFullScreenIntent(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (!tryStart(context, intent)) openAppDetails(context)
        } else {
            openAppDetails(context)
        }
    }

    /**
     * Samsung One UI often breaks ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
     * Try OEM screens, then generic battery list, then app details.
     */
    fun openBatteryOptimizations(context: Context) {
        val pkg = context.packageName
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val candidates = buildList {
            add(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$pkg")
                }
            )
            if (manufacturer.equals("samsung", ignoreCase = true)) {
                add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.battery.ui.BatteryActivity"
                        )
                    )
                )
                add(
                    Intent().setComponent(
                        ComponentName(
                            "com.samsung.android.lool",
                            "com.samsung.android.sm.battery.ui.setting.SleepingAppsActivity"
                        )
                    )
                )
                add(
                    Intent("com.samsung.android.sm.ACTION_BATTERY").setPackage("com.samsung.android.lool")
                )
            }
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", pkg, null)
                )
            )
        }

        for (intent in candidates) {
            if (tryStart(context, intent)) return
        }
        Toast.makeText(
            context,
            "Настройки → Батарея → ограничение использования в фоне → Без ограничений",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
