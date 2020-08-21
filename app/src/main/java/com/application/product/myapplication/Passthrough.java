package com.application.product.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.application.product.myapplication.ET_Command.mgConnection;

public class Passthrough extends AppCompatActivity implements TIOConnectionCallback {


    //Region Variables

    EditText editSDI;
    TextView editresponse;
    Button butnsend;
    RadioButton RadioButtonYes, RadioButtonNo;
//    Spinner mspinner;

    private TIOPeripheral mPeripheral;


    private static final String TAG = "Passthrough";
    private static int mChannel = 0;
    private Context _context;


    // SDI data

    private byte SDIdata[] = {0};

    // End region variables
    // Constructor

    public Passthrough() {
        mgConnection.setListener(this);
    }

    //UI Event Handlers
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passthrough);

        setContentViews();
        setupActionBar();
        getconnection();
        // setSpinner();
    }

    private void setContentViews() {

        editSDI = (EditText) findViewById(R.id.sdiData);
        editresponse = (TextView) findViewById(R.id.responsedata);
        butnsend = (Button) findViewById(R.id.btnsend);
        RadioButtonYes = (RadioButton) findViewById(R.id.btnyes);
        RadioButtonNo = (RadioButton) findViewById(R.id.btnno);
        //   mspinner = (Spinner) findViewById(R.id.spinner);


        _context = getApplicationContext();

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
                    Log.d(TAG, "homeconnection" + mgConnection);
                    mgConnection.transmit(powerOffdata());
                    Log.d(TAG, "Poweroff");
                    startActivity(new Intent(Passthrough.this, BleAction.class));
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
//            mgConnection.setListener(this);//Redirects the callback to same class
            Log.d(TAG, "Connection" + mgConnection);
        }

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.btnyes:
                if (checked)
                    mChannel = 0;
                break;
            case R.id.btnno:
                if (checked)
                    mChannel = 1;
                break;
        }
    }


    public void onSendButtonPressed(View sender) {
        Log.d(TAG, "onSendButtonPressed");
        try {
            String SDIDataa = editSDI.getText().toString();
            Log.d(TAG,"SDIData from edit text"+SDIDataa);
            //mspinner.getSelectedItem().toString();

            if (SDIDataa != null) {
                byte sdidata[] = SDIData(SDIDataa);
                Log.d(TAG, "SDI" + Arrays.toString(sdidata));
                Log.d(TAG, "SDI Connection" + mgConnection);

                mgConnection.transmit(sdidata);//send SDIdata
            } else {
                Log.d(TAG, "Data is empty");
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
    }
    //End UI Handlers

    //Private Method

    //switch off SDI 12 sensor
    private byte[] powerOffdata() {
        byte poweroffdata[] = new byte[10];
        poweroffdata[0] = ET_Command.CMD_SDI_12_BUS_POWOFF;//15
        poweroffdata[1] = 0;
        poweroffdata[2] = 0;
        return poweroffdata;

    }


//    private void setSpinner() {
//        mspinner.setOnItemSelectedListener(this);
//
//        // Spinner Drop down elements
//        List commands = new ArrayList();
//        commands.add("Select SDI Commnad");
//        commands.add("ZI!");
//        commands.add("ZV!");
//        commands.add("ZM!");
//        commands.add("ZD0!");
//        commands.add("ZD1!");
//        commands.add("ZD2!");
//        commands.add("ZXS!");
//        commands.add("ZXW!");
//        commands.add("ZXM!");
//
//        // Creating adapter for spinner
//        ArrayAdapter dataAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, commands);
//
//        // Drop down layout style - list view with radio button
//        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//        // attaching data adapter to spinner
//        mspinner.setAdapter(dataAdapter);
//
//    }


    private byte[] SDIData(String text) {

        SDIdata = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];

        int sdidatalen = text.getBytes().length;
        Log.d(TAG, "SDI data length:" + sdidatalen);
        int totlength = sdidatalen + 1;//Appending length of channel(1/0)

        //Byte data
        SDIdata[0] = ET_Command.CMD_SDI_12_BUS;//14
        SDIdata[1] = (byte) ((sdidatalen >> 8) & 0XFF);//0
        SDIdata[2] = (byte) (totlength & 0XFF);//4
        SDIdata[3] = (byte) mChannel;//0 or 1

        byte[] asciidata = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 4; i < sdidatalen + 4; i++) {
            SDIdata[i] = asciidata[i - 4];
            Log.d(TAG, "SDIdata4:" + SDIdata[i]);//(ZI!) 90,73,33
        }

        return SDIdata;
    }

    //End private Methods
    //Peripheral Call back Methods
    @Override
    public void onConnected(TIOConnection tioConnection) {


    }

    @Override
    public void onConnectFailed(TIOConnection tioConnection, String s) {

    }

    @Override
    public void onDisconnected(TIOConnection tioConnection, String s) {

    }

//    @Override
//    public void onDataReceived(TIOConnection tioConnection, final byte[] data) {
//        Log.d(TAG, "onDataReceived len " + data.length);
//        try {
//            Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    byte tempdata[] = new byte[data.length - 3];
//                    int len = data[2];
//                    if (data.length != 0) {
//                        if (data[0] == ET_Command.CMD_SDI_12_BUS)//14
//                        {
//                            if (data.length > 2)//Validate length is >2bytes
//                            {
//                                Toast.makeText(_context, "Entered", Toast.LENGTH_SHORT).show();
//                                if (!isFullDataPackt) {
//                                    Toast.makeText(_context, "Entered full data pkt", Toast.LENGTH_SHORT).show();
//                                    if (!isnotFirstPacket)//First pkt
//                                    {
//                                        Toast.makeText(_context, "Entered isnot first data pkt", Toast.LENGTH_SHORT).show();
////                                             isnotFirstPacket=true;
//                                        for (int i = 3; i < data.length; i++) {
//                                            //Toast.makeText(_context, "Entered for loop", Toast.LENGTH_SHORT).show();
//                                            tempdata[i - 3] = data[i];
//
//                                        }
//
//                                        SDIPacket = new byte[50];
//                                        SDIPacket = tempdata;
//
//                                        if (SDIPacket.length == len) {
//
//                                            isFullDataPackt = true;
//                                            String sdiresponse = new String(SDIPacket, StandardCharsets.UTF_8);//Conversion to string
//                                            Log.d(TAG, "SDI response" + sdiresponse);
//                                            Log.d(TAG, "SDI Packet First Pkt" + Arrays.toString(SDIPacket));
//                                            editresponse.setText(sdiresponse);
//                                        }
//
//                                    } else//second pkt
//                                    {
//                                        Log.d(TAG, "Second pkt" + isFullDataPackt);
//                                        byte secondpkt[] = new byte[data.length];
//                                        for (int s = 0; s < data.length; s++) {
//                                            secondpkt[s] = data[s];
//                                        }
//                                        System.arraycopy(secondpkt, 0, SDIPacket, tempdata.length, SDIPacket.length);
//                                        Log.d(TAG, "SDI full Packet" + Arrays.toString(SDIPacket));
//                                        if (SDIPacket.length == len) {
//
//                                            isFullDataPackt = true;
//                                            String sdiresponse = new String(SDIPacket, StandardCharsets.UTF_8);//Conversion to string
//                                            Log.d(TAG, "SDI response" + sdiresponse);
//                                            editresponse.setText(sdiresponse);
//                                        }
//                                    }
//
//                                }
//                            }
//                        }
//                    } else {
//                        Log.d(TAG, "Invalid data");
//
//                    }
//                }
//            };
//            runOnUiThread(runnable);
//        } catch (Exception ex) {
//            Log.d(TAG, ex.toString());
//        }
//
//
//    }

    @Override
    public void onDataReceived(TIOConnection tioConnection, final byte[] data) {
        Log.d(TAG, "onDataReceived len " + data.length);
        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    byte tempdata[] = new byte[data.length - 3];

                    if (data.length != 0) {
                        if (data[0] == ET_Command.CMD_SDI_12_BUS)//14
                        {
                            if (data.length > 2)//Validate length is >2bytes
                            {
                                for (int i = 3; i < data.length; i++) {
                                    tempdata[i - 3] = data[i];
                                    String sdiresponse = new String(tempdata, StandardCharsets.UTF_8);//Conversion to string
                                    Log.d(TAG, "SDI response" + sdiresponse);
                                    editresponse.setText(sdiresponse);

                                }


                            }
                        }
                    } else {
                        Log.d(TAG, "Invalid data");

                    }
                }
            };
            runOnUiThread(runnable);
        } catch (Exception ex) {
            Log.d(TAG, ex.toString());
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
////Peripheral Call back Methods
//    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//        String item = parent.getItemAtPosition(position).toString();
//        // Showing selected spinner item
//        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
//
//    }
//
//    @Override
//    public void onNothingSelected(AdapterView<?> parent) {
//
//    }
}







