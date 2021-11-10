package com.example.cyclespeedometer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Fragment2 extends Fragment {

    private List<String> listOfTours = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("updateTours", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                updateListOfTours();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment2_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateListOfTours();
    }

    public void updateListOfTours(){
        listOfTours.clear();
        File file = new File(getActivity().getFilesDir(), "saved_routes");
        if(file.exists()){
            String[] fileNamesList = file.list();
            for (int ii = 0; ii < fileNamesList.length; ii++) {
                listOfTours.add(fileNamesList[ii]);
            }
        }
        populateListView();
    }

    private void populateListView(){
        if(listOfTours.size() == 0){
            listOfTours.add("No Saved Tours");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(),R.layout.tour_list_item, listOfTours
        );
        ListView lst = (ListView) getActivity().findViewById(R.id.toursList);
        lst.setAdapter(adapter);
        registerClickCallbacks();
    }

    private void registerClickCallbacks(){
        ListView lst = (ListView) getActivity().findViewById(R.id.toursList);
        lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View clickedView, int pos, long id) {
                TextView textView = (TextView) clickedView;
                if(textView.getText() == "No Saved Tours"){ return; }
                PopUp popup = new PopUp();
                popup.showPopupWindow(getView(), (String) textView.getText());
            }
        });
    }
}
