package vn.hcmute.busbooking.api

import retrofit2.http.GET

data class MapsKeyResponse(
    val maps_api_key: String?
)

interface MapsConfigService {
    @GET("/api/config/maps-key")
    suspend fun getMapsKey(): MapsKeyResponse
}
