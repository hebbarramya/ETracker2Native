package com.application.product.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonWriter;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.application.product.myapplication.ET_Command.mgConnection;

//Report Real Time Sensor data

public class ReportRealTimeSensorReading extends AppCompatActivity implements TIOConnectionCallback {

    // region member  variables

    EditText edit_displayRealTimeSensor;
    Button btn_getSensor;
    ProgressDialog myDialog;

    private TIOPeripheral mPeripheral;
    private CommonProcessResponse commonProcessResponse;//Non activity class Obj
    private static final String TAG = "Real Time Sensor";
    private Context _context;
    private Handler handler;
    private boolean isEmptyData;
    private int TOTAL_FILE_SIZE = 0;
    private int TOTAL_FILE_PACKET = 0;
    private int CONNECTED_SENSOR_CNT = 0;
    private static int sensorCount = 0;
    private static int total_sensorCount = 0;
    private int sensorReading_index = 0;
    private int expectedSeqNum = 1;
    private boolean isEmptySDI12Data;
    private byte RealTime_Sensor[] = {0};
    private String Str_entireSensorData = "";

    //Constructor
   public ReportRealTimeSensorReading() {
        mgConnection.setListener(this);
        commonProcessResponse = new CommonProcessResponse();
    }


    // end region variable


    // UI event handlers

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    startActivity(new Intent(ReportRealTimeSensorReading.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }

        }
        return true;
    }

    public void onGetSensorReadingPressed(View sender) throws IOException {

        Log.d(TAG, "onGetSensorReadingPressed");

        byte[] sendRealTimeSensorReq = prepareRealTimeSensorReq();
        mgConnection.transmit(sendRealTimeSensorReq);//Send RealTime Sensor request pkt
        Log.d(TAG, " Report Real Time Sensor Req Sent:" + Arrays.toString(sendRealTimeSensorReq));//[22, 0, 1, 1]

    }
    //End UI event handlers


    //private method
    private void getconnection() {

        Intent intent = getIntent();
        String address = intent.getStringExtra(TIOSample.PERIPHERAL_ID_NAME);
        // retrieve peripheral instance from TIOManager
        mPeripheral = TIOManager.getInstance().findPeripheral(address);
        if (mPeripheral != null) {
            Log.d(TAG, "Connection" + mgConnection);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.realtime_sensor_reading);
        setContentViews();
        setupActionBar();
        getconnection();
    }

    private void setContentViews() {
        edit_displayRealTimeSensor = (EditText) findViewById(R.id.displaysensorreading);
        btn_getSensor = (Button) findViewById(R.id.btngetSensorReading);
        _context = getApplicationContext();
        handler = new Handler();
        edit_displayRealTimeSensor.setShowSoftInputOnFocus(false);//Hide EditText keyboard
        myDialog = CommonDialogs.showProgressDialogPercent(this, "Fetching Sensor Count...");

    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }

    private byte[] prepareRealTimeSensorReq() {
        byte[] RealTimeSensorReqPkt = new byte[4];

        RealTimeSensorReqPkt[0] = ET_Command.CMD_REPORT_REAL_TIME_SENSOR;//22
        RealTimeSensorReqPkt[1] = 0;
        RealTimeSensorReqPkt[2] = 1;
        RealTimeSensorReqPkt[3] = (byte) ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_REQUEST_SUBCMD_ID.SubCmdId;//01

        return RealTimeSensorReqPkt;

    }

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    public void processResponseData(byte[] responseData) throws IOException, ParseException {

        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];

        if (respCmdId == ET_Command.CMD_REPORT_REAL_TIME_SENSOR)//22
        {
            if (respSubCmdId == ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_PROGRESS_ACK_SUBCMD_ID.SubCmdId)//02
            {
                myDialog.show();
                ShowProgressDialog();
                total_sensorCount = responseData[4];//21
                sensorCount = responseData[5];//get SensorNum queried
                Log.d(TAG, "Real Time Sensor progress ACK" + Arrays.toString(responseData));//[22, 0, 3, 2, 21, 2](for 2nd sensor)

            } else if (respSubCmdId == ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_INFO_PACKET_SUBCMD_ID.SubCmdId)//03
            {
                Log.d(TAG, "Real Time Sensor Info Packet" + Arrays.toString(responseData));//[22, 0, 8, 3, 0, 16, 0, 0, 7, -89, 21]
                getRealTimeSensor_Pkt_Size_count(responseData);//Get pktcnt and pktsize
                prepare_send_RealTimeSensor_InfoPktACK();//send ACK

            } else if (respSubCmdId == ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_DATA_PACKET_SUBCMD_ID.SubCmdId)//05
            {
                recvRealTimeSensor_DataAndSendACK(responseData);
            }
        }
    }

    private void getRealTimeSensor_Pkt_Size_count(byte[] respData) {

        TOTAL_FILE_PACKET = (byte) (respData[4] | respData[5]);
        Log.d(TAG, "TOTAL_FILE_PKT=" + TOTAL_FILE_PACKET);//16

        byte[] SensorInfoSize = Arrays.copyOfRange(respData, sensorReading_index + 6, sensorReading_index + 10);
        TOTAL_FILE_SIZE = ByteBuffer.wrap(SensorInfoSize).getInt();
        Log.d(TAG, "SDI_12 Info File Size  :" + TOTAL_FILE_SIZE);//1959

        CONNECTED_SENSOR_CNT = (byte) respData[10];
        Log.d(TAG, "Connected Sensor Count:" + CONNECTED_SENSOR_CNT);//

    }

    private void prepare_send_RealTimeSensor_InfoPktACK() throws IOException {

        byte[] RealTimeSensorInfoACK = new byte[5];
        int InfoPktlen = 0;
        RealTimeSensorInfoACK[0] = ET_Command.CMD_REPORT_REAL_TIME_SENSOR;//04
        RealTimeSensorInfoACK[1] = (byte) InfoPktlen;//0
        RealTimeSensorInfoACK[2] = 2;
        RealTimeSensorInfoACK[3] = (byte) ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_INFO_PACKET_ACK_SUBCMD_ID.SubCmdId;//04
        RealTimeSensorInfoACK[4] = 0;

        mgConnection.transmit(RealTimeSensorInfoACK);
        Log.d(TAG, "RealTimeSensor_InfoPktACK-" + Arrays.toString(RealTimeSensorInfoACK));//[22, 0, 2, 4, 0]

    }

    private void recvRealTimeSensor_DataAndSendACK(byte[] sensorData) throws IOException, ParseException {
        isEmptySDI12Data = isValidData(sensorData);
        int respSeqNum = sensorData[5];

        if (isEmptySDI12Data) {
            Log.d(TAG, "Warning - Received Empty Sensor Data");
            return;
        } else {
            iterateRealTimeSensorData(sensorData);
            sendRealTimeSensor_DataACK(respSeqNum);
            append_realTimeSensorData(respSeqNum);
        }
    }

    private void iterateRealTimeSensorData(byte[] sensorData) {

        RealTime_Sensor = new byte[sensorData.length - 6];
        Log.d(TAG, "Real Time Sensor Data.length:" + RealTime_Sensor.length);
        for (int i = 6; i < sensorData.length; i++)//iterate from the data excluding  Metadata(index(6))
        {
            RealTime_Sensor[i - 6] = sensorData[i];
        }
        Log.d(TAG, " Real Time Sensor Data after extracting :" + Arrays.toString(RealTime_Sensor));
    }

    private void sendRealTimeSensor_DataACK(int respSeqNum) throws IOException {

        Log.d(TAG, "Seqnum-" + respSeqNum);
        if (respSeqNum == expectedSeqNum) {
            prepare_sendRealTimeSensorDataACK(respSeqNum);//Send ACK to received file
            Log.d(TAG, "Sensor Data ACK sent");

            if (respSeqNum == TOTAL_FILE_PACKET) {
                expectedSeqNum = 0;
            }
            expectedSeqNum++;
        }
    }

    private void prepare_sendRealTimeSensorDataACK(int pktSeqNum) throws IOException {

        byte realTimeSensorDataACK[] = new byte[7];
        int sensorDatalen = 4;
        realTimeSensorDataACK[0] = ET_Command.CMD_REPORT_REAL_TIME_SENSOR;//22
        realTimeSensorDataACK[1] = 0;
        realTimeSensorDataACK[2] = (byte) sensorDatalen;//Total Length
        realTimeSensorDataACK[3] = (byte) ET_Command.REAL_TIME_SENSOR_SUBMCMDID.REAL_TIME_SENSOR_DATA_PACKET_ACK_SUBCMD_ID.SubCmdId;//06
        if (pktSeqNum == expectedSeqNum) {
            realTimeSensorDataACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Success.statusVal;//0

        } else {
            realTimeSensorDataACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Failure.statusVal;//1
        }
        realTimeSensorDataACK[5] = 0;
        realTimeSensorDataACK[6] = (byte) pktSeqNum;

        mgConnection.transmit(realTimeSensorDataACK);//Send config  data ACK
        Log.d(TAG, "Send  RealTimeSensorData ACK -" + Arrays.toString(realTimeSensorDataACK));//[22, 0, 4, 6, 0, 0, 2]
    }

    private void append_realTimeSensorData(int respSeqNum) throws ParseException {

        String sensorResponse = new String(RealTime_Sensor, StandardCharsets.UTF_8);//Conversion to string
        Str_entireSensorData += sensorResponse;//Append the String
        if (respSeqNum == TOTAL_FILE_PACKET) {
            convertStringToJSON_updateUI(Str_entireSensorData);//Convert To JSON
        }
    }

    private void convertStringToJSON_updateUI(String str_sensorData) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json_sensorData = (JSONObject) parser.parse(str_sensorData);//Parse String to  JSON

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json_sensorData);//Print JSON Data as Pretty-print
        myDialog.dismiss();
        edit_displayRealTimeSensor.setText(prettyJson);//Update the Edit Text
        Toast.makeText(_context, "Sensor Reading Success !", Toast.LENGTH_SHORT).show();
    }

    public void ShowProgressDialog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (sensorCount <= total_sensorCount) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myDialog.setProgress(sensorCount);
                            if (sensorCount == total_sensorCount) {
                                myDialog.setMax(total_sensorCount);
                                myDialog.setProgress(100);

                            }
                        }
                    });
                }
            }
        }).start();

    }


    //End private method


    //Peripheral Callback method

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
                Log.d(TAG, "Warning - Invalid data received");
                return;
            } else {
                commonProcessResponse.validateRecvdData(cdata, ReportRealTimeSensorReading.this);
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

    // End Peripheral callback


}
