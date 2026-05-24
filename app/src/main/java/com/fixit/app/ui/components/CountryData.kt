package com.fixit.app.ui.auth

import kotlin.collections.first
import kotlin.collections.sortedBy

data class Country(
    val name: String,
    val dialCode: String,
    val flag: String,      // emoji flag character
    val isoCode: String,
)

val allCountries: List<Country> = listOf(
    Country("Australia", "+61", "🇦🇺", "AU"),
    Country("Bangladesh", "+880", "🇧🇩", "BD"),
    Country("Cambodia", "+855", "🇰🇭", "KH"),
    Country("Canada", "+1", "🇨🇦", "CA"),
    Country("China", "+86", "🇨🇳", "CN"),
    Country("France", "+33", "🇫🇷", "FR"),
    Country("Germany", "+49", "🇩🇪", "DE"),
    Country("Hong Kong", "+852", "🇭🇰", "HK"),
    Country("India", "+91", "🇮🇳", "IN"),
    Country("Indonesia", "+62", "🇮🇩", "ID"),
    Country("Japan", "+81", "🇯🇵", "JP"),
    Country("Laos", "+856", "🇱🇦", "LA"),
    Country("Macau", "+853", "🇲🇴", "MO"),
    Country("Malaysia", "+60", "🇲🇾", "MY"),
    Country("Myanmar", "+95", "🇲🇲", "MM"),
    Country("Nepal", "+977", "🇳🇵", "NP"),
    Country("New Zealand", "+64", "🇳🇿", "NZ"),
    Country("Pakistan", "+92", "🇵🇰", "PK"),
    Country("Philippines", "+63", "🇵🇭", "PH"),
    Country("Singapore", "+65", "🇸🇬", "SG"),
    Country("South Korea", "+82", "🇰🇷", "KR"),
    Country("Sri Lanka", "+94", "🇱🇰", "LK"),
    Country("Taiwan", "+886", "🇹🇼", "TW"),
    Country("Thailand", "+66", "🇹🇭", "TH"),
    Country("United Kingdom", "+44", "🇬🇧", "GB"),
    Country("United States", "+1", "🇺🇸", "US"),
    Country("Vietnam", "+84", "🇻🇳", "VN"),
).sortedBy { it.name }

val defaultCountry: Country = allCountries.first { it.isoCode == "KH" }