package com.example.maps.maps;

/**
 * Created by macintoshhd on 26/10/14.
 */
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlaceDetailsJSONParser {

    /** Receives a JSONObject and returns a list */
    public HashMap<String,String> parse(JSONObject jObject){

        HashMap<String,String> map = new HashMap<String, String>();
        JSONObject jPlaces = null;
        String lat;
        String lng;
        try {
            Log.d("Jobject: ",jObject.toString());
            /** Retrieves all the elements in the 'places' array */
            jPlaces = jObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
            Log.d("OBJECT: ",jPlaces.toString());
            lat = jPlaces.getString("lat");
            lng = jPlaces.getString("lng");
            Log.d("LAT: ",lat);
            Log.d("LNG: ",lng);
            map.put("lat",lat);
            map.put("lng",lng);

            //Log.d("STRING LAT:",lat);
            //Log.d("STRING LNG:",lng);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        /** Invoking getPlaces with the array of json object
         * where each json object represent a place
         */
        return map;
    }

    /** Parsing the Place JSON object */

}