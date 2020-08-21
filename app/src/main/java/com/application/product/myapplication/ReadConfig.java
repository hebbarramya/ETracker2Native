package com.application.product.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static com.application.product.myapplication.ET_Command.mgConnection;

public class ReadConfig extends AppCompatActivity implements TIOConnectionCallback {

    // region Member variables
    Button btngetconfig;
    EditText txtdisplayconfig;

    private byte START_PKT_LENGTH = 11;
    private int TOTAL_FILE_PACKET = 0;
    private int TOTAL_FILE_SIZE = 0;
    private int expectedSeqNum = 1;
    private int config_index = 0;
    private static final String TAG = "Configuration";
    ProgressDialog myDialog;
    private String Str_FullConfigString = "";
    private Context _context;
    private TIOPeripheral mPeripheral;
    private boolean isEmptyConfgFile;
    private CommonProcessResponse commonProcessResponse;//Non activity class Obj
    private byte[] configuration_data = {0};

    // endregion

    // region Constructor

    public ReadConfig() {
        mgConnection.setListener(this);
        commonProcessResponse = new CommonProcessResponse();
    }


    // region Peripheral callbacks

    @Override
    public void onConnected(TIOConnection tioConnection) {
    }

    @Override
    public void onConnectFailed(TIOConnection tioConnection, String s) {
        Log.d(TAG, "Warning- Connection Failed");
    }

    @Override
    public void onDisconnected(TIOConnection tioConnection, String s) {
        Log.d(TAG, "Warning- BLE Disconnected");
    }

    @Override
    public void onDataReceived(TIOConnection tioConnection, final byte[] cdata) {
        isEmptyConfgFile = isValidData(cdata);

        try {
            Log.d(TAG, "Warning - onDataReceived len " + cdata.length);
            Log.d(TAG, "Array Received" + Arrays.toString(cdata));

            if (isEmptyConfgFile) {
                Log.d(TAG, "Warning - Invalid data received");
                return;
            } else {
                Log.d(TAG, "After Vaidate Recvd Data");
                commonProcessResponse.validateRecvdData(cdata, ReadConfig.this);
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

    }

    @Override
    public void onDataTransmitted(TIOConnection tioConnection, int status, int bytesWritten) {
        Log.d(TAG, "Warning- On Data Transmitted. Status: " + status + " Length: " + bytesWritten);
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

    // endregion

    // region UI Event handlers

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    startActivity(new Intent(ReadConfig.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    public void onGetConfigPressed(View sender) throws IOException {
        Log.d(TAG, "Time Stamp after sent:" + System.currentTimeMillis());
        Log.d(TAG, "onGetConfigButtonPressed");
        myDialog.show();
        //Triggering Get Config with no data
        byte[] getConfigFile = startConfigReadPacket();
        mgConnection.transmit(getConfigFile);

        Log.d(TAG, "Read File:" + Arrays.toString(getConfigFile));

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration);
        setContentViews();
        setupActionBar();
        getconnection();
        requestStoragePermission();

    }

    // endregion

    // region Private methods

    private byte[] startConfigReadPacket() {
        byte[] getConfigData = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];

        //Start Configuration Data Packet for GET CONFIG
        getConfigData[0] = ET_Command.CMD_START_GET_CONFIG;//16
        //Total length
        getConfigData[1] = 0;
        getConfigData[2] = START_PKT_LENGTH;//11

        getConfigData[3] = ET_Command.CMD_START_CONFIG_SUBCMD_ID;//01
        //Total File Packet
        getConfigData[4] = 0;
        getConfigData[5] = 0;

        //File Size
        getConfigData[6] = 0;
        getConfigData[7] = 0;
        getConfigData[8] = 0;
        getConfigData[9] = 0;

        //File version
        getConfigData[10] = 0;
        getConfigData[11] = 0;

        //CRC of File
        getConfigData[12] = 0;
        getConfigData[13] = 0;
        Log.d(TAG, "startConfigReadPacket" + Arrays.toString(getConfigData));
        return getConfigData;

    }


    private void setContentViews() {
        btngetconfig = (Button) findViewById(R.id.getconfig);
        txtdisplayconfig = (EditText) findViewById(R.id.displayconfig);
        _context = getApplicationContext();
        txtdisplayconfig.setShowSoftInputOnFocus(false);//Hide EditText keyboard
        myDialog = CommonDialogs.showProgressDialog(this, "Reading Configuration file...");


    }

    private void requestStoragePermission() {
        //App opens the Settings Page to enable read Storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            finish();
            startActivity(intent);
            return;
        }

    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
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


    public void processResponseData(byte[] responseData) throws IOException, ParseException {

        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];

        //GET Config Response data
        if (respCmdId == ET_Command.CMD_START_GET_CONFIG)//16
        {
            if (respSubCmdId == ET_Command.CMD_START_CONFIG_SUBCMD_ID)//01
            {
                sendStartConfigACK();//Send ACK along with recvng data
                get_PktCount_ConfigFileSize(responseData);

            } else if (respSubCmdId == ET_Command.CMD_CONFIG_DATA_PACKET_SUBCMD_ID)//03
            {
                recvConfigFileAndSendACK(responseData);
            } else {
                Log.d(TAG, "Warning- Invalid sub command ID" + respSubCmdId);
            }
        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }

    }

    private void sendStartConfigACK() throws IOException {

        //Send ACK for start Config for GET CONFIG
        byte[] readytorecv = prepareStartConfigACK();
        mgConnection.transmit(readytorecv);
        Log.d(TAG, "Ready to receive packet transmit for read config sent" + Arrays.toString(readytorecv));//[16, 0, 3, 2, 0, 0]

    }

    private byte[] prepareStartConfigACK() {

        Log.d(TAG, "Inside sendStartConfigACK");
        byte[] startConfigACK = new byte[6];
        startConfigACK[0] = ET_Command.CMD_START_GET_CONFIG;//16
        startConfigACK[1] = 0;
        startConfigACK[2] = 3;//Total length
        startConfigACK[3] = ET_Command.CMD_START_CONFIG_ACK_SUBCMD_ID;//02
        startConfigACK[4] = 0;
        startConfigACK[5] = 0;
        Log.d(TAG, "Ready to receive  for read config sent" + Arrays.toString(startConfigACK));//[16, 0, 3, 2, 0, 0]
        return startConfigACK;
    }

    private void recvConfigFileAndSendACK(byte[] configFile) throws IOException, ParseException {
        isEmptyConfgFile = isValidData(configFile);
        int respSeqNum = configFile[5];

        if (isEmptyConfgFile) {
            Log.d(TAG, "Warning - Received Empty Config File");
            return;
        } else {
            iterateConfigData(configFile);
            sendFileACK(respSeqNum);
            append_configFileData(respSeqNum);
        }

    }

    private void sendFileACK(int respSeqNum) throws IOException {

        Log.d(TAG, "Seqnum-" + respSeqNum);
        if (respSeqNum == expectedSeqNum) {
            prepareFileACK(respSeqNum);//Send ACK to received file
            Log.d(TAG, "File ACK sent");

            if (respSeqNum == TOTAL_FILE_PACKET) {
                expectedSeqNum = 0;
            }
            expectedSeqNum++;
        }
    }

    private void append_configFileData(int respSeqNum) throws ParseException {

        Log.d(TAG, "Time Stamp after Read:" + System.currentTimeMillis());//14388

        String confileFileresponse = new String(configuration_data, StandardCharsets.UTF_8);//Conversion to string
        Str_FullConfigString += confileFileresponse;//Append the String
        if (respSeqNum == TOTAL_FILE_PACKET) {
            convertStringToJSON_updateUI(Str_FullConfigString);//Convert To JSON
        }

    }

    private void convertStringToJSON_updateUI(String str_fileData) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject json_configData = (JSONObject) parser.parse(str_fileData);//Parse String to  JSON

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json_configData);//Print JSON Data as Pretty-print
        Log.d(TAG, "Pretty Json:" + prettyJson);
        myDialog.dismiss();
        txtdisplayconfig.setText(prettyJson);//Update the Edit Text
        Toast.makeText(_context, "Reading Configuration Success !", Toast.LENGTH_SHORT).show();
    }

    private void prepareFileACK(int pktSeqNum) throws IOException {

        byte sendConfigfileACK[] = new byte[8];
        int totallength = 5;
        sendConfigfileACK[0] = ET_Command.CMD_START_GET_CONFIG;//16
        sendConfigfileACK[1] = 0;
        sendConfigfileACK[2] = (byte) totallength;//Total Length
        sendConfigfileACK[3] = ET_Command.CMD_CONFIG_DATA_PACKET_ACK_SUBCMD_ID;//04
        if (pktSeqNum == expectedSeqNum) {
            sendConfigfileACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Success.statusVal;//0
        } else {
            sendConfigfileACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Failure.statusVal;//1
        }
        sendConfigfileACK[5] = 0;
        sendConfigfileACK[6] = (byte) pktSeqNum;
        sendConfigfileACK[7] = 0;

        mgConnection.transmit(sendConfigfileACK);//Send config  data ACK
        Log.d(TAG, "Send Config File ACK for Get Config-" + Arrays.toString(sendConfigfileACK));
    }


    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private void iterateConfigData(byte[] configFile) {
        configuration_data = new byte[configFile.length - 6];
        for (int i = 6; i < configFile.length; i++) {
            configuration_data[i - 6] = configFile[i];
        }

    }

    private void get_PktCount_ConfigFileSize(byte[] recvdPacketData) {

        TOTAL_FILE_PACKET = (byte) (recvdPacketData[4] | recvdPacketData[5]);
        Log.d(TAG, "TOTAL_FILE_PKT=" + TOTAL_FILE_PACKET);

        byte[] ConfigFileSize = Arrays.copyOfRange(recvdPacketData, config_index + 6, config_index + 10);
        TOTAL_FILE_SIZE = ByteBuffer.wrap(ConfigFileSize).getInt();
        Log.d(TAG, "Config File Size  :" + TOTAL_FILE_SIZE);
    }


    // endregion
}




