package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.AppControlService
import com.example.services.AppMcpTool
import com.example.services.NotebookLMService
import com.example.services.ssh.SshService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val appControlService = AppControlService(application)
    private val notebookLMService = NotebookLMService()
    private val sshService = SshService()

    private val _mcpTools = MutableStateFlow<List<AppMcpTool>>(emptyList())
    val mcpTools: StateFlow<List<AppMcpTool>> = _mcpTools.asStateFlow()

    private val _ragStatus = MutableStateFlow("Disconnected")
    val ragStatus: StateFlow<String> = _ragStatus.asStateFlow()

    private val _sshOutput = MutableStateFlow("Terminal Standby")
    val sshOutput: StateFlow<String> = _sshOutput.asStateFlow()

    fun scanApps() {
        viewModelScope.launch {
            val tools = appControlService.scanAndRegisterApps()
            _mcpTools.value = tools
        }
    }

    fun launchApp(packageName: String) {
        appControlService.launchAppByPackage(packageName)
    }

    fun queryBrain(prompt: String) {
        viewModelScope.launch {
            _ragStatus.value = "Querying RAG Brain..."
            val result = notebookLMService.askBrain(prompt)
            if (result.isSuccess) {
                _ragStatus.value = "Success: ${result.getOrNull()?.answer}"
            } else {
                _ragStatus.value = "Error: Connection refused (MCP Hub offline)"
            }
        }
    }

    fun executeRemoteCommand(command: String, password: String) {
        viewModelScope.launch {
            _sshOutput.value = "Executing '$command' on aryan@100.91.238.121..."
            val result = sshService.executeCommand(command, password)
            if (result.isSuccess) {
                _sshOutput.value = result.getOrNull() ?: "Success"
            } else {
                _sshOutput.value = result.exceptionOrNull()?.message ?: "Unknown Error"
            }
        }
    }
}
