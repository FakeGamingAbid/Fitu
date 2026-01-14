package com.fitu.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object AutoStartManager {

    private const val TAG = "AutoStartManager"

    enum class Manufacturer {
        XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, LETV, ASUS, NOKIA, MEIZU, OTHER
    }

    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> Manufacturer.XIAOMI
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oppo") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("letv") || manufacturer.contains("leeco") -> Manufacturer.LETV
            manufacturer.contains("asus") -> Manufacturer.ASUS
            manufacturer.contains("nokia") -> Manufacturer.NOKIA
            manufacturer.contains("meizu") -> Manufacturer.MEIZU
            else -> Manufacturer.OTHER
        }
    }

    fun hasAutoStartFeature(): Boolean = getManufacturer() != Manufacturer.OTHER

    fun getManufacturerName(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> "Xiaomi/Redmi"
            Manufacturer.HUAWEI -> "Huawei/Honor"
            Manufacturer.OPPO -> "Oppo"
            Manufacturer.VIVO -> "Vivo"
            Manufacturer.SAMSUNG -> "Samsung"
            Manufacturer.ONEPLUS -> "OnePlus"
            Manufacturer.LETV -> "LeEco"
            Manufacturer.ASUS -> "Asus"
            Manufacturer.NOKIA -> "Nokia"
            Manufacturer.MEIZU -> "Meizu"
            Manufacturer.OTHER -> Build.MANUFACTURER
        }
    }

    fun getAutoStartInstructions(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> "1. Find 'Fitu' in the app list\n2. Enable 'Autostart' toggle\n3. Enable 'Start in background'"
            Manufacturer.HUAWEI -> "1. Find 'Fitu' and tap on it\n2. Enable 'Manage manually'\n3. Enable all three toggles"
            Manufacturer.OPPO -> "1. Find 'Fitu' in the app list\n2. Enable 'Allow Auto-startup'\n3. Enable 'Allow Background Activity'"
            Manufacturer.VIVO -> "1. Find 'Fitu' in the list\n2. Enable 'Background Power Consumption'\n3. Set to 'Unrestricted'"
            Manufacturer.SAMSUNG -> "1. Find 'Fitu' in the app list\n2. Tap 'Battery'\n3. Select 'Unrestricted'"
            Manufacturer.ONEPLUS -> "1. Find 'Fitu' in the app list\n2. Enable 'Allow auto-launch'\n3. Set Battery to 'Don't optimize'"
            else -> "1. Open device Settings\n2. Go to Apps\n3. Find 'Fitu'\n4. Enable auto-start"
        }
    }

    private val autoStartIntents = listOf(
        // Xiaomi
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
        // Huawei
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        // Oppo
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        // Vivo
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        // Samsung
        Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
        // OnePlus
        Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")),
        // Asus
        Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
        // Nokia
        Intent().setComponent(ComponentName("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity")),
        // Meizu
        Intent().setComponent(ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"))
    )

    fun openAutoStartSettings(context: Context): Boolean {
        for (intent in autoStartIntents) {
            if (isIntentAvailable(context, intent)) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open auto-start settings", e)
                }
            }
        }
        return openAppInfoSettings(context)
    }

    fun openAppInfoSettings(context: Context): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
