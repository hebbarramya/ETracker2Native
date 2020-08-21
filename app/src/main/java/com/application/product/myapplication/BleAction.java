package com.application.product.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;

import static com.application.product.myapplication.ET_Command.mgConnection;

public class BleAction extends AppCompatActivity implements TIOConnectionCallback {

    // region Member variables

    Button btngetconfig, btnuploadconfig, btnSDI_12, btn_Timesync;
    private static final String TAG = "BLE Action";
    private TIOPeripheral mPeripheral;
    private BLEOperation bleOperation;
    private static Context _context;
    private String password_text = "";
    private static ProgressDialog myDialog;

    //End region

    // Constructor

    public BleAction() {

        mgConnection.setListener(this);

    }


    // region Private methods

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }

    private void setContentViews() {
        bleOperation = new BLEOperation(_context);
        btngetconfig = (Button) findViewById(R.id.getconfig);
        btnuploadconfig = (Button) findViewById(R.id.setconfig);
        btnSDI_12 = (Button) findViewById(R.id.SDI_12Passthrough);
        btn_Timesync = (Button) findViewById(R.id.getTimeSync);
        _context = getApplicationContext();
        new BLEOperation(this);
        myDialog = CommonDialogs.showProgressDialog(this, "Reporting...");
    }


    private void getconnection() {
        Intent intent = getIntent();
        String address = intent.getStringExtra(TIOSample.PERIPHERAL_ID_NAME);
        // retrieve peripheral instance from TIOManager
        mPeripheral = TIOManager.getInstance().findPeripheral(address);
        if (mPeripheral != null) {
            Log.d(TAG, "Connection" + mgConnection);
        }

    }

    public static void showToast(String message) {
        Toast.makeText(_context, message, Toast.LENGTH_LONG).show();
    }

    public static void showDialog() {
        myDialog.show();
    }

    public static void hideDialog() {
        myDialog.hide();
    }

    public void showPasswordAlert() {
        LayoutInflater li = LayoutInflater.from(BleAction.this);
        View promptsView = li.inflate(R.layout.prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                BleAction.this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                password_text = userInput.getText().toString();
                                try {
                                    bleOperation.send_SetPaswordData(password_text);
                                } catch (Exception e) {
                                    Log.e(TAG, "Warning:Operation Failed");
                                }


                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    // end private method

    // UI event handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_action);
        setupActionBar();
        setContentViews();
        getconnection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    startActivity(new Intent(BleAction.this, PeripheralActivity.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
            case R.id.disconnect:
                try {
                    onDisconnectPressed();
                } catch (IOException e) {
                    Log.d(TAG, " Warning! Disconnect option not Working! ");
                }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.disconnect_menu, menu);
        return true;
    }

    private void onDisconnectPressed() throws IOException {
        mgConnection.disconnect();
        Intent i = new Intent(BleAction.this, MainActivity.class);
        startActivity(i);
    }

    public void onGetConfigurationPressed(View sender) {
        Intent intent = new Intent(BleAction.this, ReadConfig.class);
        startActivity(intent);
    }

    public void onSDI_12Pressed(View sender) {
        Intent intent = new Intent(BleAction.this, Passthrough.class);
        startActivity(intent);
    }

    public void onUploadConfigurationPressed(View sender) {
        Intent intent = new Intent(BleAction.this, UploadConfig.class);
        startActivity(intent);
    }

    public void onGetTimeSyncPressed(View sender) {
        Intent intent = new Intent(BleAction.this, DeviceDetailsTimeSync.class);
        startActivity(intent);

    }

    public void onSDICardStatusPressed(View sender) throws IOException {
        bleOperation.send_SDCardStatusReqPkt();
    }

    public void onSwitchToLMPressed(View sender) throws IOException {
        bleOperation.send_SwitchToLM();

    }

    public void onReportRealTimeSensorToServer(View sender) throws IOException {

        bleOperation.send_prepareRealtimeSensorToServer();

    }

    public void onReportCurrentConfigToServer(View sender) throws IOException {
        bleOperation.send_prepareCurrentConfigToServer();

    }

    public void onScanSDI_12Pressed(View sender) {
        Intent intent = new Intent(BleAction.this, Scan_SDI12.class);
        startActivity(intent);
    }

    public void onSetPasswordPressed(View sender) {
        showPasswordAlert();
    }

    public void onApplyActionsPressed(View sender) {
        Intent intent = new Intent(BleAction.this, ApplyActions.class);
        startActivity(intent);
    }

    public void onRealTimeSensorReadingPressed(View sender) {
        Intent intent = new Intent(BleAction.this, ReportRealTimeSensorReading.class);
        startActivity(intent);
    }

    public void onTemporaryOverRideJSONPressed(View sender) {
        Intent intent = new Intent(BleAction.this, TempoaryOverrideJson.class);
        startActivity(intent);
    }

    public void onCertificateDownloadPressed(View sender) {
        Intent intent = new Intent(BleAction.this, Certificate_Download.class);
        startActivity(intent);
    }

    public void onOTAPressed(View sender) {
        Intent intent = new Intent(BleAction.this, OTA_Ble.class);
        startActivity(intent);
    }


    public void onVerifyNWConnectivityPressed(View sender) {
        Intent intent = new Intent(BleAction.this, VerifyNetworkConnection.class);
        startActivity(intent);

    }
    // end UI event handlers

    // Peripheral CallBacks

    @Override
    public void onConnected(TIOConnection tioConnection) {

    }

    @Override
    public void onConnectFailed(TIOConnection tioConnection, String s) {

    }

    @Override
    public void onDisconnected(TIOConnection tioConnection, String s) {

    }

    @Override
    public void onDataReceived(TIOConnection tioConnection, byte[] bytes) {

    }

    @Override
    public void onDataTransmitted(TIOConnection tioConnection, int i, int i1) {

    }

    @Override
    public void onReadRemoteRssi(TIOConnection tioConnection, int i, int i1) {

    }

    @Override
    public void onLocalUARTMtuSizeUpdated(TIOConnection tioConnection, int i) {

    }

    @Override
    public void onRemoteUARTMtuSizeUpdated(TIOConnection tioConnection, int i) {

    }

    // End Callback

}
