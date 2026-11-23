package com.nijobenny.spendwise.data.repository

import com.nijobenny.spendwise.data.remote.FrankfurterApi
import com.nijobenny.spendwise.data.remote.toExchangeRates
import com.nijobenny.spendwise.domain.model.ExchangeRates
import com.nijobenny.spendwise.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.CancellationException

/** Fetches rates over the network and keeps them in memory for [cacheMillis] to avoid repeat calls. */
class FrankfurterExchangeRateRepository(
    private val api: FrankfurterApi,
    private val currentTimeMillis: () -> Long,
    private val cacheMillis: Long = 60 * 60 * 1000L,
) : ExchangeRateRepository {

    private var cached: Pair<Long, ExchangeRates>? = null

    override suspend fun getRates(base: String, targets: List<String>): Result<ExchangeRates> {
        val now = currentTimeMillis()
        cached?.let { (fetchedAt, rates) ->
            if (rates.base == base.uppercase() && now - fetchedAt < cacheMillis && rates.rates.keys.containsAll(targets)) {
                return Result.success(rates)
            }
        }
        return try {
            val rates = api.latestRates(base, targets).toExchangeRates(base)
            cached = now to rates
            Result.success(rates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
