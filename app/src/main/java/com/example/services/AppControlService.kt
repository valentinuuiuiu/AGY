package com.example.services

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class AppMcpTool(
    val appName: String,
    val packageName: String,
    val launchIntentAvailable: Boolean,
    val description: String
)

class AppControlService(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Scans the Android device for installed user applications and registers
     * them as potential MCP Tools (Function Calls) for Gemma/Gemini.
     */
    fun scanAndRegisterApps(): List<AppMcpTool> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val mcpTools = mutableListOf<AppMcpTool>()

        for (appInfo in installedApps) {
            // Filter out purely system apps unless they have a launch intent (like Maps/YouTube)
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || 
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                
                mcpTools.add(
                    AppMcpTool(
                        appName = appName,
                        packageName = appInfo.packageName,
                        launchIntentAvailable = launchIntent != null,
                        description = "Launch or control the Android app: $appName"
                    )
                )
            }
        }
        return mcpTools.sortedBy { it.appName }
    }

    /**
     * Executes the MCP Tool to launch a specific app.
     */
    fun launchAppByPackage(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }
        return false
    }
}
