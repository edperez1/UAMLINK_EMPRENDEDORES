package ni.edu.uam.uamlink.core.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth // Importación limpia y oficial de la V3
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object SupabaseNetwork {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://wetwonwwsfhhnoqzsfsx.supabase.co",
        supabaseKey = "sb_publishable_qsqg2F0tGUeHXinS-WrBbA_YYb3TYsm"
    ) {
        install(Auth) // Usamos Auth nuevamente
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}