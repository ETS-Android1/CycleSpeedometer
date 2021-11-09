package com.example.cyclespeedometer;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;



public class Fragment1 extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private MapDraw mapDrawView;
    private String deviceAddress;
    public static BluetoothSocket mmSocket;
    public static CreateConnectThread createConnectThread;
    public static ConnectedThread connectedThread;
    public static TextView messageView;

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
        buttonButton.setOnClickListener(this::add);
        Button saveButton = getView().findViewById(R.id.save);
        saveButton.setOnClickListener(this::save);
        mapDrawView = getView().findViewById(R.id.mapDraw);
        messageView = getView().findViewById(R.id.textView);
        deviceAddress = "00:18:E4:40:00:06";
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
        createConnectThread.start();
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

    private interface GPSCallback{
        void onLocation(Location location);
    }

    private void requestCurrentLocation(GPSCallback callback){
        if (checkLocationPermission()) {
            Task<Location> currentLocationTask = fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    null
            );
            currentLocationTask.addOnCompleteListener((new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location=null;
                    if (task.isSuccessful()) {
                        location = task.getResult();
                    } else {
                        Exception exception = task.getException();
                    }
                    callback.onLocation(location);
                }
            }));
        }
    }

    private void getGPSLocation(int speed){
        requestCurrentLocation(new GPSCallback() {
            public void onLocation(Location location) {
                mapDrawView.addDataPoint(location.getLatitude(),location.getLongitude(), speed);
            }
        });
    }

    public void add(View v) {
        return;
    }

    public void save(View v) {
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








    public class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.i("Connection Failed","dhdjd");
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                } catch (IOException closeException) {
                    Log.e("ConnectionIssue", "Could not close the client socket");
                }
                return;
            }

            messageView.setText("Success Connected!\n");
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("ConnectionIssue", "Could not close the client socket");
            }
        }
    }




    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (true) {
                try {
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        int speed = Integer.parseInt(readMessage);//
                        getGPSLocation(speed);//
                        mapDrawView.invalidate();//
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
