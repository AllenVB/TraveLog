package com.example.travelog;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {

    @SerializedName("main")
    public Main main;

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("name")
    public String cityName;

    @SerializedName("coord")
    public Coord coord;

    public static class Coord {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;
    }

    public static class Main {
        @SerializedName("temp")
        public float temp;

        @SerializedName("feels_like")
        public float feelsLike;

        @SerializedName("humidity")
        public int humidity;
    }

    public static class Weather {
        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }
}
