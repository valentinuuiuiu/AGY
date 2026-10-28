package com.example.data.translation

/**
 * Essential Offline Travel Dictionary & Emergency Phrasebook
 * Works 100% without internet access in remote locations or abroad.
 */
data class OfflinePhrase(
    val category: String,
    val ro: String,
    val en: String,
    val es: String,
    val fr: String,
    val de: String,
    val it: String,
    val jaPhonetic: String
)

object OfflineDictionary {
    val categories = listOf("Urgențe", "Transport / Direcții", "Cazare", "Mâncare & Restaurant", "Bani & Cumpărături")

    val phrases = listOf(
        OfflinePhrase(
            category = "Urgențe",
            ro = "Am nevoie de ajutor urgent!",
            en = "I need urgent help!",
            es = "¡Necesito ayuda urgente!",
            fr = "J'ai besoin d'aide urgente !",
            de = "Ich brauche dringende Hilfe!",
            it = "Ho bisogno di aiuto urgente!",
            jaPhonetic = "Kyuukyuu no tasuke ga hitsuyou desu!"
        ),
        OfflinePhrase(
            category = "Urgențe",
            ro = "Unde este cel mai apropiat spital sau farmacie?",
            en = "Where is the nearest hospital or pharmacy?",
            es = "¿Dónde está el hospital o farmacia más cercano?",
            fr = "Où est l'hôpital ou la pharmacie la plus proche ?",
            de = "Wo ist das nächste Krankenhaus oder die nächste Apotheke?",
            it = "Dov'è l'ospedale o la farmacia più vicina?",
            jaPhonetic = "Moyori no byouin ya yakkyoku wa doko desu ka?"
        ),
        OfflinePhrase(
            category = "Transport / Direcții",
            ro = "Cât costă o călătorie până la aeroport?",
            en = "How much does a trip to the airport cost?",
            es = "¿Cuánto cuesta un viaje al aeropuerto?",
            fr = "Combien coûte un trajet jusqu'à l'aéroport ?",
            de = "Wie viel kostet eine Fahrt zum Flughafen?",
            it = "Quanto costa una corsa per l'aeroporto?",
            jaPhonetic = "Kuukou made ikura desu ka?"
        ),
        OfflinePhrase(
            category = "Transport / Direcții",
            ro = "Puteți să-mi arătați pe hartă unde ne aflăm?",
            en = "Can you show me on the map where we are?",
            es = "¿Puede mostrarme en el mapa dónde estamos?",
            fr = "Pouvez-vous me montrer sur la carte où nous sommes ?",
            de = "Können Sie mir auf der Karte zeigen, wo wir sind?",
            it = "Può mostrarmi sulla mappa dove siamo?",
            jaPhonetic = "Genzaichi o chizu de oshiete kudasai."
        ),
        OfflinePhrase(
            category = "Cazare",
            ro = "Am o rezervare pe numele Valentin.",
            en = "I have a reservation under the name Valentin.",
            es = "Tengo una reserva a nombre de Valentin.",
            fr = "J'ai une réservation au nom de Valentin.",
            de = "Ich habe eine Reservierung auf den Namen Valentin.",
            it = "Ho una prenotazione a nome di Valentin.",
            jaPhonetic = "Valentin no namae de yoyaku ga arimasu."
        ),
        OfflinePhrase(
            category = "Mâncare & Restaurant",
            ro = "Nota de plată, vă rog.",
            en = "The check / bill, please.",
            es = "La cuenta, por favor.",
            fr = "L'addition, s'il vous plaît.",
            de = "Die Rechnung, bitte.",
            it = "Il conto, per favore.",
            jaPhonetic = "Okaikei o onegaishimasu."
        ),
        OfflinePhrase(
            category = "Bani & Cumpărături",
            ro = "Acceptați plata cu cardul sau doar numerar?",
            en = "Do you accept card payments or cash only?",
            es = "¿Aceptan tarjeta o solo efectivo?",
            fr = "Acceptez-vous les cartes ou seulement les espèces ?",
            de = "Akzeptieren Sie Karte oder nur Bargeld?",
            it = "Accettate la carta o solo contanti?",
            jaPhonetic = "Kaado wa tsukaemasu ka, genkin nomi desu ka?"
        )
    )

    fun getPhrasesForCategory(cat: String): List<OfflinePhrase> {
        return phrases.filter { it.category == cat }
    }
}
