package com.example.googlemapdemoproject.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemapdemoproject.R;
import com.example.googlemapdemoproject.bean.ModelClass;
import com.example.googlemapdemoproject.connectivity.CheckNetwork;
import com.example.googlemapdemoproject.jsonparse.DataParser;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.googlemapdemoproject.R.id.map;

/**
 * Created by sarabjjeet on 9/13/17.
 */

public class MapFagment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private TextView txt_distance;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private List<ModelClass> markerList = new ArrayList<ModelClass>();
    private ProgressDialog progressBar;
    private int position;
    private double distance = 0.00;
    private boolean isAdded = false;
    private boolean isRemoved = false;
    private ArrayList<Double> distanceArray = new ArrayList<>();
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int MY_CHECK_LOCATION = 100;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_fragment, container, false);

        markerList.clear(); //clear the markerList for everytime app runs

        initview(view);

        return view;
    }

    /**
     * @param view initialise the view
     */
    private void initview(View view) {
        txt_distance = (TextView) view.findViewById(R.id.txt_distance);

        final SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (CheckNetwork.isNetworkAvailable(this.getActivity())) {
                checkLocationPermission();
            } else {
                CheckNetwork.showAlert("Internet Connectivity Failure", getActivity());
            }

        }
        progressBar = new ProgressDialog(this.getActivity());
        progressBar.setCancelable(false);//you can cancel it by pressing back button
        progressBar.setMessage("wait ...");
        //displays the progress bar

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        zoomCurrentLocation();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this.getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);


            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Setting onclick event listener for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                //  MarkerPoints.add(point);
                MarkerOptions options = new MarkerOptions();
                options.position(point);
//                mMap.addMarker(options);


                Marker marker = mMap.addMarker(options);
                Log.e("marker", marker + "");
                markerList.add(new ModelClass(marker, null));


                try {
                    if (markerList.size() > 1) {
                        Marker marker_origin = markerList.get(markerList.size() - 2).getMarker();
                        LatLng origin = marker_origin.getPosition();
                        Marker marker_destination = markerList.get(markerList.size() - 1).getMarker();
                        LatLng dest = marker_destination.getPosition();
                        String url = getUrl(origin, dest);
                        FetchUrl FetchUrl = new FetchUrl();
                        FetchUrl.execute(url);
                        isAdded = true;

                    }
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
        });

    }

    private String getUrl(LatLng origin, LatLng dest) {


        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }


    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
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
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.e("Exception", "downloadurl");
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    /**
     * click on marker to remove
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.e("marker clicked", marker.getId());


        for (int i = 0; i < markerList.size(); i++) {
            Log.e("marker from list", markerList.get(i).getMarker().getId());
        }
        for (int i = 0; i < markerList.size(); i++) {
            if (markerList.get(i).getMarker().getId().equals(marker.getId())) {
                position = i;
                for (int j = i; j < markerList.size(); j++) {

                    markerList.get(j).getMarker().remove();

                    if (markerList.get(j).getPolyline() != null) {
                        markerList.get(j).getPolyline().remove();
                    } else {
                    }
                }
                marker.remove();
                isAdded = false;
                isRemoved = true;
                calculateDistance(markerList);

            }
            marker.remove();

        }

        for (int k = position; k < markerList.size(); k++) {
            markerList.subList(k, markerList.size()).clear();
        }
        for (int i = 0; i < markerList.size(); i++) {
            Log.e(" after marker from list", markerList.get(i).getMarker().getId() + "");
        }
        return true;

    }


    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressBar.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);


            } catch (Exception e) {
                Log.e("exception", "fetchURL");
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.contains("error_message") || result.contains("ZERO_RESULTS")) {

                Toast.makeText(getActivity(), "No Route Found", Toast.LENGTH_SHORT).show();
                progressBar.dismiss();
                markerList.get(markerList.size() - 1).getMarker().remove();
                markerList.subList(markerList.size() - 1, markerList.size()).clear();
            }

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }


    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DataParser parser = new DataParser();

                // Starts parsing data
                routes = parser.parse(jObject);


            } catch (Exception e) {
                Log.e("exception", "parser task");
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;


            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);


                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                try {
                    Polyline polyline = mMap.addPolyline(lineOptions);
                    markerList.get(markerList.size() - 1).setPolyline(polyline);
                    calculateDistance(markerList);
                    progressBar.dismiss();

                } catch (Exception exp) {
                    Log.e("no", "polyline");
                    exp.printStackTrace();
                }

            }
        }
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        settingsrequest();

    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);

            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this.getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this.getActivity(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    /**
     * method to check location (enabled or disabled)
     */
    private void settingsrequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), MY_CHECK_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case MY_CHECK_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        zoomCurrentLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        settingsrequest();//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }


    /**
     * method to zoom camera to current location
     */
    private void zoomCurrentLocation() {

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.getActivity());
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this.getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location == null) {
                        Log.e("location", "null");
                    } else {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        LatLng currentPsition = new LatLng(lat, lng);

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPsition));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                    }

                }
            });
        }
    }


    /**
     * method to calculate distance between points
     *
     * @param markerList
     */
    private void calculateDistance(List<ModelClass> markerList) {
        try {
            if (markerList.size() > 1) {
                int Radius = 6371;

                Marker marker_origin = markerList.get(markerList.size() - 2).getMarker();
                LatLng origin = marker_origin.getPosition();
                double origin_lat = origin.latitude;
                double origin_lng = origin.longitude;
                Marker marker_destination = markerList.get(markerList.size() - 1).getMarker();
                LatLng dest = marker_destination.getPosition();
                double dest_lat = dest.latitude;
                double dest_lng = dest.longitude;


                double dLat = Math.toRadians(dest_lat - origin_lat);
                double dLon = Math.toRadians(dest_lng - origin_lng);

                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(origin_lat))
                        * Math.cos(Math.toRadians(dest_lat)) * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);
                double c = 2 * Math.asin(Math.sqrt(a));
                double valueResult = Radius * c;
                double km = valueResult / 1;
                Log.e("km", km + "");
                ModelClass modelClass = new ModelClass();

                if (isAdded) {
                    distanceArray.add(km);
                    distance = distance + km;
                    modelClass.setDistance(distance);
                    txt_distance.setText(new DecimalFormat("##.##").format(Math.abs(modelClass.getDistance())) + " " + "KM");
                } else if (isRemoved) {
                    if (position == 0) {
                        for (int i = position; i < distanceArray.size(); i++) {
                            distance = distance - distanceArray.get(i);
                        }
                        distanceArray.subList(position, distanceArray.size()).clear();

                    } else {
                        for (int i = position - 1; i < distanceArray.size(); i++) {
                            distance = distance - distanceArray.get(i);
                        }
                        distanceArray.subList(position - 1, distanceArray.size()).clear();
                    }
                    txt_distance.setText(new DecimalFormat("##.##").format(Math.abs(distance)) + " " + "KM");
                }
            }
        } catch (Exception exp) {
            Log.e("distance ", "exception");
            exp.printStackTrace();
        }
    }
}
