package com.nijobenny.spendwise.domain.repository

import com.nijobenny.spendwise.domain.model.ExchangeRates

interface ExchangeRateRepository {
    suspend fun getRates(base: String, targets: List<String>): Result<ExchangeRates>
}
