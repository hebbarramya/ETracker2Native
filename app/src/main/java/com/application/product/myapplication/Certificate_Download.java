package com.application.product.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

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

import static com.application.product.myapplication.ET_Command.mgConnection;

public class Certificate_Download extends AppCompatActivity implements TIOConnectionCallback, AdapterView.OnItemSelectedListener {

    // region Variables

    EditText edit_certificateData;
    Button btnCertificateUpload, btnpickfile;
    Spinner mspinnerCertificate;
    TextView txtfilename;
    ProgressDialog myDialog;


    private static final String TAG = "Certificate Download";
    private static final int PICK_FILE_REQUEST = 1;
    private TIOPeripheral mPeripheral;

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
    public byte certificatedata[] = {0};
    private Context _context;
    private String fileName = "";
    private static int certificate_SubCMDID = 0;
    private byte START_PKT_LENGTH = 11;

    private Timer timer;
    private int retry_count = 0;

    public Certificate_Download() {
        mgConnection.setListener(this);

    }

    class CountDownTask extends TimerTask {

        @Override
        public void run() {
            stoptimertask();
            processFiledata();

        }
    }


    // end Region variables

    // UI Event Handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.certificate_download);

        setupActionBar();
        setContentViews();
        checkStoragePermission();
        getconnection();
        setCertificateSpinner();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                try {
                    Log.d(TAG, "Navigate");
                    disposeResources();
                    stoptimertask();
                    startActivity(new Intent(Certificate_Download.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    public void onCertificateDownloadPressed(View Sender) throws IOException {
        Log.d(TAG, "onCertificateDownloadPressed");
        //startTimer();
        //Get the Data
        String configFile = edit_certificateData.getText().toString();

        int configdatalen = configFile.getBytes().length;
        int packetcount = getPacketCount(configdatalen);
        if (isValidFilesize()) {
            Toast.makeText(_context, "File Size exceeded more than 4096 bytes!", Toast.LENGTH_SHORT).show();
        } else {
            myDialog.show();
            //Send reauest pkt
            byte[] startTempoaryCertificatePkt = prepareStartCertificatePkt(packetcount, configdatalen);
            mgConnection.transmit(startTempoaryCertificatePkt);//Send RealTime Sensor request pkt
            Log.d(TAG, " Certificate Download Pkt sent:" + Arrays.toString(startTempoaryCertificatePkt));//
        }
    }

    public void onPickfilePressed(View sender) {
        btnCertificateUpload.setVisibility(View.VISIBLE);
        showFileChooser();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button

    }

    // End UI Event Handlers


    // Private method
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
        btnCertificateUpload = (Button) findViewById(R.id.btnUploadCertificate);
        btnpickfile = (Button) findViewById(R.id.SelectFile);
        edit_certificateData = (EditText) findViewById(R.id.certificateData);
        mspinnerCertificate = (Spinner) findViewById(R.id.spinner_certificatedownload);
        _context = getApplicationContext();
        edit_certificateData.setShowSoftInputOnFocus(false);//Hide EditText keyboard
        txtfilename = (TextView) findViewById(R.id.textfile);
        myDialog = CommonDialogs.showProgressDialog(this, "Uploading...");
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

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }


    private void setCertificateSpinner() {


        mspinnerCertificate.setOnItemSelectedListener(this);
        List certificate = new ArrayList();
        // Spinner Drop down elements
        certificate.add("Select Certificate");
        certificate.add("Server Certificate");
        certificate.add("Client private Certificate");
        certificate.add("Client public certificate");


        // Creating & Initializing an ArrayAdapter for spinner
        final ArrayAdapter dataAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, certificate) {
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
        mspinnerCertificate.setAdapter(dataAdapter);
        dataAdapter.notifyDataSetChanged();


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
                getCertificateFile();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }
    }


    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[4];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            myDialog.dismiss();
            BleAction.showToast("Certificate Uploaded !");
            disposeResources();
            resetValues();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            myDialog.dismiss();
            BleAction.showToast("Certificate Upload Failed :(");
        } else {
            myDialog.dismiss();
            BleAction.showToast("Invalid Response !");
        }
    }

    private boolean isLastPacketofSetConfig() {
        return pktSeqNum == TOTAL_FILE_PACKET && isAdditionalPktadded == true;
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

    private boolean isValidFilesize() {

        if (TOTAL_FILE_SIZE > 4096)
            return true;
        else
            return false;
    }

    private byte[] prepareStartCertificatePkt(int packetcount, int certificatelength) {
        byte certificateData[] = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];


        //Start Configuration Data Packet
        certificateData[0] = (byte) certificate_SubCMDID;//11
        //Total length
        certificateData[1] = 0;
        certificateData[2] = START_PKT_LENGTH;//11

        certificateData[3] = ET_Command.CMD_START_CONFIG_SUBCMD_ID;//01
        //Total File Packet
        certificateData[4] = (byte) ((packetcount >> 8) & 0XFF);//0
        certificateData[5] = (byte) (packetcount & 0XFF);//45

        //File Size
        certificateData[6] = (byte) ((certificatelength & 0XFF000000) >> 24);//0
        certificateData[7] = (byte) ((certificatelength & 0X00FF0000) >> 16);//0
        certificateData[8] = (byte) ((certificatelength & 0X0000FF00) >> 8);//22
        certificateData[9] = (byte) ((certificatelength & 0X000000FF) >> 0);//84

        //File version
        certificateData[10] = 0;
        certificateData[11] = 0;

        //CRC of File
        certificateData[12] = 0;
        certificateData[13] = 0;

        return certificateData;
    }


    private byte[] preparefileData(String configData) {
        Log.d(TAG, "Prepare file data");

        subpacketdata = new byte[150];
        certificatedata = configData.getBytes(StandardCharsets.UTF_8);//File data to bytes
        Log.d(TAG, "Config byte data length----" + certificatedata.length);//403(for pulse)
        Log.d(TAG, "Whole certificatedata----" + Arrays.toString(certificatedata));
        Log.d(TAG, "TOTAL_FILE_PACKET---" + TOTAL_FILE_PACKET);//4

        if (j < certificatedata.length) {
            if (isSetConfigAckReceived) {
                pktSeqNum++;
                if (isLastPacketofSetConfig()) {
                    subpacketdata = Arrays.copyOfRange(certificatedata, j - 1, certificatedata.length);//To get the length of last pkt from whole data
                } else {
                    Log.d(TAG, "Whole Data----" + certificatedata.length);
                    subpacketdata = Arrays.copyOfRange(certificatedata, j - 1, j + 127);//Individual pkt
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
                configFileDataPkt[0] = (byte) certificate_SubCMDID;//08
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
                String subpktdata = new String(subpacketdata, StandardCharsets.UTF_8);
                Log.d(TAG, "Subpktdata:" + subpktdata);
            } else {
                Log.d(TAG, "Warning- Unable to create packet");
            }
        } else {
            Log.d(TAG, "Warning- Configuration missing");
        }

        return configFileDataPkt;

    }


    private void processFiledata() {

        String certificateData = edit_certificateData.getText().toString();
        int maxcount = 4;
        if (!isLastPacket()) {
            Log.d(TAG, "isSetConfigAck" + isSetConfigAckReceived);

            if (isSetConfigAckReceived == false && retry_count <= maxcount) {
                Log.d(TAG, "config byte data ACK " + Arrays.toString(certificatedata));
                retry_count++;
                if (retry_count > maxcount) {
                    Log.d(TAG, "Exceeded Max attempts..Transfer Failed!");
                }
            } else {
                certificatedata = preparefileData(certificateData);
                Log.d(TAG, "config byte data after preparing" + Arrays.toString(certificatedata));
            }
            try {
                mgConnection.transmit(certificatedata);
                isSetConfigAckReceived = false;
                startTimer();
                Log.d(TAG, "config byte data sent" + Arrays.toString(certificatedata));
            } catch (IOException e) {
                Log.d(TAG, "Transmit Failed!");
            }

        } else {
            Log.d(TAG, "File Transfer completed!");
        }
    }


    private void getCertificateFile() {
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File CertFile = new File(root + "/ClientCerts");
            if (CertFile.exists()) {
                Log.d(TAG, "Fetch file");
                File file = new File(CertFile, fileName);
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
                String certificateData = configtext.toString();
                edit_certificateData.setText(certificateData);

            } else {
                Toast.makeText(_context, "File Doesn't Exist", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

    }

    private void startTimer() {
        timer = new Timer(true);
        timer.schedule(new CountDownTask(), 500, 1000);
        Log.d(TAG, "Start timer");
    }

    private void processResponseData(byte[] responseData) {

        int respCmdId = responseData[0];
        //Receive ACK  based on CMDId

        if (respCmdId == ET_Command.CMD_Server_Cert_Update)//23
        {

            Log.d(TAG, "Received Temporary  Start config  ACK1" + Arrays.toString(responseData));
            validateACK_NACK(responseData);
            if (isLastPacket()) {
                validateACKStatus(responseData);
            }


        } else if (respCmdId == ET_Command.CMD_Client_Public_Key_Update)//24
        {
            Log.d(TAG, "Received Temporary  Start config  ACK2" + Arrays.toString(responseData));
            validateACK_NACK(responseData);
            if (isLastPacket()) {
                validateACKStatus(responseData);
            }

        } else if (respCmdId == ET_Command.CMD_Client_Private_Key_Update)//25
        {
            Log.d(TAG, "Received Temporary  Start config  ACK3" + Arrays.toString(responseData));
            validateACK_NACK(responseData);
            if (isLastPacket()) {
                validateACKStatus(responseData);
            }

        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        Log.d(TAG, "Stop Timer");
        if (timer != null) {
            timer.cancel();
        }
    }

    private void resetValues() {
        mspinnerCertificate.setSelection(0);
        txtfilename.setText("");
        edit_certificateData.setText("");
        btnCertificateUpload.setVisibility(View.GONE);
    }

    private void disposeResources() {
        Log.d(TAG, "Disposed");
        configFileDataPkt = new byte[0];
        subpacketdata = new byte[0];
        certificatedata = new byte[0];
        j = 1;
        pktSeqNum = 0;
        Log.d(TAG, "J:" + j + "" + "Packetseqnum" + pktSeqNum);
        certificate_SubCMDID = 0;

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
            BleAction.showToast("Invalid Response !");
        }
    }

    // end private  method


    // Periheral Call back

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
        Log.d(TAG, "Warning- On Data Transmitted. Status: " + i + " Length: " + i1);

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
        switch (position) {
            case 1:
                certificate_SubCMDID = ET_Command.CMD_Server_Cert_Update;
                break;
            case 2:
                certificate_SubCMDID = ET_Command.CMD_Client_Public_Key_Update;
                break;
            case 3:
                certificate_SubCMDID = ET_Command.CMD_Client_Private_Key_Update;
                break;
            default:
                break;
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        String text = mspinnerCertificate.getSelectedItem().toString();
        if (text == "Server Certificate") {
            Toast.makeText(_context, "Select the file", Toast.LENGTH_SHORT).show();
        }

    }

    // End Peripheral Call back
}
