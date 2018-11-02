package nl.geodan.steward;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private Intent mServiceIntent;
    private SensorService mSensorService;
    private Context ctx;

    private EditText txtStewardName;
    private TextView txtDeviceId;
    private EditText txtBroker;
    private TextView txtIp;
    private TextView txtConnected;
    private TextView txtSendLocations;
    private TextView txtLocation;
    private TextView txtAccuracy;
    private TextView txtHeading;
    private TextView txtSliderValue;

    private Timer updateTimer = new Timer();
    private Intent service;
    HashMap<Integer, String> incidentTypes = new HashMap<Integer, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {}

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        } else {}

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            LayoutInflater inflater = ((Activity)ctx).getLayoutInflater();
            View rootView = inflater.inflate(R.layout.incident,null,false);

            final View testView = view;
            final EditText description = rootView.findViewById(R.id.txtIncidentDescriptionValue);
            final Spinner incidentType = rootView.findViewById(R.id.spinnerIncidentType);

            builder.setView(rootView);
            builder.setTitle("New Incident");
            builder.setNegativeButton("Cancel", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                }
            });

            builder.setPositiveButton("Send", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                String result = mSensorService.publishIncident(getUUID(), getStewardName(), incidentTypeToNr(incidentType.getSelectedItem().toString()), description.getText().toString());
                if(result == null){
                    result = "Incident created";
                }

                Snackbar.make(testView, result, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                dialog.dismiss();
                }
            });

            builder.show();
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = sharedPref.edit();

        txtStewardName = findViewById(R.id.txtViewStewardNameValue);
        txtDeviceId = findViewById(R.id.txtDeviceIdValue);
        txtBroker = findViewById(R.id.txtViewBrokerValue);
        txtIp = findViewById(R.id.txtViewIpValue);

        txtConnected = findViewById(R.id.txtHasInternetValue);
        txtSendLocations = findViewById(R.id.txtSendLocationsValue);
        txtLocation = findViewById(R.id.txtLocationValue);
        txtAccuracy = findViewById(R.id.txtAccuracyValue);
        txtHeading = findViewById(R.id.txtHeadingValue);

        //service = new Intent(ctx, SensorService.class);
        //ctx.startService(service);
        mSensorService = new SensorService(ctx);
        mServiceIntent = new Intent(ctx, mSensorService.getClass());
        startService(mServiceIntent);

        txtDeviceId.setText(getUUID());
        txtStewardName.setText(getStewardName());
        txtStewardName.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setStewardName(s.toString());
            }
        });

        txtBroker.setText(getBroker());
        txtBroker.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setBroker(s.toString());
            }
        });

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBar.setProgress(getUpdateFrequency());
        int progress = seekBar.getProgress();
        txtSliderValue = findViewById(R.id.txtSliderValue);
        txtSliderValue.setText("Update frequency: " + progress + "ms");

        startTimer();
    }

    private String incidentTypeToNr(String incident){
        switch(incident)
        {
            case "transport":
                return "120";
            case "injury":
                return "110";
            case "fight":
                return "320";
            case "beer":
                return "220";
            case "fire":
                return "450";
            case "weather":
                return "680";
            default:
                return "100";
        }
    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(txtSliderValue != null){
                txtSliderValue.setText("Update frequency: " + progress + "ms");
            }

            setUpdateFrequency(progress);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    protected void startTimer() {
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 500);
    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            txtConnected.setText(String.valueOf(mSensorService.hasConnection));
            txtIp.setText(mSensorService.ip);
            txtSendLocations.setText(mSensorService.counter + "");
            txtHeading.setText(String.valueOf(mSensorService.bearing));
            txtLocation.setText(String.valueOf(mSensorService.longitude) + " " + String.valueOf(mSensorService.latitude));
            txtAccuracy.setText(String.valueOf(mSensorService.accuracy));
        }
    };

    @Override
    public void onDestroy() {
        stopService(service);
        Log.i("MAINACT", "onDestroy!");

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setUpdateFrequency(int value){
        editor.putInt("updateFrequency", value);
        editor.commit();
    }

    public int getUpdateFrequency(){
        return sharedPref.getInt("updateFrequency",1000);
    }

    public void setStewardName(String stewardName){
        editor.putString("stewardName", stewardName);
        editor.commit();
    }

    public String getStewardName(){
        return sharedPref.getString("stewardName","steward1");
    }

    public void setBroker(String broker){
        editor.putString("broker", broker);
        editor.commit();
    }

    public String getBroker(){
        return sharedPref.getString("broker","tcp://iot.eclipse.org:1883");
    }

    public String getUUID(){
        String uuid = sharedPref.getString("uuid","empty");
        if(uuid.equals("empty")){
            setUUID(UUID.randomUUID().toString().substring(0, 8));
        }else{
            return uuid;
        }

        return getUUID();
    }

    public void setUUID(String uuid){
        editor.putString("uuid", uuid);
        editor.commit();
    }
}
