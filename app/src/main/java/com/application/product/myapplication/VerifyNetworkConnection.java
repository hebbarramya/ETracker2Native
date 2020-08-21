package com.application.product.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.util.Arrays;

import static com.application.product.myapplication.ET_Command.mgConnection;

// verifies the cloud connectivity (Cell /Wifi)
public class VerifyNetworkConnection extends AppCompatActivity implements TIOConnectionCallback {

    // region Variable
    RadioButton RadioButtonCell, RadioButtonWifi;
    Button btnVerifyNetwork;
    ProgressDialog myDialog;

    private TIOPeripheral mPeripheral;
    private static final String TAG = "Verify NW Connection";
    private boolean isEmptyData;
    private static int mConnectionStatus = 1;
    private Context _context;


    // Constructor
   public VerifyNetworkConnection() {
        mgConnection.setListener(this);
    }

    // end region variable


    // Peripheral Call backs

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
    public void onDataReceived(TIOConnection tioConnection, final byte[] cdata) {
        isEmptyData = isValidData(cdata);
        try {
            Log.d(TAG, "Warning - onDataReceived len " + cdata.length);
            if (isEmptyData) {
                Log.d(TAG, "Warning - invalid data received");
                return;
            } else {
                processResponseData(cdata);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }


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
    //End Peripheral Call back

    // private method

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    public void processResponseData(byte[] responseData) throws IOException {

        int respCmdId = responseData[0];

        if (respCmdId == ET_Command.CMD_VERIFY_NETWORK_CONNECTION)     //21
        {
            Log.d(TAG, "Verify Cloud ACK" + Arrays.toString(responseData));//[21, 0, 1, 0]
            myDialog.dismiss();
            validateACKStatus(responseData);
        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }

    }

    private byte[] prepareVerifyNetworkReqPkt() {
        Log.d(TAG, "prepareVerifyNetworkReqPkt");
        byte[] verifyNetworkReqPkt = new byte[4];
        int verifyNWlen = 1;
        verifyNetworkReqPkt[0] = ET_Command.CMD_VERIFY_NETWORK_CONNECTION;//21
        verifyNetworkReqPkt[1] = 0;
        verifyNetworkReqPkt[2] = (byte) verifyNWlen;//Total length
        verifyNetworkReqPkt[3] = (byte) mConnectionStatus;
        return verifyNetworkReqPkt;
    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            Toast.makeText(_context, "Verify Network  Success !", Toast.LENGTH_LONG).show();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            Toast.makeText(_context, "Verify Network Failed !", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(_context, "Warning:Invalid Response !", Toast.LENGTH_LONG).show();
        }
    }


    // end Private method

    // UI event handlers

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify_networkconnection);
        setContentViews();
        setupActionBar();
        getconnection();
    }

    private void setContentViews() {
        btnVerifyNetwork = (Button) findViewById(R.id.btnVerifyCloudConnection);
        RadioButtonCell = (RadioButton) findViewById(R.id.btncell);
        RadioButtonWifi = (RadioButton) findViewById(R.id.btnwifi);
        _context = getApplicationContext();
        myDialog = CommonDialogs.showProgressDialog(this, "Verifying...");

    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }

    //  To navigate to peripheral activity

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    startActivity(new Intent(VerifyNetworkConnection.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }

        }
        return true;
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

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.btncell:
                if (checked)
                    mConnectionStatus = 1;
                break;
            case R.id.btnwifi:
                if (checked)
                    mConnectionStatus = 2;
                break;
        }
    }

    public void onVerifyNetworkPressed(View sender) throws IOException {
        byte[] sendVerifyNWReq = prepareVerifyNetworkReqPkt();
        mgConnection.transmit(sendVerifyNWReq);//Send Verify Cloud request pkt
        myDialog.show();
        Log.d(TAG, "onVerifyNetworkPressed Request packet Sent:" + Arrays.toString(sendVerifyNWReq));//[21, 0, 1, 1]

    }


    // End UI event Handlers


}
