package com.example.cyclespeedometer;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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
