package com.application.microsoft.wayfarer.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Filter;



import com.application.microsoft.wayfarer.R;
import com.application.microsoft.wayfarer.adapters.ListViewAdapter;
import com.application.microsoft.wayfarer.handlers.HttpHandler;
import com.application.microsoft.wayfarer.models.Place;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private ListView listView;
    private ListViewAdapter listAdapter;
    private String city = "";
    AutoCompleteTextView autoCompView;

    private static final String LOG_TAG = "Google Places Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyDUUBHfckNZX5kcVYv8bPXnaCaYLjxvX-8";
    String[] cities;
    ArrayList<Place> placesList;
    int index;
    String PLACES_OF_INTEREST_URL = "";

    public ArrayList<Place> getPlacesList() {
        return placesList;
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        placesList = new ArrayList<>();
        cities = getResources().getStringArray(R.array.cities_arrays);
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, cities);
        listView = (ListView) findViewById(R.id.listView);
        placesList.clear();
        autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteText);
        autoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        autoCompView.setOnItemClickListener(this);
        listAdapter = new ListViewAdapter(this, R.layout.row,placesList);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                index = arg0.getSelectedItemPosition();

                city = cities[index];
                if(index != 0) {
                    PLACES_OF_INTEREST_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + city + "+point+of+interest&language=en&key=" + API_KEY + "";
                    listView.clearAnimation();
                    listAdapter.clear();
                    new GetPlaces().execute();
                    listAdapter.addAll(placesList);
                    listView = (ListView) findViewById(R.id.listView);
                    listView.invalidateViews();
                    listAdapter.notifyDataSetChanged();
                    listView.setAdapter(listAdapter);
                    System.out.println(city);
                    Toast.makeText(getApplicationContext(), "Selected: " + city, Toast.LENGTH_LONG).show();
                }
                index = -1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Toast.makeText(getApplicationContext(),"Please Select a City!",  Toast.LENGTH_LONG).show();

            }
        });

    }

    @SuppressLint("LongLogTag")
    public static ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:ind");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
               resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    @Override
    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return (String) resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {

                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        resultList = autocomplete(constraint.toString());
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }


                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    public void plan(View v) {
        Intent intent = new Intent(MainActivity.this,PlanActivity.class);
        intent.putParcelableArrayListExtra("placesList", placesList);
        String address = autoCompView.getText().toString();
        System.out.println(address + " " + city);
        if(address != null &&  address.contains(city) ){
            Place p = new Place();
            p.setName(address);
            LatLng latLng = getLocationFromAddress(this, address);
            p.setLat(latLng.latitude);
            p.setLng(latLng.longitude);
            p.setSelected(true);
            placesList.add(0,p);
            startActivity(intent);

        }
        Toast.makeText(getApplicationContext(),"Please select a starting location !",  Toast.LENGTH_LONG).show();
    }

    public void loginIcon(View v) {
        Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(myIntent);

    }


    @SuppressLint("StaticFieldLeak")
    private class GetPlaces extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            String jsonStr = sh.makeServiceCall(PLACES_OF_INTEREST_URL);
            Log.e(TAG, "Response from PLACES_OF_INTEREST_URL: " + jsonStr);
            if (jsonStr != null) {
                try {
                    System.out.println(PLACES_OF_INTEREST_URL);
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONArray  jsonArray = jsonObj.getJSONArray("results");
                    for (int i = 0; i <  jsonArray.length(); i++) {
                        JSONObject object =  jsonArray.getJSONObject(i);
                        Place place = new Place();
                        String photoReference;
                        place.setCity(city);
                        place.setID(object.getString("place_id"));
                        String placeStr = sh.makeServiceCall("https://maps.googleapis.com/maps/api/place/details/json?placeid="+place.getID()+"&key="+API_KEY+"");
                        JSONObject jsonObj1 = new JSONObject(placeStr);
                        place.setDescription(jsonObj1.getJSONObject("result").getJSONArray("reviews").getJSONObject(0).getString("text").split("\n")[0]);
                        place.setName(object.getString("name"));
                        JSONObject geometry = object.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        place.setLat(location.getDouble("lat"));
                        place.setLng(location.getDouble("lng"));
                        JSONArray photos = object.getJSONArray("photos");
                        JSONObject photoReferenceUrl = photos.getJSONObject(0);
                        photoReference = photoReferenceUrl.getString("photo_reference");
                        String IMAGE_URL = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=1000&photoreference="+photoReference+"&sensor=false&key="+ API_KEY +"";
                        System.out.println(IMAGE_URL);
                        place.setImgURL(IMAGE_URL);
                        place.setSelected(false);
                        if (!placesList.contains(place))
                            placesList.add(place);
                    }
                    for (int i = 0; i < placesList.size(); i++){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            if(!Objects.equals(placesList.get(i).getCity(), city))
                                placesList.remove(i);
                        }
                    }
                    System.out.println("Done!!");
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                            System.out.println("Exception!!");
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                        System.out.println("Json Error!!");
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            System.out.println("PostExecute!!");
            if (pDialog.isShowing())
                pDialog.dismiss();
            listAdapter.notifyDataSetChanged();

        }
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            Address location = address.get(0);
            location.getLatitude();
            location.getLongitude();

            p1 = new LatLng(location.getLatitude(), location.getLongitude() );

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        return p1;
    }

}
