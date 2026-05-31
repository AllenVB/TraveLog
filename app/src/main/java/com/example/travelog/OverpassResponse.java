package com.example.travelog;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * OpenStreetMap Overpass API yanıtı.
 * Turizm/tarihi POI'leri (etiketli gerçek gezilecek yerler) döndürür.
 */
public class OverpassResponse {

    public List<Element> elements;

    public static class Element {
        public Tags tags;
    }

    public static class Tags {
        public String name;

        @SerializedName("name:tr")
        public String nameTr;

        @SerializedName("name:en")
        public String nameEn;

        public String tourism;
        public String historic;

        // Notable (Wikipedia/Wikidata kaydı olan) yerler daha ünlüdür
        public String wikipedia;
        public String wikidata;
    }
}
