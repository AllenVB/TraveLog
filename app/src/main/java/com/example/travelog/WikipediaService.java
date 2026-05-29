package com.example.travelog;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Wikipedia Geosearch API — API key gerekmez, tamamen ücretsiz.
 * Base URL: https://en.wikipedia.org/
 */
public interface WikipediaService {

    @GET("w/api.php")
    Call<WikiGeoSearchResponse> getPlacesNearby(
            @Query("action")      String action,
            @Query("list")        String list,
            @Query("gsradius")    int radius,
            @Query("gscoord")     String latLon,
            @Query("gslimit")     int limit,
            @Query("gsnamespace") int namespace,
            @Query("format")      String format
    );
}
