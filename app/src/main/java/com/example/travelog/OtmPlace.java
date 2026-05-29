package com.example.travelog;

import com.google.gson.annotations.SerializedName;

/**
 * OpenTripMap "radius" yanıtındaki tek bir yer.
 * format=json → düz liste döner (GeoJSON değil).
 */
public class OtmPlace {

    @SerializedName("xid")   public String xid;
    @SerializedName("name")  public String name;
    @SerializedName("rate")  public int rate;
    @SerializedName("kinds") public String kinds;
    @SerializedName("dist")  public double dist;
}
