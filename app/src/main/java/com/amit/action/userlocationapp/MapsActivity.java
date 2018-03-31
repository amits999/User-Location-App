package com.amit.action.userlocationapp;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;

    LocationListener locationListener;
    String name = "";
    String address = "";
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mDatabase;
    ProgressDialog mProgress;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    {

                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    }

                }

            }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAuth = FirebaseAuth.getInstance();
        mProgress = new ProgressDialog(this);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (firebaseAuth.getCurrentUser() == null) {

                    startActivity(new Intent(MapsActivity.this, MainActivity.class));
                    mProgress.dismiss();
                }

            }
        };


    }


    public void userAddress(Location location, LatLng userLocation) {


        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {

            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (listAddresses != null && listAddresses.size() > 0) {

                Log.i("PlaceInfo", listAddresses.get(0).toString());


                if (listAddresses.get(0).getSubThoroughfare() != null) {

                    address += listAddresses.get(0).getSubThoroughfare() + " ";

                }

                if (listAddresses.get(0).getThoroughfare() != null) {

                    address += listAddresses.get(0).getThoroughfare() + ", ";

                }

                if (listAddresses.get(0).getLocality() != null) {

                    address += listAddresses.get(0).getLocality() + ", ";

                }

                if (listAddresses.get(0).getPostalCode() != null) {

                    address += listAddresses.get(0).getPostalCode() + ", ";

                }

                if (listAddresses.get(0).getCountryName() != null) {

                    address += listAddresses.get(0).getCountryName();

                }

                Intent intent = getIntent();
                name = intent.getStringExtra("name");

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String uid = currentUser.getUid();
                mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                HashMap<String, String> userMap = new HashMap<>();
                userMap.put("name", name);
                userMap.put("location", address);

                mDatabase.setValue(userMap);

                //Toast.makeText(MapsActivity.this, address, Toast.LENGTH_SHORT).show();
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(userLocation).title(address));

            }

        } catch (IOException e) {

            e.printStackTrace();

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.clear();

                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 8));

                userAddress(location, userLocation);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {

           // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            } else {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                mMap.clear();
                userAddress(lastKnownLocation,userLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 8));


            }


        }


    }

    public void onLogout(View view) {

        mProgress.setTitle("Getting Location");
        mProgress.setMessage("Please wait while we search !");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();
        mAuth.signOut();

    }
}
