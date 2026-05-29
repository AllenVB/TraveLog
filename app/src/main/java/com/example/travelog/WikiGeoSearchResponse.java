package com.example.travelog;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Wikipedia Geosearch API cevap modeli
 * Endpoint: /w/api.php?action=query&list=geosearch&gscoord={lat}|{lon}&gsradius=...
 */
public class WikiGeoSearchResponse {

    @SerializedName("query")
    public QueryResult query;

    public static class QueryResult {
        @SerializedName("geosearch")
        public List<GeoPlace> geosearch;
    }

    public static class GeoPlace {
        @SerializedName("pageid")
        public int pageid;

        @SerializedName("title")
        public String title;

        /** Koordinat merkezine uzaklık (metre) */
        @SerializedName("dist")
        public double dist;
    }
}
