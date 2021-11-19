package com.example.cyclespeedometer;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentResultListener;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;

public class Fragment1 extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private MapDraw mapDrawView;
    public static TextView messageView;
    public boolean tourRunning = false;
    private double currentLatitude, currentLongitude;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    int updateCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment1_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(view.getContext());
        checkLocationPermission();
        Button buttonButton = getView().findViewById(R.id.button);
        buttonButton.setOnClickListener(this::startTour);
        Button saveButton = getView().findViewById(R.id.save);
        saveButton.setOnClickListener(this::save);
        mapDrawView = getView().findViewById(R.id.mapDraw);
        messageView = getView().findViewById(R.id.textView);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    updateCount += 1;
                }
            }
        };

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(200);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getParentFragmentManager().setFragmentResultListener("add_datapoint", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                if(!tourRunning)return;
                int speed = bundle.getInt("speed");
                getGPSLocation(speed);
            }
        });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(getActivity())
                        .setTitle("Location Access Required")
                        .setMessage("Allow access to High Accuracy Location to track Cycling Routes")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
                            }
                        })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            }
            return false;
        }
        return true;
    }

    private void startUpdates(){
        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void stopUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void getGPSLocation(int speed){
        if(updateCount <= 10)return;
        mapDrawView.addDataPoint(currentLatitude, currentLongitude, speed);
        mapDrawView.invalidate();
        messageView.setText(Integer.toString(speed) + " km/h");
    }

    public void startTour(View v) {
        if(tourRunning){
            Toast.makeText(MainActivity.getInstance(), "Tour already running", Toast.LENGTH_SHORT).show();
            return;
        }
        updateCount = 0;
        startUpdates();
        tourRunning = true;
        Toast.makeText(MainActivity.getInstance(), "Tour started", Toast.LENGTH_SHORT).show();
    }

    public void save(View v) {
        messageView.setText("");
        if(!tourRunning){
            Toast.makeText(getActivity(), "No Ongoing Route!", Toast.LENGTH_LONG).show();
            return;
        }
        tourRunning = false;
        updateCount = 0;
        stopUpdates();
        File file = new File(getActivity().getFilesDir(), "saved_routes");
        if (!file.exists()) {
            file.mkdir();
        }
        if(mapDrawView.saveRouteToStorage(file)==0) {
            Toast.makeText(getActivity(), "Route Saved :)", Toast.LENGTH_LONG).show();
            Bundle bundle = new Bundle();
            getParentFragmentManager().setFragmentResult("updateTours", bundle);
        }
        else{
            Toast.makeText(getActivity(), "Unable to save :\\", Toast.LENGTH_LONG).show();
        }
    }
}
