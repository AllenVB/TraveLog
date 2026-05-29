package com.example.travelog;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * OpenTripMap — turizm odaklı, popülerliğe (rate) göre sıralı yer servisi.
 * Base URL: https://api.opentripmap.com/
 *
 * "radius" endpoint'i bir koordinat etrafındaki ilgi çekici yerleri döndürür.
 * rate=2..3 → yalnızca daha ünlü/önemli yerler.
 */
public interface OpenTripMapService {

    @GET("0.1/en/places/radius")
    Call<List<OtmPlace>> getPlaces(
            @Query("radius") int radius,
            @Query("lon")    double lon,
            @Query("lat")    double lat,
            @Query("rate")   String rate,
            @Query("kinds")  String kinds,
            @Query("limit")  int limit,
            @Query("format") String format,
            @Query("apikey") String apiKey
    );
}
