package com.example.services

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.data.room.AppDatabase
import com.example.data.room.SavedPhrase
import com.example.data.room.TravelNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ToolExecutionLog(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val toolName: String,
    val argumentsJson: String,
    val resultSummary: String,
    val status: ToolStatus = ToolStatus.SUCCESS,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

enum class ToolStatus {
    PENDING, SUCCESS, FAILED
}

class AgenticToolsService private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val appControlService = AppControlService(context)

    private val _executionLogs = MutableStateFlow<List<ToolExecutionLog>>(emptyList())
    val executionLogs: StateFlow<List<ToolExecutionLog>> = _executionLogs.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AgenticToolsService? = null

        fun getInstance(context: Context): AgenticToolsService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgenticToolsService(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Returns the JSON declarations for all tools compatible with Gemini REST & WebSocket API.
         */
        fun getGeminiToolDeclarations(): JSONArray {
            return JSONArray().apply {
                // Tool 1: save_to_memory
                put(JSONObject().apply {
                    put("name", "save_to_memory")
                    put("description", "Save an important travel note, recommendation, hidden gem, or itinerary item to local infinite memory.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("title", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Short title or place name")
                            })
                            put("content", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The detailed note, tip, address, or recommendation")
                            })
                            put("category", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Category: Food, Sightseeing, Transport, Warning, Culture")
                            })
                        })
                        put("required", JSONArray().apply {
                            put("title")
                            put("content")
                        })
                    })
                })

                // Tool 2: search_places_and_navigate
                put(JSONObject().apply {
                    put("name", "search_places_and_navigate")
                    put("description", "Search for a location, hidden gem, or trigger Google Maps turn-by-turn navigation for the user.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("query", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Name or address of the destination, restaurant, or landmark")
                            })
                            put("navigate", JSONObject().apply {
                                put("type", "BOOLEAN")
                                put("description", "True to launch active GPS navigation, False for map preview")
                            })
                        })
                        put("required", JSONArray().apply {
                            put("query")
                        })
                    })
                })

                // Tool 3: get_device_telemetry
                put(JSONObject().apply {
                    put("name", "get_device_telemetry")
                    put("description", "Read battery percentage, network connectivity, and local time for proactive contextual advice.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })

                // Tool 4: open_app
                put(JSONObject().apply {
                    put("name", "open_app")
                    put("description", "Launch an installed Android app on behalf of the user (e.g. Uber, Maps, Spotify, Camera).")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("appName", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Human readable name or keyword of the app")
                            })
                        })
                        put("required", JSONArray().apply {
                            put("appName")
                        })
                    })
                })

                // Tool 5: translate_and_store
                put(JSONObject().apply {
                    put("name", "translate_and_store")
                    put("description", "Translate a phrase and store it in the user's offline phrasebook.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("originalText", JSONObject().apply { put("type", "STRING") })
                            put("translatedText", JSONObject().apply { put("type", "STRING") })
                            put("sourceLang", JSONObject().apply { put("type", "STRING") })
                            put("targetLang", JSONObject().apply { put("type", "STRING") })
                            put("phonetic", JSONObject().apply { put("type", "STRING") })
                            put("category", JSONObject().apply { put("type", "STRING") })
                        })
                        put("required", JSONArray().apply {
                            put("originalText")
                            put("translatedText")
                        })
                    })
                })
            }
        }
    }

    /**
     * Executes a tool invocation dispatched by Gemini API or WebSocket.
     */
    suspend fun executeTool(name: String, args: JSONObject): JSONObject {
        Log.i("AgenticTools", "Executing tool: $name with args: $args")
        return try {
            val response = when (name) {
                "save_to_memory" -> handleSaveToMemory(args)
                "search_places_and_navigate" -> handleSearchPlaces(args)
                "get_device_telemetry" -> handleDeviceTelemetry()
                "open_app" -> handleOpenApp(args)
                "translate_and_store" -> handleTranslateAndStore(args)
                else -> {
                    JSONObject().apply {
                        put("success", false)
                        put("error", "Unknown tool function: $name")
                    }
                }
            }

            addLog(
                ToolExecutionLog(
                    toolName = name,
                    argumentsJson = args.toString(),
                    resultSummary = response.optString("message", response.toString()),
                    status = if (response.optBoolean("success", true)) ToolStatus.SUCCESS else ToolStatus.FAILED
                )
            )

            response
        } catch (e: Exception) {
            Log.e("AgenticTools", "Execution failed for $name", e)
            val errJson = JSONObject().apply {
                put("success", false)
                put("error", e.message ?: "Execution failed")
            }
            addLog(
                ToolExecutionLog(
                    toolName = name,
                    argumentsJson = args.toString(),
                    resultSummary = "Error: ${e.message}",
                    status = ToolStatus.FAILED
                )
            )
            errJson
        }
    }

    private suspend fun handleSaveToMemory(args: JSONObject): JSONObject {
        val title = args.optString("title", "Jules Note")
        val content = args.optString("content", "")
        val category = args.optString("category", "General")

        val note = TravelNote(
            title = title,
            destination = category,
            type = TravelNote.NOTE_TYPE_GENERATED,
            transcriptOrPrompt = "Jules Autonomous Agent Action",
            aiResponse = content,
            tags = "Agentic, $category",
            isFavorite = true
        )
        db.travelNoteDao().insertNote(note)

        return JSONObject().apply {
            put("success", true)
            put("message", "Saved '$title' to Infinite Memory (Room DB)")
            put("title", title)
            put("category", category)
        }
    }

    private fun handleSearchPlaces(args: JSONObject): JSONObject {
        val query = args.optString("query", "")
        val navigate = args.optBoolean("navigate", false)

        val uri = if (navigate) {
            Uri.parse("google.navigation:q=${Uri.encode(query)}")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(mapIntent)
            JSONObject().apply {
                put("success", true)
                put("message", "Launched Google Maps for '$query'")
                put("query", query)
                put("mode", if (navigate) "navigation" else "search")
            }
        } catch (e: Exception) {
            // Fallback to browser search
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            JSONObject().apply {
                put("success", true)
                put("message", "Opened Maps web view for '$query'")
                put("query", query)
            }
        }
    }

    private fun handleDeviceTelemetry(): JSONObject {
        val batteryStatus: Intent? = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netType = when {
            isWifi -> "Wi-Fi High Speed"
            isCellular -> "Cellular 5G/LTE"
            else -> "Offline / Airplane"
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return JSONObject().apply {
            put("success", true)
            put("batteryPercent", batteryPct)
            put("network", netType)
            put("timestamp", currentTime)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("message", "Battery: $batteryPct%, Network: $netType, Time: $currentTime")
        }
    }

    private fun handleOpenApp(args: JSONObject): JSONObject {
        val appName = args.optString("appName", "").lowercase()
        val apps = appControlService.scanAndRegisterApps()
        val match = apps.firstOrNull { it.appName.lowercase().contains(appName) }

        return if (match != null) {
            val launched = appControlService.launchAppByPackage(match.packageName)
            JSONObject().apply {
                put("success", launched)
                put("message", if (launched) "Launched ${match.appName}" else "Failed to launch ${match.appName}")
                put("package", match.packageName)
            }
        } else {
            JSONObject().apply {
                put("success", false)
                put("message", "App matching '$appName' not found on device")
            }
        }
    }

    private suspend fun handleTranslateAndStore(args: JSONObject): JSONObject {
        val original = args.optString("originalText", "")
        val translated = args.optString("translatedText", "")
        val source = args.optString("sourceLang", "auto")
        val target = args.optString("targetLang", "en")
        val phonetic = args.optString("phonetic", "")
        val category = args.optString("category", "General")

        val phrase = SavedPhrase(
            originalText = original,
            translatedText = translated,
            sourceLanguage = source,
            targetLanguage = target,
            phonetic = phonetic,
            category = category,
            isFavorite = true
        )
        db.savedPhraseDao().insertPhrase(phrase)

        return JSONObject().apply {
            put("success", true)
            put("message", "Stored phrase in local offline phrasebook: '$original' -> '$translated'")
        }
    }

    private fun addLog(log: ToolExecutionLog) {
        val current = _executionLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 50) current.removeAt(current.lastIndex)
        _executionLogs.value = current
    }

    fun clearLogs() {
        _executionLogs.value = emptyList()
    }
}
