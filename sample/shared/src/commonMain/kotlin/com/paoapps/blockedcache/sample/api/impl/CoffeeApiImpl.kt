package com.paoapps.blockedcache.sample.api.impl

import com.paoapps.blockedcache.sample.api.CoffeeApi
import com.paoapps.blockedcache.sample.domain.Coffee
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class CoffeeApiImpl(
    private val baseUrl: String = "https://api.sampleapis.com/coffee"
): CoffeeApi {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun hotCoffee(): List<Coffee> = httpClient.get("$baseUrl/hot").body()
}
