package com.application.product.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOManagerCallback;
import com.telit.terminalio.TIOPeripheral;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class MainActivity extends AppCompatActivity implements TIOManagerCallback {

    private static final int ENABLE_BT_REQUEST_ID = 1;
    private static final int SCAN_INTERVAL = 10000;
    private static final String TAG = "eTracker";
    private static final int PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int SERVICE_REQUEST_LOCATION = 2;

    private Button mScanButton;
    private ListView mPeripheralsListView;
    private ArrayAdapter<TIOPeripheral> mPeripheralList;
    private Handler mScanHandler = new Handler();
    private TIOManager mTio;
    private Button btnconnect;
    private Context _context;

    public boolean checkLocationService(final @NonNull Activity activity) {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return true;
        } else {
            Intent enableLocation = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivityForResult(enableLocation, SERVICE_REQUEST_LOCATION);
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _context = getApplicationContext();
        connectViews();
        mTio = TIOManager.getInstance();
        mTio.enableTrace(true);

        if (!mTio.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
        }

        initializePeripheralsListView();
        updatePeripheralsListView();
        displayVersionNumber();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about_menu, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);

        switch (requestCode) {
            case ENABLE_BT_REQUEST_ID:
                if (resultCode == Activity.RESULT_CANCELED) {
                    mScanButton.setEnabled(false);
                    return;
                }
                break;

            case SERVICE_REQUEST_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "GPS enabled now on this device");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!checkLocationService(this)) {
                        Log.d(TAG, "GPS disabled on this device");
                    } else {
                        this.startTimedScan();
                    }
                }
                break;
            }
        }
    }

    //******************************************************************************
    // UI event handlers
    //******************************************************************************

    public void onScanButtonPressed(View sender) {
        Log.d(TAG, "onScanButtonPressed");
        mPeripheralsListView.setVisibility(View.VISIBLE);
        startTimedScan();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.about_app:

        }
        return true;
    }


    //******************************************************************************
    // TIOManagerCallback implementation
    //******************************************************************************

    @Override
    public void onPeripheralFound(TIOPeripheral peripheral) {
        Log.d(TAG, "onPeripheralDiscovered " + peripheral.toString());

        updatePeripheralsListView();
    }

    @Override
    public void onPeripheralUpdate(TIOPeripheral peripheral) {
        Log.d(TAG, "onPeripheralUpdate " + peripheral.toString());

        updatePeripheralsListView();
    }


    //******************************************************************************
    // Internal methods
    //******************************************************************************

    private void connectViews() {
        Log.d(TAG, "connectViews");

        mScanButton = (Button) findViewById(R.id.btnscan);
        mPeripheralsListView = (ListView) findViewById(R.id.peripheralsListView);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.onScanButtonPressed(view);
            }
        });

    }

    private void initializePeripheralsListView() {

        // create data adapter for peripherals list view
        mPeripheralList = new ArrayAdapter<TIOPeripheral>(this, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return createPeripheralCell(position);
            }

            @Override
            public int getCount() {
                return mTio.getPeripherals().length;
            }
        };
        mPeripheralsListView.setAdapter(mPeripheralList);
    }

    private View createPeripheralCell(int position) {


        final TIOPeripheral peripheral = mTio.getPeripherals()[position];

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View peripheralCell = inflater.inflate(R.layout.peripheral_cell, mPeripheralsListView, false);

        TextView mainTitle = (TextView) peripheralCell.findViewById(R.id.mainTitle);
        btnconnect = (Button) peripheralCell.findViewById(R.id.Connect);
        mainTitle.setText(peripheral.getName() + "  " + peripheral.getAddress());

        TextView subTitle = (TextView) peripheralCell.findViewById(R.id.subTitle);
        if (peripheral.getAdvertisement() != null) {
            subTitle.setText(peripheral.getAdvertisement().toString() + " RSSI " + peripheral.getAdvertisement().getRssi());
        } else {
            subTitle.setText("");
            mainTitle.setText("");
            btnconnect.setVisibility(View.GONE);
            peripheralCell.setEnabled(false);
        }
        btnconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, PeripheralActivity.class);
                intent.putExtra(TIOSample.PERIPHERAL_ID_NAME, peripheral.getAddress());
                startActivity(intent);

            }
        });

        return peripheralCell;
    }


    private void startTimedScan() {
        if (SDK_INT > LOLLIPOP) {
            // Check for permissions
            int persmissionCheck = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

            if (persmissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMITIONS_REQUEST_ACCESS_COARSE_LOCATION);
                return;
            }

            if (!checkLocationService(this)) {
                Log.d(TAG, "GPS disabled on this device");
                return;
            }
        }

        Log.d(TAG, "startTimedScan");

        mScanButton.setEnabled(false);

        mScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                try {
                    mTio.stopScan();
                } catch (Exception ex) {

                }
                mScanButton.setEnabled(true);
            }
        }, MainActivity.SCAN_INTERVAL);

        mTio.startScan(this);
    }

    private void updatePeripheralsListView() {
        // update adapter with currently known peripherals
        mPeripheralList.notifyDataSetChanged();
    }


    private void displayVersionNumber() {
        PackageInfo packageInfo;
        String version = "";
        try {
            packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (Exception ex) {
            Log.e(TAG, toString());
        }
        setTitle(getTitle() + " " + version);
    }


}
