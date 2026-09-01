package com.example.foodieheal

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://gamlgwljbicaanomazvz.supabase.co",
        supabaseKey = "sb_publishable_NHNRwxkgEzo4V-dgDokkKw_WbrTkrQr"
    ) {
        install(Auth) {
            scheme = "foodieheal"
            host = "reset"
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }

    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }
}
