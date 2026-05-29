package com.example.travelog;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // ── OpenWeatherMap ────────────────────────────────────────────────────────
    private static Retrofit weatherRetrofit;
    private static final String WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/";

    public static Retrofit getRetrofitInstance() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl(WEATHER_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherRetrofit;
    }

    // ── Wikipedia Geosearch (API key gerekmez) ────────────────────────────────
    private static Retrofit wikipediaRetrofit;
    private static final String WIKI_BASE_URL = "https://en.wikipedia.org/";

    public static Retrofit getWikipediaInstance() {
        if (wikipediaRetrofit == null) {
            wikipediaRetrofit = new Retrofit.Builder()
                    .baseUrl(WIKI_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return wikipediaRetrofit;
    }
}
