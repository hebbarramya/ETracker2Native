package com.application.product.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static android.content.ContentValues.TAG;
import static com.application.product.myapplication.ET_Command.mgConnection;

// Performs the operation SetPassw,SwitchToLM,Check SDCard Status,Report Realtime Sensor reading to server,Report Current confog to server
public class BLEOperation implements TIOConnectionCallback {
    //
    // region member variables

    private static final String TAG = "BLEOperation";
    private boolean isEmptyData;
    private boolean isValidPassw;
    private final Context context;


    // end Region variables
    //Constructor

    public BLEOperation(Context context) {
        mgConnection.setListener(this);
        this.context = context;

    }


    // End Constructor

    // Peripheral Call Back Methods

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
    // End Peripheral call back


    // Private methods

    private byte[] prepareSDCardStatus() {
        Log.d(TAG, "Inside prepare_SDCardStatus");
        byte[] SDCardStatusReqPkt = new byte[3];
        int SDCardDatalen = 0;
        SDCardStatusReqPkt[0] = ET_Command.CMD_SD_CARD_STATUS;//20
        SDCardStatusReqPkt[1] = (byte) SDCardDatalen;
        SDCardStatusReqPkt[2] = (byte) SDCardDatalen;

        return SDCardStatusReqPkt;
    }


    private byte[] prepareSwitchToLM() {
        Log.d(TAG, "Inside prepareSwitchToLM");
        byte[] SwitchToLMReqPkt = new byte[3];
        int SwitchTOLMDatalen = 0;
        SwitchToLMReqPkt[0] = ET_Command.CMD_SWITCH_TO_LM;//10
        SwitchToLMReqPkt[1] = (byte) SwitchTOLMDatalen;
        SwitchToLMReqPkt[2] = (byte) SwitchTOLMDatalen;

        return SwitchToLMReqPkt;
    }

    private byte[] prepareRealtimeSensorToServer() {
        Log.d(TAG, "Inside prepareRealtimeSensorToServer");
        byte[] sensorToServerReq = new byte[3];
        int SensorToServerlen = 0;
        sensorToServerReq[0] = ET_Command.CMD_REPORTREALTIMESENSOR_SERVER_CMDID;//05
        sensorToServerReq[1] = (byte) SensorToServerlen;
        sensorToServerReq[2] = (byte) SensorToServerlen;

        return sensorToServerReq;
    }


    private byte[] prepareCurrentConfigToServer() {
        Log.d(TAG, "Inside prepareRealtimeSensorToServer");
        byte[] ConfigToServerReq = new byte[3];
        int ConfigToServerlen = 0;
        ConfigToServerReq[0] = ET_Command.CMD_REPORTCURRENTCONFIG_SERVER_CMDID;//06
        ConfigToServerReq[1] = (byte) ConfigToServerlen;
        ConfigToServerReq[2] = (byte) ConfigToServerlen;

        return ConfigToServerReq;
    }

    private int passwordlen(String password) {

        int passwordlen = password.getBytes().length;
        return passwordlen;

    }

    private byte[] prepareSetPassword(String password, int passwordlen) {
        byte[] passwordData = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];
        passwordData[0] = ET_Command.CMD_SET_PASSWORD;//03
        passwordData[1] = (byte) ((passwordlen >> 8) & 0XFF);//0
        passwordData[2] = (byte) (passwordlen & 0XFF);//20

        byte[] password_asciiData = password.getBytes(StandardCharsets.UTF_8);
        for (int i = 3; i < passwordlen + 3; i++) {
            passwordData[i] = password_asciiData[i - 3];
        }
        return passwordData;
    }

    public void send_SDCardStatusReqPkt() throws IOException {
        Log.d(TAG, "Inside send_SDCardStatusReqPkt");
        byte[] sendSDCardStatusReq = prepareSDCardStatus();
        mgConnection.transmit(sendSDCardStatusReq);//Send SDCard Req
        Log.d(TAG, "SD Card Status Req sent-" + Arrays.toString(sendSDCardStatusReq));//[20, 0, 0]
    }

    public void send_SwitchToLM() throws IOException {
        Log.d(TAG, "Inside send_SwitchToLM");
        byte[] sendSwitchToLMReq = prepareSwitchToLM();
        mgConnection.transmit(sendSwitchToLMReq);//Send SwitchToLM Req
        Log.d(TAG, "SwitchToLM Req sent-" + Arrays.toString(sendSwitchToLMReq));//[10, 0, 0]
    }

    public void send_prepareRealtimeSensorToServer() throws IOException {
        Log.d(TAG, "Inside prepareRealtimeSensorToServer");
        byte[] sendRealTimeSensorToServer = prepareRealtimeSensorToServer();
        mgConnection.transmit(sendRealTimeSensorToServer);//Send RealTimeSensor To Server
        BleAction.showDialog();
        Log.d(TAG, "RealTimeSensor To Server sent-" + Arrays.toString(sendRealTimeSensorToServer));//[5, 0, 0]
    }

    public void send_prepareCurrentConfigToServer() throws IOException {
        Log.d(TAG, "Inside prepareCurrentConfigToServer");
        byte[] sendCurrentConfigToServer = prepareCurrentConfigToServer();
        mgConnection.transmit(sendCurrentConfigToServer);//Send CurrentConfig To Server
        BleAction.showDialog();
        Log.d(TAG, "CurrentConfig To Server sent-" + Arrays.toString(sendCurrentConfigToServer));//[6, 0, 0]
    }

    public void send_SetPaswordData(String password) throws IOException {
        isValidPassw = validatePassword(password);//Check for password len =20 to or >20
        if (isValidPassw) {
            int len = passwordlen(password);
            byte[] sendPasswordData = prepareSetPassword(password, len);
            mgConnection.transmit(sendPasswordData);//Send Set Password
            Log.d(TAG, "Set Password sent-" + Arrays.toString(sendPasswordData));
        } else {
            BleAction.showToast("Password length exceeded length 20! Please try again:(");
        }
    }

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            BleAction.showToast("Device Operation  Success");
            BleAction.hideDialog();

        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            BleAction.showToast("Device Operation  Failed :(");
            BleAction.hideDialog();
        } else {
            BleAction.showToast("Invalid Response !");
        }
    }

    private void validateSwitchToLMACKStatus(byte[] responseData) {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            BleAction.showToast("Switched to Logging Mode");
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            BleAction.showToast("Switched to Logging Mode Failed :(");
        } else {
            BleAction.showToast("Invalid Response !");
        }
    }

    private void validateSetPasswACKStatus(byte[] responseData)throws IOException {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            BleAction.showToast("Password Set Successfully");
            mgConnection.disconnect();
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            BleAction.showToast("Password Set Failed :(");
        } else {
            BleAction.showToast("Invalid Response !");
        }
    }

    public void processResponseData(byte[] responseData)throws IOException {
        int respCmdId = responseData[0];

        if (respCmdId == ET_Command.CMD_SD_CARD_STATUS)//20
        {
            Log.d(TAG, "SD_Card Status Ack" + Arrays.toString(responseData));//[20, 0, 1, 0]
            validateACKStatus(responseData);

        } else if (respCmdId == ET_Command.CMD_SWITCH_TO_LM)//10
        {
            Log.d(TAG, "Switch TO LM Ack" + Arrays.toString(responseData));//[10, 0, 1, 0]
            validateSwitchToLMACKStatus(responseData);

        } else if (respCmdId == ET_Command.CMD_SET_PASSWORD)//03
        {
            Log.d(TAG, "Set Password Ack" + Arrays.toString(responseData));//[3, 0, 1, 0]
            validateSetPasswACKStatus(responseData);


        } else if (respCmdId == ET_Command.CMD_REPORTREALTIMESENSOR_SERVER_CMDID)//05
        {
            Log.d(TAG, "Report RealTime Server  Ack" + Arrays.toString(responseData));//[5, 0, 1, 0]
            validateACKStatus(responseData);

        } else if (respCmdId == ET_Command.CMD_REPORTCURRENTCONFIG_SERVER_CMDID)//06
        {
            Log.d(TAG, "Report Current Config to Server  Ack" + Arrays.toString(responseData));//[6, 0, 1, 0]
            validateACKStatus(responseData);

        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }
    }

    private boolean validatePassword(String passw) {
        int passwlen = passwordlen(passw);
        if (passwlen > 20 || passwlen == 0)
            return false;
        else
            return true;
    }
    // end private methods

    //
}
