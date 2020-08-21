package com.application.product.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.EthiopicCalendar;
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
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


import static com.application.product.myapplication.ET_Command.mgConnection;

public class UploadConfig extends AppCompatActivity implements TIOConnectionCallback {

    // Region Member Variables

    Button btnuploadconfiguration, btneditconfiguration, btnpickfile;
    EditText editdisplayconfig;
    ProgressDialog myDialog;
    TextView txtfilename;


    private final long DELAY = 60000; // milliseconds

    //Start Configuration Data
    private byte configData[] = {0};
    private byte START_PKT_LENGTH = 11;

    private int TOTAL_FILE_PACKET = 0;
    private int TOTAL_FILE_SIZE = 0;
    private static final int PACKET_SIZE = 128;
    //File Data
    private int pktSeqNum = 0;
    private int resend_count = 0;
    private static int j = 1;

    private byte configFileDataPkt[] = {0};
    private byte subpacketdata[] = {0};
    private byte configbytedata[] = {0};

    public static boolean isSetConfigAckReceived;
    public static boolean isAdditionalPktadded;

    private static String TAG = "Upload Config";
    private static final int PICK_FILE_REQUEST = 1;
    private Context _context;
    private TIOPeripheral mPeripheral;
    private CountDownTask timerCntrl;
    private String fileName = "";


    // end Region

    // Constructor

    public UploadConfig() {
        mgConnection.setListener(this);
    }

    class CountDownTask extends TimerTask {
        private Timer timer = null;

        CountDownTask() {
            this.timer = new Timer(true);
        }

        public Timer getTimer() {
            return timer;
        }

        public void scheduleTimer() {
            timer.scheduleAtFixedRate(this, DELAY, DELAY);
            Log.d(TAG, "Timer Started");
        }

        @Override
        public void run() {
            int maxlimit = 3;
            Log.d(TAG, "Inside Timer ");
            if (isSetConfigAckReceived) {
                Log.d(TAG, "Inside Timer ACK Status" + isSetConfigAckReceived);
                try {
                    resend_count++;
                    if (resend_count > maxlimit) {
                        Log.d(TAG, "ACK not receive,Timeout Occured ");
                        resend_count = 0;
                        mgConnection.setListener(null);
                        timer.cancel();
                    } else {
                        mgConnection.transmit(configbytedata);
                        Log.d(TAG, "Resend Count" + resend_count);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopTimer() {
            timer.cancel();
        }
    }

    // region Peripheral callbacks

    @Override
    public void onConnected(TIOConnection tioConnection) {

    }

    @Override
    public void onConnectFailed(TIOConnection tioConnection, String s) {
        timerCntrl.stopTimer();
        Log.d(TAG, "Warning- Connection Failed");

    }

    @Override
    public void onDisconnected(TIOConnection tioConnection, String s) {
        timerCntrl.stopTimer();
        Log.d(TAG, "Warning- BLE Disconnected");

    }

    @Override
    public void onDataReceived(TIOConnection tioConnection, final byte[] cdata) {
        try {
            Log.d(TAG, "Warning - onDataReceived len " + cdata.length);
            Log.d(TAG, "Array Received" + Arrays.toString(cdata));

            if (cdata == null || cdata.length == 0) {
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
    // endregion callback

    // region UI Event handlers

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    mgConnection.setListener(null);
                    disposeResources();
                    startActivity(new Intent(UploadConfig.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_config);
        setContentViews();
        setupActionBar();
        checkStoragePermission();
        getconnection();

    }


    public void onSetConfigPressed(View sender) {

        Log.d(TAG, "onSetConfigButtonPressed");
        String configurationdata = editdisplayconfig.getText().toString();
        int configdatalen = configurationdata.getBytes().length;
        int packetcount = getPacketCount(configdatalen);
        Log.d(TAG, "Configdatalen=" + configdatalen + "pktcount=" + packetcount);//10798,85
        myDialog.show();
        try {
            Log.d(TAG, "Send start config packet");

            if (configurationdata != null) {
                byte Configuredata[] = configUpdateData(packetcount, configdatalen);
                mgConnection.transmit(Configuredata);//send data
                isSetConfigAckReceived = false;
            } else {
                Log.d(TAG, "Config data is empty");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Data transmit failed !");
        }
        Log.d(TAG, "Start config ended !");
    }

    public void onPickfilePressed(View sender) {
        btnuploadconfiguration.setVisibility(View.VISIBLE);
        btneditconfiguration.setVisibility(View.VISIBLE);
        showFileChooser();
    }

    // End region UI Event handlers

    // region Private methods

    private void setContentViews() {
        btnuploadconfiguration = (Button) findViewById(R.id.setconfig);
        btneditconfiguration = (Button) findViewById(R.id.editconfig);
        editdisplayconfig = (EditText) findViewById(R.id.displayconfigfromdevice);
        btnpickfile = (Button) findViewById(R.id.SelectFile);
        txtfilename = (TextView) findViewById(R.id.textfile);
        _context = getApplicationContext();
        editdisplayconfig.setShowSoftInputOnFocus(false);//Hide EditText keyboard
        myDialog = CommonDialogs.showProgressDialog(this, "Uploading Config changes...");

    }

    public void onEditConfigPressed(View sender) {
        editdisplayconfig.setShowSoftInputOnFocus(true);//Enable EditText keyboard
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

    private void getConfigFilefromFolder() {
        try {
            //Fetch file from downloads folder
            String root = Environment.getExternalStorageDirectory().toString();
            File configFile = new File(root + "/configFile");
            if (configFile.exists()) {
                Log.d(TAG, "Fetch file");
                //Get the text file
                File file = new File(configFile, fileName);
                //Read text from file
                StringBuilder configtext = new StringBuilder();

                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        configtext.append(line);
                        configtext.append('\n');
                    }
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
                String configText = configtext.toString();
                convertStringToJSON_updateUI(configText);//Parse the file to JSON

            } else {
                Toast.makeText(_context, "File Doesn't Exist", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
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


    private void convertStringToJSON_updateUI(String str_fileData) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject json_configData = (JSONObject) parser.parse(str_fileData);//Parse String to  JSON

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json_configData);//Print JSON Data as Pretty-print
        editdisplayconfig.setText(prettyJson);//Update the Edit Text
    }

    //Prepare Start Configuration data packet for SET  CONFIG
    private byte[] configUpdateData(int packetcount, int configdatalen) {

        configData = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];

        //Start Configuration Data Packet
        configData[0] = ET_Command.CMD_START_SET_CONFIG;//11
        //Total length
        configData[1] = 0;
        configData[2] = START_PKT_LENGTH;//11

        configData[3] = ET_Command.CMD_START_CONFIG_SUBCMD_ID;//01
        //Total File Packet
        configData[4] = (byte) ((packetcount >> 8) & 0XFF);//0
        configData[5] = (byte) (packetcount & 0XFF);//45

        //File Size
        configData[6] = (byte) ((configdatalen & 0XFF000000) >> 24);//0
        configData[7] = (byte) ((configdatalen & 0X00FF0000) >> 16);//0
        configData[8] = (byte) ((configdatalen & 0X0000FF00) >> 8);//22
        configData[9] = (byte) ((configdatalen & 0X000000FF) >> 0);//84

        //File version
        configData[10] = 0;
        configData[11] = 0;

        //CRC of File
        configData[12] = 0;
        configData[13] = 0;

        return configData;
    }

    //Prepare file data packet

    private byte[] preparefileData(String configData) {
        Log.d(TAG, "Prepare file data");


        configFileDataPkt = new byte[137];
        subpacketdata = new byte[150];

        configbytedata = configData.getBytes(StandardCharsets.UTF_8);//File data to bytes
        Log.d(TAG, "Config byte data length----" + configbytedata.length);//5716
        Log.d(TAG, "Whole certificatedata----" + Arrays.toString(configbytedata));
        Log.d(TAG, "TOTAL_FILE_PACKET---" + TOTAL_FILE_PACKET);//45
        Log.d(TAG, "J before loop " + j);
        if (j < configbytedata.length) {
            if (isSetConfigAckReceived) {
                pktSeqNum++;

                if (isLastPacketofSetConfig()) {
                    // int lastpktlength = certificatedata.length % 128;//last pkt
                    subpacketdata = Arrays.copyOfRange(configbytedata, j - 1, configbytedata.length);//To get the length of last pkt from whole data
                } else {
                    Log.d(TAG, "Whole Data----" + configbytedata.length);//5716
                    subpacketdata = Arrays.copyOfRange(configbytedata, j - 1, j + 127);//Individual pkt
                    j += 128;
                }
                Log.d(TAG, "SeqNum--" + pktSeqNum);
                //Total length of the packet
                int subpacketlength = subpacketdata.length;//128
                Log.d(TAG, "sub pakt Length:" + subpacketlength);

                int configFileTotlength = subpacketlength + 3;//131
                Log.d(TAG, "config file single pkt legth---" + configFileTotlength);//131

                //File Packet Structure
                configFileDataPkt[0] = ET_Command.CMD_START_SET_CONFIG;//11
                //Total Length
                configFileDataPkt[1] = (byte) ((configFileTotlength >> 8) & 0XFF);//0
                configFileDataPkt[2] = (byte) (configFileTotlength & 0XFF);//131

                configFileDataPkt[3] = ET_Command.CMD_CONFIG_DATA_PACKET_SUBCMD_ID;//3
                //Packet SequenceNO
                configFileDataPkt[4] = (byte) ((pktSeqNum >> 8) & 0XFF);//0
                configFileDataPkt[5] = (byte) (pktSeqNum & 0XFF);//1...

                for (int i = 0; i < subpacketlength; i++) {
                    configFileDataPkt[i + 6] = subpacketdata[i];
                }

            } else {
                Log.d(TAG, "Warning- Unable to create packet");
            }
        } else {
            Log.d(TAG, "Warning- Configuration missing");
        }
        return configFileDataPkt;

    }

    private void processFiledata() throws IOException {

        Log.d(TAG, "Process file data");
        String configurationdata = editdisplayconfig.getText().toString();
        if (configurationdata != null) {
            configbytedata = preparefileData(configurationdata);
            mgConnection.transmit(configbytedata);
            isSetConfigAckReceived = false;
            Log.d(TAG, "config byte data" + Arrays.toString(configbytedata));

        }
    }

    private void disposeResources() {
        Log.d(TAG, "Disposed");
        configFileDataPkt = new byte[0];
        subpacketdata = new byte[0];
        configbytedata = new byte[0];
        j = 1;
        pktSeqNum = 0;

    }

    private void resetValues() {
        txtfilename.setText("");
        editdisplayconfig.setText("");
        btnuploadconfiguration.setVisibility(View.GONE);
        btneditconfiguration.setVisibility(View.GONE);
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
            fileName = FilePath.substring(FilePath.lastIndexOf("/") + 1);
            txtfilename.setText(fileName);
            try {
                getConfigFilefromFolder();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }


    private void showAlertDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(UploadConfig.this, R.style.AlertDialogTheme).create();
        // Setting Dialog Title
        alertDialog.setTitle("Confirm Update");
        // Setting Dialog Message
        alertDialog.setMessage("Are you sure you want to save ConfigFile?");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            processApplyConfigChanges();//Save changes
                        } catch (IOException e) {
                            Log.d(TAG, "Apply Config failed");
                        }
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();

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

    private boolean isLastPacketofSetConfig() {
        return pktSeqNum == TOTAL_FILE_PACKET && isAdditionalPktadded == true;
    }


    private void processApplyConfigChanges() throws IOException {

        Log.d(TAG, "Inside processApplyConfigChanges");
        byte[] sendApplyConfigData = applyConfigurationChanges();
        mgConnection.transmit(sendApplyConfigData);//Send config  data
        Log.d(TAG, "Apply Config Data-" + Arrays.toString(sendApplyConfigData));

    }

    //Apply Configuration changes to the BLE device
    private byte[] applyConfigurationChanges() {

        byte applyConfigChangesPackt[] = new byte[3];
        int configDatalen = 0;

        applyConfigChangesPackt[0] = ET_Command.CMD_APPLY_CONFIG_CHANGES;//07
        applyConfigChangesPackt[1] = (byte) configDatalen;//0
        applyConfigChangesPackt[2] = (byte) configDatalen;//0

        return applyConfigChangesPackt;
    }

    private void processResponseData(byte[] responseData) throws IOException {

        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];

        Log.d(TAG, "Cmdid in response-" + respCmdId);
        Log.d(TAG, "SubCmdid in response-" + respSubCmdId);

        //Receive ACK  based on CMDId

        if (respCmdId == ET_Command.CMD_START_SET_CONFIG)//11
        {

            isSetConfigAckReceived = true;
            Log.d(TAG, "Received Start config  ACK" + Arrays.toString(responseData));
            processFiledata();
            int respSeqNum = responseData[6];
            if (respSeqNum == TOTAL_FILE_PACKET) {
                Log.d(TAG, "ACK of last packet received:" + respSeqNum);
                myDialog.dismiss();
                showAlertDialog();//To confirm the Config Changes to device.
            } else {
                Log.d(TAG, "Warning- Invalid sub command ID" + respSubCmdId);
            }
        } else if (respCmdId == ET_Command.CMD_APPLY_CONFIG_CHANGES)//07
        {
            Log.d(TAG, "Apply Configuration Changes ACK - " + Arrays.toString(responseData));// [7, 0, 1, 0]
            int ACKStatus = responseData[3];
            if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
                Toast.makeText(_context, "Configuration File Uploaded !", Toast.LENGTH_SHORT).show();
                disposeResources();
                resetValues();

            } else {
                Toast.makeText(_context, "Configuration File Uploading Failed :(", Toast.LENGTH_SHORT).show();

            }
        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
    }
}

// End Private method