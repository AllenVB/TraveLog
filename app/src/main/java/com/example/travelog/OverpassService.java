package com.example.travelog;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * OpenStreetMap Overpass API — API key gerektirmez.
 * Bir koordinat çevresindeki turistik/tarihi yerleri (etiketli POI) döndürür.
 */
public interface OverpassService {

    @FormUrlEncoded
    @POST("api/interpreter")
    Call<OverpassResponse> query(@Field("data") String data);
}
