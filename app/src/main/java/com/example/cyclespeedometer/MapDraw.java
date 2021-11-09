package com.example.cyclespeedometer;

import static java.lang.Math.min;
import static java.lang.Math.max;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;

public class MapDraw extends View {

    private Paint paint;
    private List<Double> x, y, z, x_true, y_true, z_true;
    private List<Integer> colorCodesList;
    private double x_base,y_base;
    private final int SCALE=5000000;

    private void initVars() {
        paint = new Paint();
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        x_true = new ArrayList<>();
        y_true = new ArrayList<>();
        z_true = new ArrayList<>();
        colorCodesList = new ArrayList<Integer>(){{
            add(Color.rgb(255, 0, 0));
            add(Color.rgb(255, 17, 0));
            add(Color.rgb(255, 35, 0));
            add(Color.rgb(255, 52, 0));
            add(Color.rgb(255, 70, 0));
            add(Color.rgb(255, 87, 0));
            add(Color.rgb(255, 105, 0));
            add(Color.rgb(255, 123, 0));
            add(Color.rgb(255, 140, 0));
            add(Color.rgb(255, 158, 0));
            add(Color.rgb(255, 175, 0));
            add(Color.rgb(255, 193, 0));
            add(Color.rgb(255, 211, 0));
            add(Color.rgb(255, 228, 0));
            add(Color.rgb(255, 246, 0));
            add(Color.rgb(247, 255, 0));
            add(Color.rgb(229, 255, 0));
            add(Color.rgb(212, 255, 0));
            add(Color.rgb(194, 255, 0));
            add(Color.rgb(176, 255, 0));
            add(Color.rgb(159, 255, 0));
            add(Color.rgb(141, 255, 0));
            add(Color.rgb(124, 255, 0));
            add(Color.rgb(106, 255, 0));
            add(Color.rgb(88, 255, 0));
            add(Color.rgb(71, 255, 0));
            add(Color.rgb(53, 255, 0));
            add(Color.rgb(36, 255, 0));
            add(Color.rgb(18, 255, 0));
            add(Color.rgb(0, 255, 0));
        }};
    }

    public MapDraw(Context context) {
        super(context);
        initVars();
    }

    public MapDraw(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initVars();
    }

    public int saveRouteToStorage(File file){
        try {
            Date currentTime = Calendar.getInstance().getTime();
            String[] parts = currentTime.toString().split(" ");
            String fileName = parts[2]+"-"+parts[1]+"-"+parts[5]+" "+parts[3];
            File gpxfile = new File(file, fileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append("latitude,longitude,speed(kmph)\n");
            for(int i = 0; i< min(x_true.size(),z_true.size()); i++) {
                writer.append(Double.toString(x_true.get(i))+","+Double.toString(y_true.get(i))+","+Double.toString(z_true.get(i))+"\n");
            }
            writer.flush();
            writer.close();
            x = new ArrayList<>();
            y = new ArrayList<>();
            z = new ArrayList<>();
            x_true = new ArrayList<>();
            y_true = new ArrayList<>();
            z_true = new ArrayList<>();
            this.invalidate();
            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public int loadRouteFromStorage(String fileName){
        try {
            x = new ArrayList<>();
            y = new ArrayList<>();
            z = new ArrayList<>();
            x_true = new ArrayList<>();
            y_true = new ArrayList<>();
            z_true = new ArrayList<>();
            File gpxfile = new File(getContext().getFilesDir(), "saved_routes");
            File readFile = new File(gpxfile, fileName);
            BufferedReader reader = new BufferedReader(new FileReader(readFile));
            String line;
            reader.readLine();
            Double minLat = 1000.0, maxLat = -1000.0, minLong = 1000.0, maxLong = -1000.0;
            while((line = reader.readLine()) != null){
                String[] parts = line.split(",");
                Double lat = Double.parseDouble(parts[0]), long_ = Double.parseDouble(parts[1]);
                x_true.add(lat);
                y_true.add(long_);
                z.add(Double.parseDouble(parts[2]));
                z_true.add(Double.parseDouble(parts[2]));
                minLat = min(minLat, lat);
                maxLat = max(maxLat, lat);
                minLong = min(minLong, long_);
                maxLong = max(maxLong, long_);
            }
            Double diff = max(maxLat - minLat, maxLong - minLong);
            x_base = x_true.get(0);
            y_base = y_true.get(0);
            int scale = (int) (400 / diff);
            for(int ii = 0; ii < x_true.size(); ii++){
                x.add(scale * (x_true.get(ii) - x_base));
                y.add(scale * (y_true.get(ii) - y_base));
            }
            return 0;
        }
        catch (Exception e){
            return -1;
        }
    }

    public void setBaseDataPoint(double latitude, double longitude){
        x_base = longitude;
        y_base = latitude;
    }

    public void addDataPoint(double latitude, double longitude){
        addDataPoint(latitude,longitude,0, SCALE);
    }

    public void addDataPoint(double latitude, double longitude, double speed){
        addDataPoint(latitude,longitude,speed,SCALE);
    }

    public void addDataPoint(double latitude, double longitude, double speed, int scale){
        if(x.size()==0){
            setBaseDataPoint(latitude,longitude);
        }
        x.add(scale*(longitude-x_base));
        y.add(scale*(latitude-y_base));
        z.add(speed);
        x_true.add(longitude);
        y_true.add(latitude);
        z_true.add(speed);
    }

    private void initPaint(int color, int strokeWidth){
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
    }

    private int getColorCode(double speed){
        int c = min(colorCodesList.size() - 1, (int)Math.round(speed));
        return colorCodesList.get(c);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = (int) canvas.getWidth()/2;
        int height = (int) canvas.getWidth()/2;
        for(int i = 1; i < x.size(); i++) {
            initPaint(getColorCode(z.get(i)), 10);
            canvas.drawLine( height + x.get(i).floatValue(), width - y.get(i).floatValue(), height + x.get(i-1).floatValue(), width - y.get(i-1).floatValue(), paint);
        }
    }
}
