package com.application.product.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.application.product.myapplication.ET_Command.mgConnection;

public class OTA_Ble extends AppCompatActivity implements TIOConnectionCallback {

    //  region private variables
    EditText edit_majorlevel, edit_minorlevel;
    Button btnSendOTA, btnpickFile;
    TextView txtfilename;

    private static final String TAG = "OTA_BLE";
    private static final int PICK_FILE_REQUEST = 1;

    private TIOPeripheral mPeripheral;
    private boolean isEmptyData;
    private int pktSeqNum = 0;
    private static int j = 1;
    private static final int PACKET_SIZE = 128;
    private int TOTAL_FILE_PACKET = 0;
    private int TOTAL_FILE_SIZE = 0;
    private int OTADatalen = 0;
    public static boolean isSetConfigAckReceived;
    public static boolean isAdditionalPktadded;
    private String filename = "";
    private byte OTADataPkt[] = {0};
    private byte subpacketdata[] = {0};
    public byte certificatedata[] = {0};
    private Context _context;
    private byte START_PKT_LENGTH = 11;
    ProgressDialog myDialog1;
    private byte[] OTAByteArr = {0};
    private Timer timer;
    private int retry_count = 0;


    public OTA_Ble() {
        mgConnection.setListener(this);
    }

    class CountDownTask extends TimerTask {

        @Override
        public void run() {
            stoptimertask();
            processFiledata();

        }
    }

    //  end region

    // private method
    private void getconnection() {
        Intent intent = getIntent();
        String address = intent.getStringExtra(TIOSample.PERIPHERAL_ID_NAME);
        // retrieve peripheral instance from TIOManager
        mPeripheral = TIOManager.getInstance().findPeripheral(address);
        if (mPeripheral != null) {
            Log.d(TAG, "Connection" + mgConnection);
        }

    }

    private void checkStoragePermission() {
        //App opens the Settings Page to enable read Storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            finish();
            startActivity(intent);
            return;
        }
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

    private void setContentViews() {
        btnSendOTA = (Button) findViewById(R.id.btnsendOTA);
        edit_majorlevel = (EditText) findViewById(R.id.majorlevel);
        edit_minorlevel = (EditText) findViewById(R.id.minorlevel);
        btnpickFile = (Button) findViewById(R.id.SelectFile);
        txtfilename = (TextView) findViewById(R.id.textfile);
        _context = getApplicationContext();

        myDialog1 = CommonDialogs.showProgressDialog(this, "Uploading...");

    }


    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    public void processResponseData(byte[] responseData) throws IOException {
        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];
        if (respCmdId == ET_Command.CMD_OTA_CMDID)//13
        {
            if (respSubCmdId == ET_Command.CMD_START_CONFIG_ACK_SUBCMD_ID)//02
            {
                isSetConfigAckReceived = true;//[13, 0, 3, 2, 2, 0]
                processFiledata();

            } else if (respSubCmdId == ET_Command.CMD_CONFIG_DATA_PACKET_ACK_SUBCMD_ID)//04
            {
                Log.d(TAG, "Received Temporary  Start config  ACK" + Arrays.toString(responseData));
                validateACK_NACK(responseData);
                if (isLastPacket()) {
                    validateACKStatus(responseData);
                }
            }

        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
    }

    private void validateACK_NACK(byte[] responseData) {
        int ACKStatus = responseData[4];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            retry_count = 0;
            isSetConfigAckReceived = true;
            stoptimertask();
            processFiledata();

        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            isSetConfigAckReceived = false;
            stoptimertask();
            processFiledata();
        } else {
            BleAction.showToast("Invalid Response ACK_NACK !");
        }
    }

    private byte[] preparefileData() {
        Log.d(TAG, "Prepare file data");

        subpacketdata = new byte[150];
        Log.d(TAG, "Config byte data length----" + OTAByteArr.length);
        Log.d(TAG, "Whole certificatedata----" + Arrays.toString(OTAByteArr));
        Log.d(TAG, "TOTAL_FILE_PACKET---" + TOTAL_FILE_PACKET);//1980

        if (j < OTAByteArr.length) {
            if (isSetConfigAckReceived) {
                pktSeqNum++;
                if (isLastPacketofSetConfig()) {
                    subpacketdata = Arrays.copyOfRange(OTAByteArr, j - 1, OTAByteArr.length);//To get the length of last pkt from whole data
                } else {
                    Log.d(TAG, "Whole Data----" + OTAByteArr.length);
                    subpacketdata = Arrays.copyOfRange(OTAByteArr, j - 1, j + 127);//Individual pkt
                    j += 128;
                }
                Log.d(TAG, "SeqNum--" + pktSeqNum);
                //Total length of the packet
                int subpacketlength = subpacketdata.length;//128
                Log.d(TAG, "sub pakt Length:" + subpacketlength);

                int configFileTotlength = subpacketlength + 3;//131
                Log.d(TAG, "config file single pkt legth---" + configFileTotlength);//131
                OTADataPkt = new byte[configFileTotlength + 6];

                //File Packet Structure
                OTADataPkt[0] = (byte) ET_Command.CMD_OTA_CMDID;//08
                //Total Length
                OTADataPkt[1] = (byte) ((configFileTotlength >> 8) & 0XFF);//0
                OTADataPkt[2] = (byte) (configFileTotlength & 0XFF);//131

                OTADataPkt[3] = ET_Command.CMD_START_TEMPORARY_OVERRIDE_DATA_PKT_SUBCMDID;//3
                //Packet SequenceNO
                OTADataPkt[4] = (byte) ((pktSeqNum >> 8) & 0XFF);//0
                OTADataPkt[5] = (byte) (pktSeqNum & 0XFF);//1...

                for (int i = 0; i < subpacketlength; i++) {
                    OTADataPkt[i + 6] = subpacketdata[i];
                }
                String subpktdata = new String(subpacketdata, StandardCharsets.UTF_8);
                Log.d(TAG, "Subpktdata:" + subpktdata);
            } else {
                Log.d(TAG, "Warning- Unable to create packet");
            }
        } else {
            Log.d(TAG, "Warning-File missing");
        }

        return OTADataPkt;

    }


    private void processFiledata() {

        int maxcount = 4;
        if (!isLastPacket()) {
            Log.d(TAG, "isSetConfigAck" + isSetConfigAckReceived);

            if (isSetConfigAckReceived == false && retry_count <= maxcount) {

                retry_count++;
                if (retry_count > maxcount) {
                    Log.d(TAG, "Exceeded Max attempts..Transfer Failed!");
                }
            } else {
                certificatedata = preparefileData();
            }
            try {
                mgConnection.transmit(certificatedata);
                isSetConfigAckReceived = false;
                startTimer();
            } catch (IOException e) {
                Log.d(TAG, "Transmit Failed!");
            }

        } else {
            Log.d(TAG, "File Transfer completed!");
        }
    }

    private boolean isLastPacketofSetConfig() {
        return pktSeqNum == TOTAL_FILE_PACKET && isAdditionalPktadded == true;
    }

    private void startTimer() {
        timer = new Timer(true);
        timer.schedule(new CountDownTask(), 1000, 1000);
        Log.d(TAG, "Start timer");
    }


    private void validateACKStatus(byte[] responseData) throws IOException {
        int ACKStatus = responseData[4];
        int CRCStatus = responseData[7];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal && CRCStatus == ET_Command.CRC_ACK_FILE_STATUS.Success.statusVal) {
            BleAction.showToast("CRC matched ! OTA file Uploaded !");
            disposeResources();
            resetValues();
            myDialog1.dismiss();
            mgConnection.disconnect();
            navigateToDiscovery();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal && CRCStatus == ET_Command.CRC_ACK_FILE_STATUS.Failure.statusVal) {
            BleAction.showToast("CRC not matched ! OTA File Upload Failed :(");
        } else {
            // BleAction.showToast("Invalid Response !");
        }
    }

    private void navigateToDiscovery() throws IOException {

        Intent intent = new Intent(OTA_Ble.this, MainActivity.class);
        startActivity(intent);
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        Log.d(TAG, "Stop Timer");
        if (timer != null) {
            timer.cancel();
        }
    }

    private void resetValues() {
        edit_minorlevel.setText("");
        edit_majorlevel.setText("");
        txtfilename.setText("");

    }

    private void disposeResources() {
        Log.d(TAG, "Disposed");
        OTADataPkt = new byte[0];
        subpacketdata = new byte[0];
        certificatedata = new byte[0];
        j = 1;
        pktSeqNum = 0;


    }

    private boolean isLastPacket() {
        return pktSeqNum == TOTAL_FILE_PACKET;
    }


    private int getPacketCount(int configdatalen) {

        TOTAL_FILE_PACKET = configdatalen % PACKET_SIZE;

        if (TOTAL_FILE_PACKET == 0) {
            TOTAL_FILE_PACKET = configdatalen / PACKET_SIZE;
        } else {
            TOTAL_FILE_PACKET = (configdatalen / PACKET_SIZE) + 1;
            isAdditionalPktadded = true;
        }
        return TOTAL_FILE_PACKET;
    }


    private int getFwMajorver() {
        int majorlevel = Integer.parseInt(edit_majorlevel.getText().toString());
        return majorlevel;
    }

    private int getFwMinorver() {
        int minorlevel = Integer.parseInt(edit_minorlevel.getText().toString());
        return minorlevel;
    }


    private byte[] prepareStartOTAPkt(int packetcount, int OTAlength, int crcVal) {
        byte OTAData[] = new byte[14];

        int major = getFwMajorver();
        int minor = getFwMinorver();
        //Start Configuration Data Packet
        OTAData[0] = (byte) ET_Command.CMD_OTA_CMDID;//13
        //Total length
        OTAData[1] = 0;
        OTAData[2] = START_PKT_LENGTH;//11

        OTAData[3] = ET_Command.CMD_START_CONFIG_SUBCMD_ID;//01
        //Total File Packet
        OTAData[4] = (byte) ((packetcount >> 8) & 0XFF);//0
        OTAData[5] = (byte) (packetcount & 0XFF);//45

        //File Size
        OTAData[6] = (byte) ((OTAlength & 0XFF000000) >> 24);//0
        OTAData[7] = (byte) ((OTAlength & 0X00FF0000) >> 16);//0
        OTAData[8] = (byte) ((OTAlength & 0X0000FF00) >> 8);//22
        OTAData[9] = (byte) ((OTAlength & 0X000000FF) >> 0);//84

        //File version
        OTAData[10] = (byte) major;
        OTAData[11] = (byte) minor;

        //CRC of File
        OTAData[12] = (byte) ((crcVal >> 8) & 0XFF);
        OTAData[13] = (byte) (crcVal & 0XFF);

        return OTAData;
    }


    private int Calc_CRC_For_Totl_Payld(byte[] payload, int length) {
        // CRC Values
        int crc_buff = 0;
        int x16;
        int input;
        int bytes;
        int count;

        for (bytes = 0; bytes < length; bytes++) {
            input = payload[bytes];
            for (count = 0; count < 8; count++) {
                if ((crc_buff & 0x0001) != (input & 0x01))
                    x16 = 0x8408;
                else
                    x16 = 0x0000;
                // shift crc buffer
                crc_buff = crc_buff >> 1;
                // XOR in the x16 value
                crc_buff ^= x16;
                // shift input for next iteration
                input = input >> 1;

            }

        }
        System.out.println("CRC16-CCITT for Total Payld= " + Integer.toHexString(crc_buff));//f3b1
        return crc_buff;
    }

    //method to show file chooser
    private void showFileChooser() {

        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select file"), PICK_FILE_REQUEST);
    }

    //handling the file chooser activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            String FilePath = data.getData().getPath();
            filename = FilePath.substring(FilePath.lastIndexOf("/") + 1);
            txtfilename.setText(filename);
            try {
                getBinaryOTAFileFromFolder();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    private void getBinaryOTAFileFromFolder() throws IOException {

        String root = Environment.getExternalStorageDirectory().toString();
        File OTAFile = new File(root + "/OTA_File");
        if (OTAFile.exists()) {
            File file = new File(OTAFile, filename);
            InputStream insputStream = new FileInputStream(file);
            OTADatalen = (int) file.length();
            OTAByteArr = new byte[OTADatalen];
            insputStream.read(OTAByteArr);
            insputStream.close();
        } else {
            Toast.makeText(_context, "File Doesn't Exist", Toast.LENGTH_SHORT).show();

        }

    }


    // end private method

    // UI event Handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_ble);
        setupActionBar();
        setContentViews();
        checkStoragePermission();
        getconnection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    startActivity(new Intent(OTA_Ble.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }

    public void onUploadOTAFilePressed(View Sender) throws IOException {
        Log.d(TAG, "onUploadOTAFilePressed");
        getBinaryOTAFileFromFolder();
        int crc_buffVal = Calc_CRC_For_Totl_Payld(OTAByteArr, OTADatalen);
        int packetcount = getPacketCount(OTADatalen);
        //Send reauest pkt
        byte[] sendStartOTAPkt = prepareStartOTAPkt(packetcount, OTADatalen, crc_buffVal);
        mgConnection.transmit(sendStartOTAPkt);//Send RealTime Sensor request pkt
        myDialog1.show();
        // ShowProgressDialog();
        Log.d(TAG, " Start OTA  Pkt sent:" + Arrays.toString(sendStartOTAPkt));//

    }

    public void onPickfilePressed(View sender) {
        showFileChooser();
    }


    // End UI event Handlers


    //Peripheral Call backs

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

    // End Peripheral Callback
}
