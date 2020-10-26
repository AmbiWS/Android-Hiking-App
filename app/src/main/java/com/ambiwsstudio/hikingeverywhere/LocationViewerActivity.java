package com.ambiwsstudio.hikingeverywhere;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationViewerActivity extends FragmentActivity implements OnMapReadyCallback {

    private LatLng location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_viewer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        Intent currentIntent = getIntent();

        if (currentIntent.getStringExtra("location") != null) {

            String locStr = currentIntent.getStringExtra("location");

            double locLat = ChallengeActivity.parseLatLng("lat", locStr);
            double locLng = ChallengeActivity.parseLatLng("lng", locStr);

            location = new LatLng(locLat, locLng);

        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (location != null) {

            googleMap.addMarker(new MarkerOptions().position(location).title("Finish Location"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

        } else {

            Toast.makeText(getApplicationContext(), "Unable to find location, try again later.", Toast.LENGTH_SHORT).show();

        }

    }
}
