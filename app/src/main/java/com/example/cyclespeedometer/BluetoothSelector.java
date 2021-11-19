package com.example.cyclespeedometer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothSelector {

    private PopupWindow popupWindow;
    private BluetoothAdapter bluetoothAdapter;
    private ListView btList;
    private List<String> devices = new ArrayList<>();
    private List<String> deviceMacs = new ArrayList<>();

    public void showSelector(final View view){
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
        View btsView = inflater.inflate(R.layout.bts_layout, null);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            MainActivity.getInstance().someActivityResultLauncher.launch(enableBtIntent);
            return;
        }

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        popupWindow = new PopupWindow(btsView, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        btList = (ListView) btsView.findViewById(R.id.deviceList);

        btsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                popupWindow.dismiss();
                return true;
            }
        });

        populateListView(view);
    }

    private void populateListView(View view){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for(BluetoothDevice btd: pairedDevices){
            devices.add(btd.getName());
            deviceMacs.add(btd.getAddress());
        }
        if(devices.size() == 0){
            devices.add("No Paired Devices!");
            deviceMacs.add("");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                view.getContext(), R.layout.tour_list_item, devices
        );
        btList.setAdapter(adapter);
        registerClickCallbacks(view);
    }

    private void registerClickCallbacks(View view){
        btList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View clickedView, int pos, long id) {
                if(devices.get(pos) == "No Paired Devices!")return;
                Log.i("deviceAddress", MainActivity.getInstance().deviceAddress);
                MainActivity.getInstance().deviceAddress = deviceMacs.get(pos);
                MainActivity.getInstance().tryToConnect();
                popupWindow.dismiss();
            }
        });
    }

}
