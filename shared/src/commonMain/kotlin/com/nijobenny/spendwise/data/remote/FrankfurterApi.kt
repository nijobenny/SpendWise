package com.nijobenny.spendwise.data.remote

import com.nijobenny.spendwise.domain.model.ExchangeRates
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/** One rate entry from https://api.frankfurter.dev/v2/rates (free, no API key, ECB data). */
@Serializable
data class RateDto(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

class FrankfurterApi(private val client: HttpClient) {

    suspend fun latestRates(base: String, quotes: List<String>): List<RateDto> =
        client.get("https://api.frankfurter.dev/v2/rates") {
            parameter("base", base.lowercase())
            parameter("quotes", quotes.joinToString(",") { it.lowercase() })
        }.body()
}

/** Maps the API's list of rate rows into one domain object. */
fun List<RateDto>.toExchangeRates(base: String): ExchangeRates = ExchangeRates(
    base = base.uppercase(),
    rates = associate { it.quote.uppercase() to it.rate },
    date = maxOfOrNull { it.date } ?: "",
)
