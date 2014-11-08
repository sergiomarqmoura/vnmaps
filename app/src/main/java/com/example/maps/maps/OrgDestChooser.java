package com.example.maps.maps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.maps.maps.R;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.auth.AUTH;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.logging.LogRecord;


public class OrgDestChooser extends Activity {

    PlacesTask placesTask;
    ParserTask parserTask;
    PlacesDetailsTask placesDetailsTask;
    ParserDetailsTask parserDetailsTask;
    CustomAutoCompleteTextView autoCompViewFrom;
    CustomAutoCompleteTextView autoCompViewTo;
    String from;
    String to;
    boolean from_to;
    Button get_directions;
    Context context;
    String lat_from;
    String lng_from;
    String lat_to;
    String lng_to;
    String from_reference;
    String to_reference;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.org_dest_chooser);
        context=this;

        get_directions = (Button) findViewById(R.id.get_directions);
        get_directions.setEnabled(false);
        get_directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoCompViewFrom.getText().toString().length()>=3 && autoCompViewTo.getText().toString().length()>=3){


                    Intent intent = new Intent(context, MainActivity.class);
                    intent.putExtra("lat_from",lat_from);
                    intent.putExtra("lng_from",lng_from);
                    intent.putExtra("lat_to",lat_to);
                    intent.putExtra("lng_to",lng_to);
                    context.startActivity(intent);
                }
            }
        });

        autoCompViewFrom = (CustomAutoCompleteTextView) findViewById(R.id.autocompleteFrom);
        autoCompViewFrom.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
        autoCompViewFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                from_to=true;
            }
        });
        autoCompViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                from_to=true;
                String str = (String) adapterView.getItemAtPosition(position).toString();
                Log.d("OPTION CHOOSED: ",str);
                from = str.substring(str.lastIndexOf("=") + 1,str.length()-1);
                from_reference = str.substring(str.lastIndexOf("reference="));
                from_reference = from_reference.substring(0,from_reference.indexOf(","));
                Log.d("REFERENCE: ", from_reference);

                placesDetailsTask = new PlacesDetailsTask();
                placesDetailsTask.execute(from_reference);
                Log.d("FROM REFERENCE= ", from_reference);
                autoCompViewFrom.setText(from);

            }
        });

        autoCompViewFrom.setThreshold(1);
        autoCompViewFrom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                Log.d("TAG: ","placesTask about to start!");
                Log.d("String to send: ",autoCompViewFrom.getText().toString());

                if (autoCompViewFrom.getText().toString().length()<5)
                    get_directions.setEnabled(false);

                placesTask = new PlacesTask();
                placesTask.execute(autoCompViewFrom.getText().toString());


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        autoCompViewTo = (CustomAutoCompleteTextView) findViewById(R.id.autocompleteTo);
        autoCompViewTo.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
        autoCompViewTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                from_to=false;
            }
        });
        autoCompViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                from_to=false;
                String str = (String) adapterView.getItemAtPosition(position).toString();
                Log.d("OPTION CHOOSED: ",str);
                to = str.substring(str.lastIndexOf("=") + 1,str.length()-1);
                to_reference = str.substring(str.lastIndexOf("reference="));
                to_reference = to_reference.substring(0,to_reference.indexOf(","));
                Log.d("TO REFERENCE: ", to_reference);
                placesDetailsTask = new PlacesDetailsTask();
                placesDetailsTask.execute(to_reference);
                autoCompViewTo.setText(to);

            }
        });

        autoCompViewTo.setThreshold(1);
        autoCompViewTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                Log.d("TAG: ","placesTask about to start!");
                Log.d("String to send: ",autoCompViewTo.getText().toString());

                if (autoCompViewTo.getText().toString().length()<5)
                    get_directions.setEnabled(false);

                placesTask = new PlacesTask();
                placesTask.execute(autoCompViewTo.getText().toString());

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });




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

    private class PlacesTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            // Obtain browser key from https://code.google.com/apis/console
            String key = "key=AIzaSyDP3J_zhNB-wIuGooWuNOIHuPpOOG88dIQ";

            String input="";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                // Fetching the data from we service
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            Log.d("Data size: ",""+data.length());
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("RESULT: ",result);

            // Creating ParserTask
            parserTask = new ParserTask();

            // Starting Parsing the JSON string returned by Web Service
            parserTask.execute(result);
        }
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

    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            String[] from = new String[] { "description"};
            int[] to = new int[] { android.R.id.text1 };

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);

            // Setting the adapter

                autoCompViewFrom.setAdapter(adapter);
            autoCompViewTo.setAdapter(adapter);

        }
    }

    private class PlacesDetailsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            // Obtain browser key from https://code.google.com/apis/console
            String key = "key=AIzaSyDP3J_zhNB-wIuGooWuNOIHuPpOOG88dIQ";

            String reference="";


                reference = place[0];


            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=true";

            // Building the parameters to the web service
            String parameters = reference+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/details/"+output+"?"+parameters;
            Log.d("URL::::::",url);

            try{
                // Fetching the data from we service
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            Log.d("Data size: ",""+data.length());
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("RESULT: ",result);

            // Creating ParserTask
            parserDetailsTask = new ParserDetailsTask();

            // Starting Parsing the JSON string returned by Web Service
            parserDetailsTask.execute(result);
        }
    }

    private class ParserDetailsTask extends AsyncTask<String, Integer, HashMap<String,String>>{

        JSONObject jObject;

        @Override
        protected HashMap<String,String> doInBackground(String... jsonData) {

            HashMap<String, String> places = null;

            PlaceDetailsJSONParser placeDetailsJsonParser = new PlaceDetailsJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeDetailsJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }


        protected void onPostExecute(HashMap<String, String> result) {

            if(from_to) {
                lat_from = result.get("lat");
                lng_from = result.get("lng");
                Log.d("LAT_FROM: ", lat_from);
                Log.d("LNG_FROM: ", lng_from);
            } else{
                lat_to = result.get("lat");
                lng_to = result.get("lng");
                Log.d("LAT_TO: ", lat_to);
                Log.d("LNG_TO: ", lng_to);
            }

            if (autoCompViewFrom.getText().toString().length()>=5 && autoCompViewTo.getText().toString().length()>=5){
                get_directions.setEnabled(true);
            }

        }
    }


}




