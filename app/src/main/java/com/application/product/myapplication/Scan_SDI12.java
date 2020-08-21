package com.application.product.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.application.product.myapplication.ET_Command.mgConnection;

//Scan SDI_12 operation
public class Scan_SDI12 extends AppCompatActivity implements TIOConnectionCallback {

    // region Memeber variables

    RadioButton RadioButtonYes, RadioButtonNo;
    Button btnScanSDI_12;
    EditText display_SDI12resp;
    ProgressDialog myDialog;

    private TIOPeripheral mPeripheral;
    private CommonProcessResponse commonProcessResponse;//Non activity class Obj
    private static final String TAG = "Scan SDI-12";

    private int TOTAL_FILE_SIZE = 0;
    private int TOTAL_FILE_PACKET = 0;
    private int CONNECTED_SENSOR_CNT = 0;
    private int ScanSDI_index = 0;
    private int conSenIndex = 0;
    private Handler handler;
    private Context _context;
    private byte ScanSDI_12[] = {0};
    private byte Merge_ScanSDI_12[] = new byte[1024];
    private byte Single_ScanSDI_12[] = new byte[1024];
    private static int mChannel = 1;
    private int index = 0;
    private static int sensorCount = 0;
    private static int progressstatus = 0;
    private static int individualsensorlen = 0;
    private int expectedSeqNum = 1;
    private boolean isEmptyData;
    private boolean isEmptySDI12Data;


    // End region

    //Constructor
    public Scan_SDI12() {
        mgConnection.setListener(this);
        commonProcessResponse = new CommonProcessResponse();
    }

    // End Constructor

    // Peripheral Call back methods

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
                commonProcessResponse.validateRecvdData(cdata, Scan_SDI12.this);
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
    // End peripheral Call back methods

    // Private methods

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private byte[] prepareScanSDI_12Req(int mChannel) {
        byte[] ScanSDI_12ReqPkt = new byte[5];
        int Scan_SDI_12len = 0;

        ScanSDI_12ReqPkt[0] = ET_Command.CMD_SCAN_SDI12_REQUEST_PACKET;//04
        ScanSDI_12ReqPkt[1] = (byte) Scan_SDI_12len;
        ScanSDI_12ReqPkt[2] = (byte) (Scan_SDI_12len + 2);//Append the length of channel & SubCMDID
        ScanSDI_12ReqPkt[3] = ET_Command.CMD_SCAN_SDI12_REQUEST_SUBCMD_ID;//01
        ScanSDI_12ReqPkt[4] = (byte) mChannel;//1 or 2

        return ScanSDI_12ReqPkt;
    }

    public void processResponseData(byte[] responseData) throws IOException {

        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];

        if (respCmdId == ET_Command.CMD_SCAN_SDI12_REQUEST_PACKET)     //04
        {
            if (respSubCmdId == ET_Command.CMD_SCAN_SDI12_PROGRESS_ACK_SUBCMD_ID)//02
            {
                myDialog.show();
                ShowProgressDialog();
                sensorCount = responseData[4];//get SensorNum queried
                Log.d(TAG, "Scan SDI-12 progress ACK" + Arrays.toString(responseData));//[4, 0, 2, 2, 7]//for 7th sensor

            } else if (respSubCmdId == ET_Command.CMD_SCAN_SDI12_INFO_PACKET_SUBCMD_ID)//03
            {
                Log.d(TAG, "Scan SDI-12 Info Packet" + Arrays.toString(responseData));//[4, 0, 8, 3, 0, 1, 0, 0, 0, 34, 1]
                getScanSDI_12Pkt_Size_count(responseData);//Get pktcnt and pktsize
                prepare_send_ScanSDI_12_InfoPktACK();//send ACK

            } else if (respSubCmdId == ET_Command.CMD_SCAN_SDI12_DATA_PACKET_SUBCMD_ID)//05
            {
                Log.d(TAG, "Scan SDI-12 data packet" + Arrays.toString(responseData));
                recvScanSDI_12DataAndSendACK(responseData);
            }
            else {
                Log.d(TAG, "Invalid Command Id" + respCmdId);
            }
        }


    }

    private void getScanSDI_12Pkt_Size_count(byte[] respData) {

        TOTAL_FILE_PACKET = (byte) (respData[4] | respData[5]);
        Log.d(TAG, "TOTAL_FILE_PKT=" + TOTAL_FILE_PACKET);//2

        byte[] SDI_12InfoSize = Arrays.copyOfRange(respData, ScanSDI_index + 6, ScanSDI_index + 10);
        TOTAL_FILE_SIZE = ByteBuffer.wrap(SDI_12InfoSize).getInt();
        Log.d(TAG, "SDI_12 Info File Size  :" + TOTAL_FILE_SIZE);//34

        CONNECTED_SENSOR_CNT = (byte) respData[10];
        Log.d(TAG, "Connected Sensor Count:" + CONNECTED_SENSOR_CNT);//5

    }


    private void prepare_send_ScanSDI_12_InfoPktACK() throws IOException {

        byte[] ScanSDI_12InfoACK = new byte[5];
        int InfoPktlen = 0;
        ScanSDI_12InfoACK[0] = ET_Command.CMD_SCAN_SDI12_REQUEST_PACKET;//04
        ScanSDI_12InfoACK[1] = (byte) InfoPktlen;//0
        ScanSDI_12InfoACK[2] = 2;
        ScanSDI_12InfoACK[3] = ET_Command.CMD_SCAN_SDI12_INFO_PACKET_ACK_SUBCMD_ID;//04
        ScanSDI_12InfoACK[4] = 0;

        mgConnection.transmit(ScanSDI_12InfoACK);
        Log.d(TAG, "send_ScanSDI_12_InfoPktACK-" + Arrays.toString(ScanSDI_12InfoACK));//[4, 0, 2, 4, 0]

    }

    private void recvScanSDI_12DataAndSendACK(byte[] Scan_SDI12Data) throws IOException {
        isEmptySDI12Data = isValidData(Scan_SDI12Data);
        int respSeqNum = Scan_SDI12Data[5];

        if (isEmptySDI12Data) {
            Log.d(TAG, "Warning - Received Empty ScanSDI_12 Data");
            return;
        } else {
            iterateScanSDI_12Data(Scan_SDI12Data);
            merge_sendSDI_12ACK_getSingleSensor(respSeqNum);
        }
    }

    private void iterateScanSDI_12Data(byte[] Scan_SDI12Data) {

        ScanSDI_12 = new byte[Scan_SDI12Data.length - 6];
        Log.d(TAG, "ScanSDI_12.length:" + ScanSDI_12.length);
        for (int i = 6; i < Scan_SDI12Data.length; i++)//iterate from the data excluding  Metadata(index(6))
        {
            ScanSDI_12[i - 6] = Scan_SDI12Data[i];
        }
        Log.d(TAG, " ScanSDI_12 after extracting :" + Arrays.toString(ScanSDI_12));
    }

    private void merge_sendSDI_12ACK_getSingleSensor(int respSeqNum) throws IOException {

        Log.d(TAG, "SEQNUMMMM-" + respSeqNum);
        if (respSeqNum == expectedSeqNum) {
            mergeSensorData();//Merge all the recvd Sensor data
            prepare_sendSDI12DataACK(respSeqNum);//Send ACK to received SDI12 Data

            if (respSeqNum == TOTAL_FILE_PACKET) {
                getIndividualSensor_Data();//Get individual sensor data
                expectedSeqNum = 0;
                ScanSDI_index = 0;
                index = 0;
            }
            expectedSeqNum++;
        }
    }

    private void mergeSensorData() {
        //Merge all the recvd sensor data into  Merge_ScanSDI_12 array (excluding Metadata)
        System.arraycopy(ScanSDI_12, 0, Merge_ScanSDI_12, ScanSDI_index, ScanSDI_12.length);
        ScanSDI_index += ScanSDI_12.length;
        Log.d(TAG, "gatherSensorData :" + Arrays.toString(Merge_ScanSDI_12));
    }

    private void getIndividualSensor_Data() {
        //Get individual sensor data from  Merge_ScanSDI_12 to Single_ScanSDI_12 excluding len of single sensor
        for (conSenIndex = 1; conSenIndex <= CONNECTED_SENSOR_CNT; conSenIndex++) {
            individualsensorlen = Merge_ScanSDI_12[index];
            System.arraycopy(Merge_ScanSDI_12, index + 1, Single_ScanSDI_12, 0, individualsensorlen);
            index += individualsensorlen + 1;
            //Update the UI
            updateUI_ScanSDI12();
        }
    }

    private void updateUI_ScanSDI12() {
        //Update the UI for each sensor recvd
        String SDI12Response = new String(Single_ScanSDI_12, StandardCharsets.UTF_8);//Conversion to string
        display_SDI12resp.append("Sensor :  " + conSenIndex + "\n" + SDI12Response + "\n");//Update the Edit Text
        Toast.makeText(_context, "SDI_12 Resp Success!", Toast.LENGTH_SHORT).show();
    }

    private void prepare_sendSDI12DataACK(int pktSeqNum) throws IOException {

        byte sendSDI12DataACK[] = new byte[7];
        int SDI12Datalen = 4;
        sendSDI12DataACK[0] = ET_Command.CMD_SCAN_SDI12_REQUEST_PACKET;//04
        sendSDI12DataACK[1] = 0;
        sendSDI12DataACK[2] = (byte) SDI12Datalen;//Total Length
        sendSDI12DataACK[3] = ET_Command.CMD_SCAN_SDI12_DATA_PACKET_ACK_SUBCMD_ID;//06
        if (pktSeqNum == expectedSeqNum) {
            sendSDI12DataACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Success.statusVal;//0
        } else {
            sendSDI12DataACK[4] = (byte) ET_Command.ACK_FILE_STATUS.Failure.statusVal;//1
        }
        sendSDI12DataACK[5] = 0;
        sendSDI12DataACK[6] = (byte) pktSeqNum;

        mgConnection.transmit(sendSDI12DataACK);//Send config  data ACK
        Log.d(TAG, "Send  sendSDI12Data ACK -" + Arrays.toString(sendSDI12DataACK));//
    }

    public void ShowProgressDialog() {
        progressstatus = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (progressstatus <= sensorCount) {
                    progressstatus += 1;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myDialog.setProgress(sensorCount);
                            if (progressstatus == sensorCount) {
                                myDialog.setProgress(100);
                                myDialog.setMax(sensorCount);
                                myDialog.dismiss();
                            }
                        }
                    });
                }
            }
        }).start();

    }


    // end Private methods

    // UI event handlers

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_sdi_12);
        setContentViews();
        setupActionBar();
        getconnection();
    }

    private void setContentViews() {
        display_SDI12resp = (EditText) findViewById(R.id.displaySDI12);
        btnScanSDI_12 = (Button) findViewById(R.id.btnscanSDI12);
        RadioButtonYes = (RadioButton) findViewById(R.id.btnyes);
        RadioButtonNo = (RadioButton) findViewById(R.id.btnno);
        _context = getApplicationContext();
        handler = new Handler();
        display_SDI12resp.setShowSoftInputOnFocus(false);//Hide EditText keyboard
        myDialog = CommonDialogs.showProgressDialogPercent(this, "Fetching Sensor Count...");

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
                    startActivity(new Intent(Scan_SDI12.this, BleAction.class));
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
            case R.id.btnyes:
                if (checked)
                    mChannel = 1;
                break;
            case R.id.btnno:
                if (checked)
                    mChannel = 2;
                break;
        }
    }

    public void onScanSDI12Pressed(View sender) throws IOException {

        Log.d(TAG, "onScanSDI12Pressed");

        byte[] sendScanSDI12Reqest = prepareScanSDI_12Req(mChannel);
        Log.d(TAG, "mChannel:" + mChannel);
        mgConnection.transmit(sendScanSDI12Reqest);//Send ScanSDI12 request pkt
        Log.d(TAG, "Scan SDI_12 Request packet Sent:" + Arrays.toString(sendScanSDI12Reqest));//[4, 0, 2, 1, 1]

    }
    // End UI event Handlers


}
