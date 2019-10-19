package com.example.myfirstapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVWriter;
import com.opencsv.bean.OpencsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Button startBtn, stopBtn, viewMap, saveFile;
    private EditText fileName;
    private LinearLayout fileNameLayout;
    private TextView latView, lngView, speedView;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private double latitude, longitude, speed;
    private boolean isThreadRunning;
    private LatLngBounds.Builder builder;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = (Button) findViewById(R.id.buttonStart);
        stopBtn = (Button) findViewById(R.id.buttonStop);
        latView = (TextView) findViewById(R.id.lat);
        lngView = (TextView) findViewById(R.id.lng);
        speedView = (TextView) findViewById(R.id.speed);
        viewMap = (Button) findViewById(R.id.viewMap);
        saveFile = (Button) findViewById(R.id.saveFile);
        fileName = (EditText) findViewById(R.id.fileName);
        fileNameLayout = (LinearLayout) findViewById(R.id.fileNameLayout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        builder = new LatLngBounds.Builder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 10);
                return;
            }
        }
        else{
            configureButton();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        LatLng sydney = new LatLng(37.7750, 122.4183);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("inside", "reqpermission");
        switch (requestCode){
            case 10:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("Permissions", "Granted");
                    configureButton();
                }
                return;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
//                Toast.makeText(getApplicationContext(),"Got activity 10", Toast.LENGTH_SHORT).show();
                Log.i("on activity result", "df");

                mMap.clear();

                Uri uri = data.getData();
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

                    for (String line; (line = r.readLine()) != null; ) {
                        String[] str = line.trim().split(",");
                        String latitude = str[0].substring(1, str[0].length() - 2);
                        String longitude = str[1].substring(1, str[1].length() - 2);
//                        String latitude = str[0];
//                        String longitude = str[1];
                        Log.i("Rendering row", latitude + " - " + longitude);
//                        Toast.makeText(getApplicationContext(),"Rendering data " + latitude, Toast.LENGTH_SHORT).show();

                        double lat = Double.parseDouble(latitude);
                        double lng = Double.parseDouble(longitude);
                        Log.i("row value", lat + " - " + lng);

                        LatLng latLng = new LatLng(lat, lng);
                        builder.include(latLng);
                        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker in Chandigarh"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    }

                    LatLngBounds bounds = builder.build();
                    int padding = 0;
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cameraUpdate);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void fetchAndDisplayLocation(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                Toast.makeText(getApplicationContext(),"Location Changing", Toast.LENGTH_SHORT).show();
                Log.i("Location", "Changed");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                speed = location.getSpeed();
                latView.setText("Latitude: " + latitude);
                lngView.setText("Longitude: " + longitude);
                speedView.setText("Speed: " + speed);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.i("Location Status Change ", s);
//                Toast.makeText(getApplicationContext(),s, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i("Loc Provider enabled", s);
//                Toast.makeText(getApplicationContext(),s, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String s) { //ask user if permissions not enabled
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void configureButton(){
        fetchAndDisplayLocation();
        stopBtn.setEnabled(false);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        Log.i("button", "clicks enabled");
//        Toast.makeText(getApplicationContext(),"Reached Configure btn", Toast.LENGTH_SHORT).show();

        viewMap.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
//                Toast.makeText(getApplicationContext(),"View Files", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");

                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try{
                    startActivityForResult(intent, 10);
                } catch (Exception e) {
                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(),"error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fileNameLayout.getVisibility() == View.VISIBLE){
                    startBtn.setText("Start");
                    fileNameLayout.setVisibility(View.GONE);
                }
                else{
                    startBtn.setText("Cancel");
                    fileNameLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        saveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("click", "save");
                String csvFileName = fileName.getText().toString();
                if (csvFileName.matches("")) {
                    Toast.makeText(getApplicationContext(), "You did not enter a File Name", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    fileNameLayout.setVisibility(View.GONE);
                }
                String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                Toast.makeText(getApplicationContext(),"Started", Toast.LENGTH_SHORT).show();
                String fileName = csvFileName + ".csv";
                String filePath = baseDir + File.separator  +  fileName;
                final File file = new File(filePath);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isThreadRunning = true;
//                        FileWriter fileWriter = null;
                        try {
                            FileWriter fileWriter = new FileWriter(file);
                            CSVWriter writer = new CSVWriter(fileWriter);
                            while(isThreadRunning) {
                                String[] data = {Double.toString(latitude), Double.toString(longitude), Double.toString(speed)};
                                writer.writeNext(data);

                                Thread.sleep(1000);
                            }
                            Log.i("task", "running");
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("exception", e.getMessage());
                        }
                        Log.i("task", "finished");
                    }
                }).start();
                startBtn.setText("Start");
                stopBtn.setEnabled(true);
                startBtn.setEnabled(false);
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
                Log.i("task", "stopped");
                isThreadRunning = false;
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
//        locationManager.removeUpdates(locationListener);
        Log.i("on", "pause");
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        Log.i("on", "resume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
        Log.i("on", "destroy");
    }
}
