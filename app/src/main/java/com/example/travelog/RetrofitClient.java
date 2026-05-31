package com.example.travelog;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    /**
     * Wikipedia ve OpenTripMap, kimliksiz (User-Agent'sız) istekleri engelliyor.
     * Bu interceptor her isteğe açıklayıcı bir User-Agent ekler.
     */
    private static final String USER_AGENT =
            "TraveLog/1.0 (Android; https://github.com/AllenVB/TraveLog)";

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()))
                .build();
    }

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

    // ── OpenTripMap (turizm odaklı — popülerliğe göre sıralı) ──────────────────
    private static Retrofit openTripMapRetrofit;
    private static final String OTM_BASE_URL = "https://api.opentripmap.com/";

    public static Retrofit getOpenTripMapInstance() {
        if (openTripMapRetrofit == null) {
            openTripMapRetrofit = new Retrofit.Builder()
                    .baseUrl(OTM_BASE_URL)
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return openTripMapRetrofit;
    }

    // ── OpenStreetMap Overpass (turistik/tarihi POI — API key gerekmez) ────────
    private static Retrofit overpassRetrofit;
    private static final String OVERPASS_BASE_URL = "https://overpass-api.de/";

    public static Retrofit getOverpassInstance() {
        if (overpassRetrofit == null) {
            overpassRetrofit = new Retrofit.Builder()
                    .baseUrl(OVERPASS_BASE_URL)
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return overpassRetrofit;
    }

    // ── Wikipedia Geosearch (yedek kaynak — API key gerekmez) ──────────────────
    private static Retrofit wikipediaRetrofit;
    private static final String WIKI_BASE_URL = "https://en.wikipedia.org/";

    public static Retrofit getWikipediaInstance() {
        if (wikipediaRetrofit == null) {
            wikipediaRetrofit = new Retrofit.Builder()
                    .baseUrl(WIKI_BASE_URL)
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return wikipediaRetrofit;
    }
}
