package com.example.mobileassignmentloginpart

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

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
    }
}
