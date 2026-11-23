package com.nijobenny.spendwise.domain.usecase

import com.nijobenny.spendwise.domain.model.ExchangeRates
import com.nijobenny.spendwise.domain.repository.ExchangeRateRepository

class GetExchangeRatesUseCase(private val repository: ExchangeRateRepository) {
    suspend operator fun invoke(base: String, targets: List<String> = DEFAULT_TARGETS): Result<ExchangeRates> =
        repository.getRates(base, targets)

    companion object {
        /** Currencies for the countries I'm relocating to: Netherlands/Ireland, Switzerland, UK, US. */
        val DEFAULT_TARGETS = listOf("EUR", "CHF", "GBP", "USD")
    }
}
