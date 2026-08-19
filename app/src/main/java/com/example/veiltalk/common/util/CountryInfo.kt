package com.example.veiltalk.common.util

data class CountryInfo(
    val name: String,
    val code: String,
    val flag: String
)

object CountryCodes {
    val countries = listOf(
        CountryInfo("ایران", "+98", "🇮🇷"),
        CountryInfo("ایالات متحده", "+1", "🇺🇸"),
        CountryInfo("انگلستان", "+44", "🇬🇧"),
        CountryInfo("آلمان", "+49", "🇩🇪"),
        CountryInfo("اسپانیا", "+34", "🇪🇸"),
        CountryInfo("هلند", "+31", "🇳🇱"),
        CountryInfo("فرانسه", "+33", "🇫🇷"),
        CountryInfo("کانادا", "+1", "🇨🇦"),
        CountryInfo("ترکیه", "+90", "🇹🇷"),
        CountryInfo("امارات", "+971", "🇦🇪"),
        CountryInfo("عراق", "+964", "🇮🇶"),
        CountryInfo("افغانستان", "+93", "🇦🇫"),
        CountryInfo("روسیه", "+7", "🇷🇺"),
        CountryInfo("چین", "+86", "🇨🇳"),
        CountryInfo("ژاپن", "+81", "🇯🇵"),
        CountryInfo("هند", "+91", "🇮🇳"),
        CountryInfo("برزیل", "+55", "🇧🇷"),
        CountryInfo("استرالیا", "+61", "🇦🇺"),
    ).sortedBy { it.name }
}
