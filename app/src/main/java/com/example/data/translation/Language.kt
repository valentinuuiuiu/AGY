package com.example.data.translation

import java.util.Locale

data class Language(
    val code: String,
    val nameRo: String,
    val nativeName: String,
    val flag: String,
    val ttsLocale: Locale
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            Language("ro", "Română", "Română", "🇷🇴", Locale("ro")),
            Language("en", "Engleză", "English", "🇬🇧", Locale.ENGLISH),
            Language("it", "Italiană", "Italiano", "🇮🇹", Locale.ITALIAN),
            Language("es", "Spaniolă", "Español", "🇪🇸", Locale("es")),
            Language("fr", "Franceză", "Français", "🇫🇷", Locale.FRENCH),
            Language("de", "Germană", "Deutsch", "🇩🇪", Locale.GERMAN),
            Language("ja", "Japoneză", "日本語", "🇯🇵", Locale.JAPANESE),
            Language("zh", "Chineză", "中文", "🇨🇳", Locale.CHINESE),
            Language("ko", "Coreeană", "한국어", "🇰🇷", Locale.KOREAN),
            Language("tr", "Turcă", "Türkçe", "🇹🇷", Locale("tr")),
            Language("el", "Greacă", "Ελληνικά", "🇬🇷", Locale("el")),
            Language("ar", "Arabă", "العربية", "🇸🇦", Locale("ar")),
            Language("pt", "Portugheză", "Português", "🇵🇹", Locale("pt")),
            Language("th", "Thailandeză", "ไทย", "🇹🇭", Locale("th")),
            Language("vi", "Vietnameză", "Tiếng Việt", "🇻🇳", Locale("vi")),
            Language("nl", "Olandeză", "Nederlands", "🇳🇱", Locale("nl"))
        )

        fun findByCode(code: String): Language {
            return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
                ?: SUPPORTED_LANGUAGES[0]
        }
    }
}
