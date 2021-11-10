package com.example.cyclespeedometer;

import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import java.io.File;

public class PopUp {

    public void showPopupWindow(final View view, String fileName) {

        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        MapDraw mapDraw = (MapDraw) popupView.findViewById(R.id.popup_mapdraw);
        if(mapDraw.loadRouteFromStorage(fileName) == -1) {
            popupWindow.dismiss();
            return;
        }
        mapDraw.invalidate();
        mapDraw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        ConstraintLayout popupCard = popupView.findViewById(R.id.popup_card);
        popupCard.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

        Button shareCSV = popupView.findViewById(R.id.share_csv);
        shareCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                File gpxfile = new File(view.getContext().getFilesDir(), "saved_routes");
                File fileWithinMyDir = new File(gpxfile, fileName);

                if(fileWithinMyDir.exists()) {
                    Uri fileUri = FileProvider.getUriForFile(view.getContext(), BuildConfig.APPLICATION_ID + ".provider", fileWithinMyDir);
                    intentShareFile.setType("text/plain");
                    intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    view.getContext().startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
            }
        });

    }

}