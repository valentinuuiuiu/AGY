# WanderAI: Asistent Inteligent de Călătorie & Traducător Universal Live

Un salt major pentru WanderAI axat pe depășirea barierelor lingvistice în călătorii internaționale: traducător vocal bidirecțional față-în-față, scanare vizuală multimodală pentru meniuri și indicatoare, dicționar salvat offline în Room și generare de rezumate multimedia de călătorie.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Prioritate Confirmată de Utilizator**: Focus maxim pe limbaj și traducere universală în orice limbă pentru a ajuta călătorii să comunice fluent la fața locului (față în față, meniuri, indicatoare, situații de urgență), combinat cu salvare offline și experiențe multimedia spectaculoase.

- **Modulul Principal de Comunicare**: Traducător bidirecțional tip *Dual-Persona* (Split-Screen) unde două persoane pot vorbi consecutiv în limbi diferite, beneficiind de traducere vocală (TTS) și afișare text instant.
- **Traducere Vizuală Multimodală (Camera / Menu Lens)**: Scanare directă a meniurilor sau panourilor stradale prin Gemini Multimodal, cu explicații culinare și culturale, nu doar traducere mecanică.
- **Persistență Offline în Room**: Baza de date locală este extinsă pentru a salva automat istoricul traducerilor, fraze utile de supraviețuire (Urgențe, Taxi, Restaurant, Cazare) și itinerariile cu coordonate GPS, accesibile complet offline fără roaming.
- **Estetică Vizuală**: Temă modernă dark de explorare cu accente vibrante (Cyan Ocean `#06B6D4`, Indigo `#6366F1` și Sunset Coral `#F43F5E`), micro-animații fluide și feedback tactil.

---

### 1. Overview & Core Concept

- **Ce realizează noua versiune**:
  Transformă WanderAI dintr-o aplicație conceptuală într-un companion esențial de vacanță. Oferă o suită completă de comunicare:
  1. **Live Dual-Mic Translator**: Ecran dedicat împărțit în două cadrane (pentru călător și pentru localnic). Fiecare apasă pe cadranul său, vorbește în limba maternă, iar aplicația traduce și pronunță instant în limba celuilalt.
  2. **Camera Visual Lens**: Îndreptarea camerei spre meniuri, indicatoare sau etichete într-o limbă străină; AI-ul extrage textul, îl traduce și oferă context (ex: alergeni, ingrediente, sfaturi culturale).
  3. **Smart Pocket Phrasebook**: Ghid de conversație salvat local în Room, organizat pe categorii (Urgențe medicale, Tranzacții, Transport, Hotel) pentru utilizare fără semnal sau roaming.
  4. **AI Travel Storyboard & Planner**: Păstrează generatorul inteligent de itinerarii cu coordonate pe hartă și generează rezumate vizuale ale locurilor explorate.

- **Public Țintă**: Călători internaționali, turiști independenți, backpackeri și oricine vizitează țări cu limbi sau alfabete necunoscute (Asia, Orientul Mijlociu, Europa, America Latină).
- **Valoare Adăugată**: Elimină anxietatea de a comanda mâncare sau de a cere indicații în străinătate; funcționează fiabil chiar și fără date mobile datorită salvării offline.

---

### 2. User Experience & Visual Design

#### Fluxuri de Utilizator Principale

1. **Fluxul „Față-în-Față” (Face-to-Face Dual Translator)**:
   - Utilizatorul selectează perechea de limbi (ex: Română ↔ Japoneză, Română ↔ Italiană, etc.).
   - Ecranul se împarte în două secțiuni orientate ergonomic: jumătatea superioară este orientată către interlocutor, iar cea inferioară către călător.
   - La apăsarea butonului de microfon, vocea este transcrisă și tradusă automat; aplicația redă pronunția audio și afișează transcrierea mare și lizibilă.
   - Buton rapid de salvare cu o singură atingere (⭐️ Salvează în Phrasebook).

2. **Fluxul „Scanare Vizuală Meniu / Indicator” (Multimodal Lens)**:
   - Utilizatorul deschide camera sau alege o poză din galerie.
   - Selectează modul „Meniu & Culinărie” sau „Indicatoare stradale”.
   - Gemini analizează imaginea, evidențiază termenii cheie și afișează o fișă clară cu traducerea, pronunția fonetică și explicații utile.

3. **Fluxul „Ghid Offline & Expresii Salvate”**:
   - Navigare directă prin categorii rapide (Salvare automată din conversații + colecții de urgență preîncărcate).
   - Redare audio sau afișare pe tot ecranul pentru a arăta ecranul șoferului de taxi sau vânzătorului fără a fi nevoie să pronunți.

#### Identitate Vizuală & Elemente de Stil
- **Cromatică**:
  - Fundal: Deep Obsidian `#0B0F19` cu carduri semi-transparente Dark Slate `#1E293B`.
  - Accente: Neon Cyan `#06B6D4` (simbolizează călătoria și conexiunea) și Vivid Amber `#F59E0B` (pentru acțiuni cheie și puncte de interes).
- **Tipografie**: Ierarhie clară, titluri aerisite și corpuri de text optimizate pentru citire rapidă în aer liber (contrast ridicat conform ghidurilor M3).
- **Componente**: Butoane rotunjite cu ripple pronunțat, indicatoare de nivel audio animate în timp real la vorbire, carduri elevate cu umbre subtile și badge-uri de limbă personalizate cu steaguri și coduri ISO.

---

### 3. Key Product Decisions & Trade-Offs

- **Traducere Dual-Mode (Online Gemini + Fallback Offline)**:
  - *Abordare*: Când conexiunea la internet este activă, se folosește modelul avansat Gemini pentru nuanțe naturale, colocvialisme și context cultural bogat. Când conexiunea lipsește, aplicația comută instant pe expresiile stocate în Room Database, informând utilizatorul discret prin SnackBar.
  - *De ce*: În călătorii, accesul la roaming poate fi intermitent sau scump; siguranța de a avea un dicționar de urgență funcțional offline este crucială.
- **Interfață Dedicată de Conversație în locul unui simplu Chat Text**:
  - *Abordare*: Crearea unui ecran specializat de traducere split-screen în loc de mesagerie clasică.
  - *De ce*: Experiența utilizatorului în fața unui localnic într-un magazin sau gară necesită viteză și simplitate extremă (apăsare unică, afișare cu caractere mari), nu tastare lungă.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              WanderAI UI                               │
├──────────────┬──────────────────┬─────────────────┬────────────────────┤
│  Translate   │   Live Camera    │    Itinerary    │  Offline History   │
│ Dual-Mic Bar │ Multimodal Lens  │  Trip Creator   │   & Phrasebook     │
└───────┬──────┴─────────┬────────┴────────┬────────┴──────────┬─────────┘
        │                │                 │                   │
┌───────▼────────────────▼─────────────────▼───────────────────▼─────────┐
│                              ViewModels                                │
│       TranslateViewModel  •  GeminiLiveViewModel  •  ItineraryVM       │
└───────┬──────────────────────────────────┬───────────────────┬─────────┘
        │                                  │                   │
┌───────▼──────────────────────┐           │        ┌──────────▼─────────┐
│        Firebase / AI         │           │        │   Room Database    │
│  Gemini 3.8 Flash (Text/TTS) │           │        │ (Itineraries,      │
│  Multimodal Lens (Vision)    │           │        │  SavedPhrases,     │
│  OkHttp Live Streaming       │           │        │  OfflineCache)     │
└──────────────────────────────┘           │        └────────────────────┘
                                ┌──────────▼──────────┐
                                │ ConnectivityMonitor │
                                │ (Offline Detection) │
                                └─────────────────────┘
```

#### Structură Date (Room Entities)
- `SavedPhrase`: `id`, `sourceText`, `translatedText`, `sourceLanguage`, `targetLanguage`, `category`, `timestamp`, `isFavorite`.
- `Itinerary`: `id`, `title`, `destination`, `startDate`, `endDate`, `notes`, `latitude`, `longitude`, `timestamp`.

---

### Verificare și Compilare
La aprobarea planului, modificările vor fi integrate modular, testate la nivel de UI și compilate cu succes prin `compile_applet` pentru a genera o versiune actualizată a APK-ului.
