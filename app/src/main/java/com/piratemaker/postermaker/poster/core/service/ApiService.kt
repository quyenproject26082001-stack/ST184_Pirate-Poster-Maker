package com.piratemaker.postermaker.poster.core.service
import com.piratemaker.postermaker.poster.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("/api/ST181_HalloweenMaker")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}