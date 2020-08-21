package com.application.product.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.application.product.myapplication.ET_Command.TIO_DEFAULT_UART_DATA_SIZE;
import static com.application.product.myapplication.ET_Command.mgConnection;

// Tempaoary Override JSON

public class TempoaryOverrideJson extends AppCompatActivity implements TIOConnectionCallback, AdapterView.OnItemSelectedListener {

    //region variables

    RadioButton RadioButtonNine, RadioButtonTen;
    EditText edit_TimeInsecs, edit_overrideJson_data;
    Button btnOverrideJson;
    Spinner mspinner;


    private static final String TAG = "Temporary Override";
    private TIOPeripheral mPeripheral;

    private static int mChannel = 9;
    private int TOTAL_FILE_PACKET = 0;
    private int TOTAL_FILE_SIZE = 0;
    private int pktSeqNum = 0;
    private static int j = 1;
    private static final int PACKET_SIZE = 128;
    private boolean isEmptyData;
    public static boolean isSetConfigAckReceived;
    public static boolean isAdditionalPktadded;
    private byte configFileDataPkt[] = {0};
    private byte subpacketdata[] = {0};
    public byte configbytedata[] = {0};
    private Context _context;
    private String filename = "";

   public TempoaryOverrideJson() {
        mgConnection.setListener(this);
    }


    // end Region variables

    // UI event handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temporary_override);
        setupActionBar();
        setContentViews();
        getconnection();
        setSpinner();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    startActivity(new Intent(TempoaryOverrideJson.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    public void onOverRideJsonPressed(View Sender) throws IOException {
        Log.d(TAG, "onOverRideJsonPressed");
        //Get the Data
        String configFile = edit_overrideJson_data.getText().toString();
        int configdatalen = configFile.getBytes().length;
        int packetcount = getPacketCount(configdatalen);
        if(isValidFilesize()){
            Toast.makeText(_context, "File Size exceeded more than 600 bytes!", Toast.LENGTH_SHORT).show();
        }
        else {
            //Send reauest pkt
            byte[] startTemporaryOveridepkt = prepareTemporaryOverRidePkt(packetcount, configdatalen);
            mgConnection.transmit(startTemporaryOveridepkt);//Send RealTime Sensor request pkt
            isSetConfigAckReceived = false;
            Log.d(TAG, " Temporary OverRide Start Pkt sent:" + Arrays.toString(startTemporaryOveridepkt));//
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }


    //End UI event Handlers

    // Private Methods


    private void setContentViews() {
        btnOverrideJson = (Button) findViewById(R.id.btnOverrideJSON);
        edit_overrideJson_data = (EditText) findViewById(R.id.overideJSONData);
        edit_TimeInsecs = (EditText) findViewById(R.id.timeoutInsecs);
        RadioButtonNine = (RadioButton) findViewById(R.id.btnnine);
        RadioButtonTen = (RadioButton) findViewById(R.id.btnten);
        mspinner = (Spinner) findViewById(R.id.spinner_jsonfile);


        _context = getApplicationContext();
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
            case R.id.btnnine:
                if (checked)
                    mChannel = 9;
                break;
            case R.id.btnten:
                if (checked)
                    mChannel = 10;
                break;
        }
    }

    private byte[] prepareTemporaryOverRidePkt(int packetcount, int filelength) {
        int timeout = Integer.parseInt(edit_TimeInsecs.getText().toString());
        byte[] startTemporaryOverridePkt = new byte[TIO_DEFAULT_UART_DATA_SIZE];

        int temporaryOverrideLen = 14;

        startTemporaryOverridePkt[0] = ET_Command.CMD_START_TEMPORARY_OVERRIDE;//8
        //Total Length
        startTemporaryOverridePkt[1] = 0;
        startTemporaryOverridePkt[2] = (byte) (temporaryOverrideLen);//14

        startTemporaryOverridePkt[3] = ET_Command.CMD_START_TEMPORARY_OVERRIDE_SUBCMDID;//01
        //Total File Pkt
        startTemporaryOverridePkt[4] = (byte) ((packetcount >> 8) & 0XFF);//0
        startTemporaryOverridePkt[5] = (byte) (packetcount & 0XFF);//4
        //Total File Size
        startTemporaryOverridePkt[6] = (byte) ((filelength & 0XFF000000) >> 24);//0
        startTemporaryOverridePkt[7] = (byte) ((filelength & 0X00FF0000) >> 16);//0
        startTemporaryOverridePkt[8] = (byte) ((filelength & 0X0000FF00) >> 8);//22
        startTemporaryOverridePkt[9] = (byte) ((filelength & 0X000000FF) >> 0);//84

        startTemporaryOverridePkt[10] = (byte) mChannel;
        startTemporaryOverridePkt[11] = (byte) ((timeout >> 8) & 0XFF);
        startTemporaryOverridePkt[12] = (byte) (timeout & 0XFF);
        //
        //File version
        startTemporaryOverridePkt[13] = 0;
        startTemporaryOverridePkt[14] = 0;

        //CRC of File
        startTemporaryOverridePkt[15] = 0;
        startTemporaryOverridePkt[16] = 0;

        return startTemporaryOverridePkt;
    }

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private void processResponseData(byte[] responseData) {

        int respCmdId = responseData[0];
        int respSubCmdId = responseData[3];

        Log.d(TAG, "Cmdid in response-" + respCmdId);
        Log.d(TAG, "SubCmdid in response-" + respSubCmdId);

        //Receive ACK  based on CMDId

        if (respCmdId == ET_Command.CMD_START_TEMPORARY_OVERRIDE)//08
        {
            if (respSubCmdId == ET_Command.CMD_START_TEMPORARY_OVERRIDE_ACK_SUBCMDID)//02
            {
                isSetConfigAckReceived = true;
                Log.d(TAG, "Received Temporary  Start config  ACK" + Arrays.toString(responseData));
                processFiledata();

            } else if (respSubCmdId == ET_Command.CMD_START_TEMPORARY_OVERRIDE_ACK_DATA_PKT_SUBCMDID)// 04
            {

                isSetConfigAckReceived = true;
                Log.d(TAG, "Received Temporary data ACK" + Arrays.toString(responseData));
                processFiledata();
                if (isLastPacket()) {
                    validateACKStatus(responseData);
                }

            } else {
                Log.d(TAG, "Warning- Invalid sub command ID" + respSubCmdId);
            }
        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
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

    private boolean isLastPacket() {
        return pktSeqNum == TOTAL_FILE_PACKET;
    }

    private boolean isValidFilesize() {

        if (TOTAL_FILE_SIZE == 600)
            return true;
        else
            return false;
    }

    private void processFiledata() {
        Log.d(TAG, "Process file data");
        String configurationdata = edit_overrideJson_data.getText().toString();
        if (!isLastPacket()) {
            configbytedata = preparefileData(configurationdata);
            Log.d(TAG, "config byte data" + Arrays.toString(configbytedata));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Inside background  thread!");
                        mgConnection.transmit(configbytedata);
                        Log.d(TAG, "Inside background  thread after transmit!");
                        isSetConfigAckReceived = false;

                    } catch (Exception e) {
                        Log.e(TAG, "Warning- File transfer failed !");
                    }
                }
            }, 50);
        } else {

            Log.d(TAG, "File Transfer");
        }

    }

    private byte[] preparefileData(String configData) {
        Log.d(TAG, "Prepare file data");

        subpacketdata = new byte[150];
        configbytedata = configData.getBytes(StandardCharsets.UTF_8);//File data to bytes
        Log.d(TAG, "Config byte data length----" + configbytedata.length);//403(for pulse)
        Log.d(TAG, "Whole certificatedata----" + Arrays.toString(configbytedata));
        Log.d(TAG, "TOTAL_FILE_PACKET---" + TOTAL_FILE_PACKET);//4

        if (j < configbytedata.length) {
            if (isSetConfigAckReceived) {
                pktSeqNum++;

                if (isLastPacketofSetConfig()) {
                    subpacketdata = Arrays.copyOfRange(configbytedata, j - 1, configbytedata.length);//To get the length of last pkt from whole data
                } else {
                    Log.d(TAG, "Whole Data----" + configbytedata.length);
                    subpacketdata = Arrays.copyOfRange(configbytedata, j - 1, j + 127);//Individual pkt
                    j += 128;
                }
                Log.d(TAG, "SeqNum--" + pktSeqNum);
                //Total length of the packet
                int subpacketlength = subpacketdata.length;//128
                Log.d(TAG, "sub pakt Length:" + subpacketlength);

                int configFileTotlength = subpacketlength + 3;//131
                Log.d(TAG, "config file single pkt legth---" + configFileTotlength);//131
                configFileDataPkt = new byte[configFileTotlength + 6];

                //File Packet Structure
                configFileDataPkt[0] = ET_Command.CMD_START_TEMPORARY_OVERRIDE;//08
                //Total Length
                configFileDataPkt[1] = (byte) ((configFileTotlength >> 8) & 0XFF);//0
                configFileDataPkt[2] = (byte) (configFileTotlength & 0XFF);//131

                configFileDataPkt[3] = ET_Command.CMD_START_TEMPORARY_OVERRIDE_DATA_PKT_SUBCMDID;//3
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

    private void setSpinner() {

        mspinner.setOnItemSelectedListener(this);
        List commands = new ArrayList();
        // Spinner Drop down elements
        commands.add("Select Command");
        commands.add("override1_pulse");
        commands.add("override2_station");
        commands.add("override3_power");
        commands.add("override4_llac");
        commands.add("override5_sdi12");
        commands.add("override6_analog");
        commands.add("override7_debug");
        commands.add("override8_serialpassive");
        commands.add("override9_serialactive");
        commands.add("override10_modbus");


        // Creating & Initializing an ArrayAdapter for spinner
        final ArrayAdapter dataAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, commands) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        mspinner.setAdapter(dataAdapter);
        dataAdapter.notifyDataSetChanged();


    }

    private void getConfigFilefromDownload() {
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File configFile = new File(root + "/Apply_Override_Testing");
            if (configFile.exists()) {
                Log.d(TAG, "Fetch file");
                //Get the text file
                File file = new File(configFile, filename + ".txt");
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

    private void convertStringToJSON_updateUI(String str_fileData) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject json_configData = (JSONObject) parser.parse(str_fileData);//Parse String to  JSON

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json_configData);//Print JSON Data as Pretty-print
        edit_overrideJson_data.setText(prettyJson);//Update the Edit Text
    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[4];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            BleAction.showToast("JSON file overrided !");
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            BleAction.showToast("JSON file overrided Failed :(");
        } else {
            BleAction.showToast("Invalid Response !");
        }
    }


    //Peripheral CallBacks

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        filename = parent.getItemAtPosition(position).toString();
        getConfigFilefromDownload();


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    //End Peripheral CallBacks
}
