package com.example.core.i18n

enum class AppLanguage(val code: String, val toggleLabel: String) {
    EN("en", "EN"),
    BN("bn", "বাংলা");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}
