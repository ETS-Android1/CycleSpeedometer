package com.example.cyclespeedometer;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.camera2.params.BlackLevelPattern;
import android.os.Bundle;

import com.example.cyclespeedometer.databinding.Fragment1LayoutBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.cyclespeedometer.ui.main.SectionsPagerAdapter;
import com.example.cyclespeedometer.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;
    public String deviceAddress = "";
    private ActivityMainBinding binding;
    public BluetoothSocket mmSocket = null;
    public CreateConnectThread createConnectThread;
    public ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothSelector bts = new BluetoothSelector();
                bts.showSelector(view.getRootView());
            }
        });

        //////////////////////////////////////////////////////////////////////
        DeleteThisLater dtl = new DeleteThisLater();
        dtl.start();
        //////////////////////////////////////////////////////////////////////

    }

    //////////////////////////////////////////////////////////////////////
    public class DeleteThisLater extends Thread {
        public void run(){
            int speed = 0, offset = 1;
            while(true) {
                Bundle bundle = new Bundle();
                bundle.putInt("speed", speed);
                MainActivity.getInstance().getSupportFragmentManager().setFragmentResult("add_datapoint", bundle);
                if(speed >= 30)offset = -1;
                if(speed <= 0)offset = 1;
                speed += offset;
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////////////

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onDestroy() {
        if(connectedThread != null){
            connectedThread.cancel();
        }
        super.onDestroy();
    }

    public void tryToConnect(){
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
        createConnectThread.start();
    }


    public class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                MainActivity.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText( MainActivity.getInstance(), "Failed to connect to Bluetooth device", Toast.LENGTH_LONG).show();
                    }
                });
                Log.i("Connection Failed","connection failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                MainActivity.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText( MainActivity.getInstance(), "Bluetooth connection established", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Status", "Device connected");
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                    MainActivity.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText( MainActivity.getInstance(), "Failed to connect to Bluetooth device", Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.e("Status", "Cannot connect to device");
                } catch (IOException closeException) {
                    MainActivity.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText( MainActivity.getInstance(), "Failed to connect to Bluetooth device", Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.e("ConnectionIssue", "Could not close the client socket");
                }
                return;
            }

            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }
    }


    public static class ConnectedThread extends Thread {
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
                    if(!mmSocket.isConnected()){
                        Toast.makeText(getInstance(), "Bluetooth disconnected :\\", Toast.LENGTH_LONG).show();
                        Button saveButton = (Button) getInstance().findViewById(R.id.save);
                        saveButton.performClick();
                        break;
                    }
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        int speed = Integer.parseInt(readMessage.trim());
                        Bundle bundle = new Bundle();
                        bundle.putInt("speed", speed);
                        MainActivity.getInstance().getSupportFragmentManager().setFragmentResult("add_datapoint", bundle);
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
            }
            catch (IOException e) { }
        }
    }

}