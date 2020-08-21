package com.application.product.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.application.product.myapplication.ET_Command.mgConnection;
//Connected Device Details & Time Sync
public class DeviceDetailsTimeSync extends AppCompatActivity implements TIOConnectionCallback {
    // region variables
    Button btn_timestamp;
    RelativeLayout device_details;
    TextView txtStationName, txtSUIData, txtStationTime, txtBattery_volt, txtGPSCordinate, txtTimeZone, txtHWVer, txtFWVer;

    private static int epochTimeInSeconds = 0;
    private byte deviceData[] = {0};
    private boolean isEmptyData;
    private Context _context;
    private static final String TAG = "Device Details";
    private TIOPeripheral mPeripheral;
    private CommonProcessResponse commonProcessResponse;//Non-Activity class Obj
    private static int device_Startindex = 0;
    private static int device_Endindex = 1;


    //Device variables
    String stationName;
    String hex_SUI;
    String station_localTimeZone;
    String cur_HardwareVersion;
    String cur_FirmwareVersion;
    String GPS_CoOrdinate;
    String str_TimeZone;
    Float battery_voltage;
    // End region variable

    // Constructor
   public DeviceDetailsTimeSync() {
        mgConnection.setListener(this);
        commonProcessResponse = new CommonProcessResponse();
    }
    // Peripheral Callback

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
                commonProcessResponse.validateRecvdData(cdata, DeviceDetailsTimeSync.this);
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

    // End Peripheral Call back

    //UI Event Handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_details);
        setupActionBar();
        setContentViews();
        getconnection();
        getEpochTime();
    }

    public void onTimeStampPressed(View Sender) throws IOException {
        Log.d(TAG, "Inside onTimeStampPressed");
        byte[] sendTimeStampRequest = prepareTimeStampData();
        mgConnection.transmit(sendTimeStampRequest);//Send UTC Time
        Log.d(TAG, "Time Stamp Req sent-" + Arrays.toString(sendTimeStampRequest));//[19, 0, 4, 93, 21, -61, -111] // 1561707409
    }

    public void onDeviceDetailsPressed(View Sender) throws IOException {
        Log.d(TAG, "Inside onDeviceDetailsPressed");
        device_details.setVisibility(View.VISIBLE);
        byte[] sendDeviceDetailsRequest = prepareGetDeviceDetailReqPkt();
        mgConnection.transmit(sendDeviceDetailsRequest);//Send Device Details request
        Log.d(TAG, "Devcie Detail Req sent-" + Arrays.toString(sendDeviceDetailsRequest));//[18, 0, 0]

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    startActivity(new Intent(DeviceDetailsTimeSync.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }
    // end  UI Event Handlers

    // region Private Methods

    private void setContentViews() {
        _context = getApplicationContext();
        btn_timestamp = (Button) findViewById(R.id.setTimestamp);
        device_details = (RelativeLayout) findViewById(R.id.relativelayout_device);
        txtStationName = (TextView) findViewById(R.id.txtdisplayStationName);
        txtSUIData = (TextView) findViewById(R.id.txtdisplaySUIData);
        txtStationTime = (TextView) findViewById(R.id.txtdisplayStationTime);
        txtHWVer = (TextView) findViewById(R.id.txtdisplayHWVersion);
        txtFWVer = (TextView) findViewById(R.id.txtdisplayFWVersion);
        txtBattery_volt = (TextView) findViewById(R.id.txtdisplayBattery_voltage);
        txtGPSCordinate = (TextView) findViewById(R.id.txtdisplayGPS_Coor);
        txtTimeZone = (TextView) findViewById(R.id.txtdisplayTimeZone);
        device_details.setVisibility(View.GONE);
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

    private void getEpochTime() {

        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(" MMM dd yyyy HH:mm:ss zzz");
        // format() formats a Date into a date/time string.
        String currentTime = sdf.format(today);
        Log.d(TAG, "Current Time = " + currentTime);
        try {
            // parse() parses text from the beginning of the given string to produce a date.
            Date date = sdf.parse(currentTime);
            // getTime() returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object.
            long epochTime = date.getTime();
            epochTimeInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(epochTime);//Parse Long to Int
            Log.d(TAG, "Current Time in Epoch: " + epochTime);
            Log.d(TAG, "Current Time in Epoch in seconds: " + epochTimeInSeconds);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private byte[] prepareTimeStampData() {

        Log.d(TAG, "prepareTimeStampData");
        byte[] timeSetRequestPkt = new byte[7];
        timeSetRequestPkt[0] = ET_Command.CMD_TIME_SYNC_REQUEST_PACKET;//19

        timeSetRequestPkt[1] = 0;
        timeSetRequestPkt[2] = 4;//Total length
        //Time in Epoch
        timeSetRequestPkt[3] = (byte) ((epochTimeInSeconds & 0XFF000000) >> 24);
        timeSetRequestPkt[4] = (byte) ((epochTimeInSeconds & 0X00FF0000) >> 16);
        timeSetRequestPkt[5] = (byte) ((epochTimeInSeconds & 0X0000FF00) >> 8);
        timeSetRequestPkt[6] = (byte) ((epochTimeInSeconds & 0X000000FF) >> 0);
        return timeSetRequestPkt;
    }

    public void processResponseData(byte[] responseData) {
        int respCmdId = responseData[0];

        if (respCmdId == ET_Command.CMD_TIME_SYNC_REQUEST_PACKET)//19
        {
            Log.d(TAG, "Time Sync Ack" + Arrays.toString(responseData));//[19, 0, 1, 0]
            validateACKStatus(responseData);

        } else if (respCmdId == ET_Command.CMD_DEVICE_DETAILS_REQUEST_PACKET)//18
        {
            processDeviceDetails(responseData);

        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            Toast.makeText(_context, "Set TimeStamp To Device Success !", Toast.LENGTH_LONG).show();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            Toast.makeText(_context, "Set TimeStamp To Device Failed !", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(_context, "Warning:Invalid Response !", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private byte[] prepareGetDeviceDetailReqPkt() {

        byte[] deviceDetailReqPackt = new byte[3];

        deviceDetailReqPackt[0] = ET_Command.CMD_DEVICE_DETAILS_REQUEST_PACKET;//18
        //Total Length
        deviceDetailReqPackt[1] = 0;
        deviceDetailReqPackt[2] = 0;
        return deviceDetailReqPackt;
    }

    private void processDeviceDetails(byte[] respData) {
        isEmptyData = isValidData(respData);
        if (isEmptyData) {
            Log.d(TAG, "Warning - Received Empty Device Details");
            return;
        } else {
            iterateDeviceData(respData);
            updateUI_deviceDetails();
        }

    }

    private void iterateDeviceData(byte[] respData) {
        deviceData = new byte[respData.length];

        for (int i = 3; i < respData.length; i++) {
            deviceData[i - 3] = respData[i];
        }
        Log.d(TAG, "Device Data Recvd:" + Arrays.toString(deviceData));
        getDeviceDetails(deviceData);//Get the device data

    }

    private void getDeviceDetails(byte[] recvDeviceData) {

        getStationName(recvDeviceData);
        getSUI_Data(recvDeviceData);
        getCurrentTime_Station(recvDeviceData);
        getCur_HW_version(recvDeviceData);
        getCur_FW_version(recvDeviceData);
        getBattery_Voltage(recvDeviceData);
        getGPS_CoOrdinate(recvDeviceData);
        getTimeZone(recvDeviceData);

    }

    private void getStationName(byte[] deviceData) {

        int Stationnamelength = deviceData[0];
        Log.d(TAG, "Stationnamelength:" + Stationnamelength);//11

        byte[] Stationname = Arrays.copyOfRange(deviceData, device_Startindex + 1, device_Endindex + Stationnamelength);//(index )1 to 11
        stationName = new String(Stationname, StandardCharsets.UTF_8);

        device_Startindex += Stationnamelength;//11
        device_Endindex += Stationnamelength;//12
    }

    private void getSUI_Data(byte[] deviceData) {

        byte[] SUI_data = Arrays.copyOfRange(deviceData, device_Startindex + 1, device_Endindex + 8);//12 to 19
        long SUI_value = ByteBuffer.wrap(SUI_data).getLong();
        hex_SUI = Long.toHexString(SUI_value);

        device_Startindex += SUI_data.length;//19
        device_Endindex += SUI_data.length;//20

    }

    private void getCurrentTime_Station(byte[] deviceData) {

        byte[] cStationTimeData = Arrays.copyOfRange(deviceData, device_Startindex + 1, device_Endindex + 4);//20 to 23
        Log.d(TAG, "cStationTimeData=" + Arrays.toString(cStationTimeData));

        int curStationTimeInSeconds = ByteBuffer.wrap(cStationTimeData).getInt();
        Log.d(TAG, "Byte array to int=" + curStationTimeInSeconds);

        Date date = new Date(curStationTimeInSeconds * 1000L);//Convert Sec to MillSec
        // format of the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");//Convert UTC to Local Time Zone
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        station_localTimeZone = dateFormat.format(date);
        Log.d(TAG, "UTC Time in Standard Time Zone -" + station_localTimeZone);


        device_Startindex += cStationTimeData.length;//23
        device_Endindex += cStationTimeData.length;//24
    }

    private void getCur_HW_version(byte[] deviceData) {

        cur_HardwareVersion = deviceData[device_Startindex + 1] + "." + deviceData[device_Endindex + 1];//24 to 25
        device_Startindex += 2;//25
        device_Endindex += 2;//26

    }

    private void getCur_FW_version(byte[] deviceData) {

        cur_FirmwareVersion = deviceData[device_Startindex + 1] + "." + deviceData[device_Endindex + 1];//26 to 27
        device_Startindex += 2;//27
        device_Endindex += 2;//28

    }

    private void getBattery_Voltage(byte[] deviceData) {

        byte[] batteryVoltage = Arrays.copyOfRange(deviceData, device_Startindex + 1, device_Endindex + 4);//28 to 31
        battery_voltage = ByteBuffer.wrap(batteryVoltage).getFloat();
        device_Startindex += batteryVoltage.length;//31
        device_Endindex += batteryVoltage.length;//32

    }

    private void getGPS_CoOrdinate(byte[] deviceData) {

        int GPSCoOrdinatelen = deviceData[device_Startindex + 1];
        byte[] GPScordinate_data = Arrays.copyOfRange(deviceData, device_Startindex + 2, (device_Endindex + 1) + GPSCoOrdinatelen);//33 to 57
        GPS_CoOrdinate = new String(GPScordinate_data, StandardCharsets.UTF_8);
        Log.d(TAG, "GPSCoordinate String:" + GPS_CoOrdinate);

        device_Startindex += GPScordinate_data.length + 1;//56
        device_Endindex += GPScordinate_data.length + 1;//58

    }

    private void getTimeZone(byte[] deviceData) {

        int TimeZonelen = deviceData[device_Endindex];//3
        byte[] TimeZone_data = Arrays.copyOfRange(deviceData, device_Startindex + 2, (device_Endindex + 1) + TimeZonelen);//59 to 61
        str_TimeZone = new String(TimeZone_data, StandardCharsets.UTF_8);
        Log.d(TAG, "Time Zone String:" + str_TimeZone);

    }

    private void updateUI_deviceDetails() {

        txtStationName.setText(stationName);
        txtSUIData.setText(hex_SUI);
        txtStationTime.setText(station_localTimeZone);
        txtHWVer.setText(cur_HardwareVersion);
        txtFWVer.setText(cur_FirmwareVersion);
        txtGPSCordinate.setText(GPS_CoOrdinate);
        txtBattery_volt.setText(Float.toString(battery_voltage));
        txtTimeZone.setText(str_TimeZone);


    }


    // end Region Private Methods


}
