package com.example.googlemapdemoproject.bean;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

/**
 * Created by sarabjjeet on 9/13/17.
 */

public class ModelClass {
    Marker marker;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    Polyline polyline;
    double distance;
    public ModelClass(Marker marker, Polyline polyline) {
        this.marker = marker;
        this.polyline = polyline;
    }

    public ModelClass() {
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }
}
