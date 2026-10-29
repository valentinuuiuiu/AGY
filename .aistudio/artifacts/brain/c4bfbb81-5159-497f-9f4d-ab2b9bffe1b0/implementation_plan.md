# Jules AGY — Cutting-Edge AI Companion Architecture

## Overview
A complete transformation of the application from a basic translator into **Jules AGY**, an ultra-advanced multimodal AI copilot and agentic travel companion.

---

## The 6 Completed Pillars

### 1. Real-time Multimodal Live Copilot ("Jarvis" Experience)
- **Engine**: `com.example.services.GeminiLiveService.kt`
- **Protocol**: Direct WebSocket connection (`wss://generativelanguage.googleapis.com/.../BidiGenerateContent`) using `models/gemini-2.0-flash-exp`.
- **Bidirectional Streaming**: Low-latency 16kHz PCM audio microphone streaming and 24kHz PCM speech playback with `AudioTrack`.
- **Vision Pipeline**: Continuous 640px JPEG camera frames sent via `realtimeInput.mediaChunks`.
- **Integrated Tool Calling**: WebSocket server dispatches `toolCall` frames; Jules executes the Android tools and responds with `toolResponse` frames in real time.
- **HUD Interface**: `com.example.ui.screens.GeminiLiveScreen.kt` with `HolographicReticle`, `JarvisRadarPulse`, and real-time `CyberWaveformVisualizer`.

### 2. Context-Aware "Infinite Memory" Brain (Gemini 1.5 Pro & Local RAG)
- **Engine**: `com.example.services.MemoryRagService.kt`
- **Context Synthesis**: Ingests all offline records from Room Database (`travel_notes`, `saved_phrases`, `itineraries`) into a structured semantic prompt.
- **2M Token Capability**: Powered by `gemini-1.5-pro` (with `gemini-2.0-flash` fallback) for deep personalization ("Where should I go next?", "Synthesize my journey").
- **Metrics Dashboard**: Displays real-time indexed context token counts and entity breakdown.
- **UI**: `com.example.ui.screens.MemoryBrainScreen.kt` with quick prompt chips and offline memory record vault.

### 3. Agentic Tool Calling (AGY Nexus Evolution)
- **Engine**: `com.example.services.AgenticToolsService.kt`
- **Tool Declarations**:
  1. `save_to_memory`: Autonomously persists recommendations and discoveries to Room DB (`TravelNoteDao`).
  2. `search_places_and_navigate`: Launches Google Maps search or turn-by-turn GPS navigation intents (`geo:`, `google.navigation:q=`).
  3. `get_device_telemetry`: Extracts live battery percentage, network type (Wi-Fi/Cellular/Offline), and device posture.
  4. `open_app`: Launches installed Android apps on device.
  5. `translate_and_store`: Translates and saves to offline phrasebook (`SavedPhraseDao`).
- **Command Center**: `com.example.ui.screens.NexusScreen.kt` with real-time execution stream timeline and manual agent action triggers.

### 4. Immersive AR Translation & World Lens
- **Viewfinder**: `com.example.ui.screens.ArLensScreen.kt` powered by CameraX.
- **Holographic AR Reticle**: Corner brackets with target pulse.
- **3 Dynamic Modes**:
  - `MENU SCANNER`: Dish breakdown with dietary tags (Vegan, Halal, Gluten-Free) and high-visibility Allergen Alerts.
  - `WORLD LENS`: Street signs, public transit, and landmark explanations.
  - `CULTURE HUD`: Local etiquette rules and historical background.
- **Memory Integration**: Direct 1-tap "Save to Infinite Memory" button.

### 5. Advanced Creation Studio (Imagen 3 & Veo)
- **Workstation**: `com.example.ui.screens.CreateScreen.kt` & `CreateViewModel.kt`
- **Gemini Magic Prompt Expander**: Upgrades simple prompts into rich cinematic descriptions.
- **Curated Style Presets**: Cyberpunk 2077, Hyper-Realistic 8K, Retro Synthwave, Cinematic Travel, Ukiyo-e Sci-Fi, Blade Runner Noir.
- **Controls**: Negative prompts, canvas aspect ratios (1:1, 16:9, 9:16, 4:3, 21:9), and Imagen 3 / Veo 3.1 neural pipelines.

### 6. Next-Gen Cyberpunk / Glassmorphism UI
- **Design System**: `com.example.ui.theme.Color.kt`, `Theme.kt`, and `CyberModifiers.kt`.
- **Palette**: `CyberBackground` (`#07090E`), `CyberSurface` (`#0D121F`), `NeonCyan` (`#00F5D4`), `NeonPurple` (`#7928CA`), `NeonPink` (`#FF007F`), `ElectricBlue` (`#0070F3`).
- **Components**: Frosted glass cards (`cyberGlass()`), gradient borders, monospaced cyber badges, ambient atmospheric glow, and sleek bottom navigation bar in `MainActivity.kt`.

---

## Verification
- Compilation verified cleanly with `compile_applet` (exit code 0).
- All dependencies verified: OkHttp WebSocket, Retrofit, CameraX, Room Database, Accompanist, and Coroutines.
