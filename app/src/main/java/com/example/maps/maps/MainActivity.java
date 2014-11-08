package com.example.maps.maps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity{


    private static final String API_KEY = "@string/google_maps_key";
    private Button load_directions;
    Context context;
    ArrayList<LatLng> markerPoints;
    LatLng origin,dest;
    private int stepCounter;
    String from;
    String to;
    MapFragment map;
    private GoogleMap googleMap;
    List<LatLng> undesiredPoints;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        undesiredPoints=new ArrayList<LatLng>();

        context=this;

        String lat_from = getIntent().getStringExtra("lat_from");
        String lng_from = getIntent().getStringExtra("lng_from");
        String lat_to = getIntent().getStringExtra("lat_to");
        String lng_to = getIntent().getStringExtra("lng_to");

        Log.d("LAT_FROM: ", lat_from);
        Log.d("LNG_FROM: ", lng_from);
        Log.d("LAT_TO: ", lat_to);
        Log.d("LNG_TO: ", lng_to);

        googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();

        CameraUpdate center=
                CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(lat_from),Double.parseDouble(lng_from)));
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);

        googleMap.moveCamera(center);
        googleMap.animateCamera(zoom);


        origin = new LatLng(Double.parseDouble(lat_from),Double.parseDouble(lng_from));
        dest=new LatLng(Double.parseDouble(lat_to), Double.parseDouble(lng_to));

        load_directions = (Button) findViewById(R.id.load_directions);
        load_directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stepCounter = 0;
                String url = getDirectionsUrl(origin, dest);
                Log.d("DIR URL", url);
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
            }
        });


    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){
// Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
// Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
// Sensor enabled
        String sensor = "sensor=true";
        String alternatives = "alternatives=true";
        String key = "key="+API_KEY;
// Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+alternatives;
// Output format
        String output = "json";
// Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;
        return url;
    }



    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);
// Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
// Connecting to url
            urlConnection.connect();
// Reading data from url
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String>{
// Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {
// For storing data from web service
            String data = "";
            try{
// Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }
// Executes in UI thread, after the execution of
// doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("RESULT", result);
            ParserTask parserTask = new ParserTask();
// Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
// Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }
        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            PolylineOptions undesiredOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            Log.d("NUMERO DE ROTAS: ",""+result.size());
// Traversing through all the routes
            for(int i=0;i<result.size();i++){
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                undesiredOptions = new PolylineOptions();
// Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
// Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

// Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(5);
                lineOptions.color(color);
                googleMap.addPolyline(lineOptions);

                LatLng point1 = new LatLng(38.71936,-9.2385);
                LatLng point2 = new LatLng(38.71934,-9.238470000000001);
                LatLng point3 = new LatLng(38.71916,-9.23807);
                LatLng point4 = new LatLng(38.71913,-9.238010000000001);

                undesiredPoints.add(point1);
                undesiredPoints.add(point2);
                undesiredPoints.add(point3);
                undesiredPoints.add(point4);

// Adding all undesired options
                if(undesiredPoints.size()>0){
                    undesiredOptions.addAll(undesiredPoints);
                    undesiredOptions.width(15);
                    undesiredOptions.color(Color.RED);
                    googleMap.addPolyline(undesiredOptions);
                }
            }

// Drawing polyline in the Google Map for the i-th route



            //Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
              //      Uri.parse("http://maps.google.com/maps?saddr="+origin.latitude+","+origin.longitude+"&daddr="+dest.latitude+","+dest.longitude));
            //startActivity(intent);
        }
    }

    public class DirectionsJSONParser {
        /**
         * Receives a JSONObject and returns a list of lists containing latitude and longitude
         */
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;
            try {
                jRoutes = jObject.getJSONArray("routes");
/** Traversing all routes */
                for (int i = 0; i < jRoutes.length(); i++) {
                    Log.d("Route " + i, ((JSONObject) jRoutes.get(i)).get("summary").toString());

                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<HashMap<String, String>>();
/** Traversing all legs */
                    for (int j = 0; j < jLegs.length(); j++) {
                        Log.d("Leg " + j + " of Route "+i, ((JSONObject) jRoutes.get(i)).get("summary").toString());
                        Log.d("Distance", ((JSONObject) ((JSONObject) jLegs.get(j)).get("distance")).get("text").toString());
                        Log.d("Duration", ((JSONObject) ((JSONObject) jLegs.get(j)).get("duration")).get("text").toString());
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
/** Traversing all steps */
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            if ((boolean) (((JSONObject) jSteps.get(k)).has("html_instructions"))) {
                                String html = (String) (((JSONObject) jSteps.get(k)).get("html_instructions"));
                                html = html.replaceAll("\\<.*?>", "");
                                Log.d("html_instructions", html);
                            } else {
                                Log.d("SIDE", "NO Side");
                            }
                            if ((boolean) (((JSONObject) jSteps.get(k)).has("maneuver"))) {
                                String side = (String) (((JSONObject) jSteps.get(k)).get("maneuver"));
                                Log.d("SIDE", side);
                            } else {
                                Log.d("SIDE", "NO Side");
                            }

                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = PolyUtil.decode(polyline);
                            for(int z=0;z<list.size();z++){
                                stepCounter++;
                                Log.d("Point "+z+" of Step "+k+" of Leg "+j+" of Route "+i,"("+list.get(z).latitude+","+list.get(z).longitude+")");
                            }
/** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
                Log.d("TOTAL COORDINATES: ",""+stepCounter);
                Log.d("MÉDIA DE COORDENADAS/KM: ", "41,3 coord/km");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }
            return routes;
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}

