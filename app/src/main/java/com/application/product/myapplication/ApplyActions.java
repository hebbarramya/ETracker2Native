package com.application.product.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.application.product.myapplication.ET_Command.mgConnection;

// Apply Action Command by selecting command from spinner with the value as TimeInMilisecs  & SDI adction Data

public class ApplyActions extends AppCompatActivity implements TIOConnectionCallback, AdapterView.OnItemSelectedListener {

    //Region Private variables

    Spinner mspinner;
    EditText edit_TimeInMilisecs, edit_applyactions_data;
    Button btnApplyActions;
    RelativeLayout RL_TimeInMilisecs, RL_ActionData;

    private static final String TAG = "Apply Actions";
    private TIOPeripheral mPeripheral;
    private static int action_SubCMDID = 0;
    private static int timeInMilsecs = 0;
    private static int datalen = 0;
    private String actionData = "";
    private boolean isEmptyData;

    private Context _context;

    // Constructor

    public ApplyActions() {
        mgConnection.setListener(this);
    }

    // End constructor

    // End region Private variables

    //UI Event Handlers

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.apply_actions);
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
                    startActivity(new Intent(ApplyActions.this, BleAction.class));
                    break;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
        }
        return true;
    }

    public void onApplyActionsPressed(View Sender) throws IOException {
        Log.d(TAG, "Inside onApplyActionsPressed");
        if (isValidSubCmdId()) {
            timeInMilsecs = Integer.parseInt(edit_TimeInMilisecs.getText().toString());
            if (!isValidTime(timeInMilsecs)) {
                Toast.makeText(_context, "Warning !Time should be in range from 1 to 65500", Toast.LENGTH_SHORT).show();
            } else {
                sendApplyActionData();
            }
        } else {
            actionData = edit_applyactions_data.getText().toString();
            if (!isValidActionData(actionData)) {
                Toast.makeText(_context, "Warning -Data has exceeded the length !!", Toast.LENGTH_SHORT).show();
            } else {
                sendApplyActionData();
            }
        }
    }

    private void sendApplyActionData() throws IOException {
        getDatalen();
        byte[] sendApplyActionData = prepareApplyActionsData();
        mgConnection.transmit(sendApplyActionData);//Send Device Details request
    }


    // End UI Event Handlers

    // Private Methods

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//Set the <- button
    }

    private void setContentViews() {
        btnApplyActions = (Button) findViewById(R.id.btnApplyActions);
        edit_applyactions_data = (EditText) findViewById(R.id.enterApplyActionData);
        edit_TimeInMilisecs = (EditText) findViewById(R.id.timeinMilisecs);
        mspinner = (Spinner) findViewById(R.id.spinner);
        RL_TimeInMilisecs = (RelativeLayout) findViewById(R.id.relativelayout_timeInMilisec);
        RL_ActionData = (RelativeLayout) findViewById(R.id.relativelayout_applyaction_data);

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

    private void setSpinner() {

        mspinner.setOnItemSelectedListener(this);
        List commands = new ArrayList();
        // Spinner Drop down elements
        commands.add("Select Command");
        commands.add("toggle_power24");
        commands.add("toggle_power12_1!");
        commands.add("toggle_power12_2!");
        commands.add("toggle_power12_3!");
        commands.add("toggle_power5!");
        commands.add("toggle_power3v3!");
        commands.add("execute_rs485_cmd!");
        commands.add("execute_rs232_cmd!");
        commands.add("execute_sdi0_cmd");
        commands.add("execute_sdi1_cmd");

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

    private void enableLayout() {
        if (isValidSubCmdId()) {
            if (RL_TimeInMilisecs.getVisibility() == View.GONE) {
                RL_TimeInMilisecs.setVisibility(View.VISIBLE);
                RL_ActionData.setVisibility(View.GONE);
            }
        } else {
            if (RL_ActionData.getVisibility() == View.GONE) {
                RL_ActionData.setVisibility(View.VISIBLE);
                RL_TimeInMilisecs.setVisibility(View.GONE);
            }

        }
    }

    private int getDatalen() {
        if (isValidSubCmdId()) {
            datalen = edit_TimeInMilisecs.getText().length();
        } else {
            datalen = edit_applyactions_data.getText().length();
        }
        return datalen;
    }


    private boolean isValidSubCmdId() {
        if (action_SubCMDID <= 6) {
            return true;
        } else {
            return false;
        }

    }

    private boolean isValidTime(int timeInMilSecs) {
        if (timeInMilSecs >= 1 && timeInMilSecs <= 65500)
            return true;
        else
            return false;
    }

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private boolean isValidActionData(String actionData) {

        int actiondatalen = actionData.getBytes().length;
        boolean validate = false;
        if (action_SubCMDID == 7 || action_SubCMDID == 8) {
            if (actiondatalen > 48)
                validate = false;
            else
                validate = true;

        } else if (action_SubCMDID == 9 || action_SubCMDID == 10) {
            if (actiondatalen > 98)
                validate = false;
            else
                validate = true;


        } else {
            // Toast.makeText(_context, "Warning ! Invalid SubCmdId", Toast.LENGTH_SHORT).show();
        }
        return validate;
    }


    private byte[] prepareApplyActionsData() {

        byte[] applyActionData = new byte[datalen + 4];

        int applyactiontotallen = datalen + 1;//Append SubcmdId length
        applyActionData[0] = ET_Command.CMD_APPLY_ACTION_COMMAND;//09
        //Total Length
        applyActionData[1] = (byte) ((applyactiontotallen >> 8) & 0XFF);//0
        applyActionData[2] = (byte) (applyactiontotallen & 0XFF);//
        applyActionData[3] = (byte) action_SubCMDID;//01
        if (isValidSubCmdId()) {
            //TimeinMiliSecs (2 bytes)
            applyActionData[4] = (byte) ((timeInMilsecs >> 8) & 0XFF);
            applyActionData[5] = (byte) (timeInMilsecs & 0XFF);//[9, 0, 6, 1, 78, 32,]

        } else {
            // Action Data
            byte[] asciidata = actionData.getBytes(StandardCharsets.UTF_8);
            for (int i = 4; i < datalen + 4; i++) {
                applyActionData[i] = asciidata[i - 4];//[9, 0, 4, 10, 90, 73, 33,]
                Log.d(TAG, "applyActionData4:" + applyActionData[i]);
            }

        }
        Log.d(TAG, "prepareApplyActionsData" + Arrays.toString(applyActionData));
        return applyActionData;
    }

    private void processResponseData(byte[] responseData) {
        int respCmdId = responseData[0];

        if (respCmdId == ET_Command.CMD_APPLY_ACTION_COMMAND)//9
        {
            Log.d(TAG, "Apply Actions ACK" + Arrays.toString(responseData));//[9, 0, 2, 1, 1],,[9, 0, 2, 10, 0]
            validateACKStatus(responseData);
            datalen = 0;

        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }

    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[4];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            Toast.makeText(_context, "Apply Actions Success !", Toast.LENGTH_LONG).show();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            Toast.makeText(_context, "Apply Actions Failed !", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(_context, "Warning:Invalid Response !", Toast.LENGTH_LONG).show();
        }
    }


    //End Private methods

    // Peripheral Callback methods

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (position) {
            case 1:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power24.subCMDId;
                break;
            case 2:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power12_1.subCMDId;
                break;
            case 3:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power12_2.subCMDId;
                break;
            case 4:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power12_3.subCMDId;
                break;
            case 5:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power5.subCMDId;
                break;
            case 6:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.toggle_power3v3.subCMDId;
                break;
            case 7:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.execute_rs485_cmd.subCMDId;
                break;
            case 8:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.execute_rs232_cmd.subCMDId;
                break;
            case 9:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.execute_sdi0_cmd.subCMDId;
                break;
            case 10:
                action_SubCMDID = ET_Command.APPLY_ACTION_SUBCMDID.execute_sdi1_cmd.subCMDId;
                break;
            default:
                break;
        }
        enableLayout();


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Toast.makeText(parent.getContext(), "Please select the command from the list !", Toast.LENGTH_SHORT).show();

    }

    // End Peripheral call backs
}
