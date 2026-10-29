package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.AgenticToolsService
import com.example.services.AppControlService
import com.example.services.AppMcpTool
import com.example.services.ToolExecutionLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val appControlService = AppControlService(application)
    private val agenticTools = AgenticToolsService.getInstance(application)

    val executionLogs: StateFlow<List<ToolExecutionLog>> = agenticTools.executionLogs

    private val _mcpTools = MutableStateFlow<List<AppMcpTool>>(emptyList())
    val mcpTools: StateFlow<List<AppMcpTool>> = _mcpTools.asStateFlow()

    private val _telemetryStatus = MutableStateFlow<String?>(null)
    val telemetryStatus: StateFlow<String?> = _telemetryStatus.asStateFlow()

    init {
        scanApps()
        runTelemetryCheck()
    }

    fun scanApps() {
        viewModelScope.launch {
            val tools = appControlService.scanAndRegisterApps()
            _mcpTools.value = tools
        }
    }

    fun launchApp(packageName: String) {
        appControlService.launchAppByPackage(packageName)
    }

    fun runTelemetryCheck() {
        viewModelScope.launch {
            val result = agenticTools.executeTool("get_device_telemetry", JSONObject())
            _telemetryStatus.value = result.optString("message", "Telemetry OK")
        }
    }

    fun searchPlaces(query: String, navigate: Boolean = false) {
        viewModelScope.launch {
            val args = JSONObject().apply {
                put("query", query)
                put("navigate", navigate)
            }
            agenticTools.executeTool("search_places_and_navigate", args)
        }
    }

    fun testSaveMemory(title: String, content: String, category: String = "Recommendation") {
        viewModelScope.launch {
            val args = JSONObject().apply {
                put("title", title)
                put("content", content)
                put("category", category)
            }
            agenticTools.executeTool("save_to_memory", args)
        }
    }

    fun clearLogs() {
        agenticTools.clearLogs()
    }
}
