package com.application.product.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import java.lang.reflect.Array;
import java.util.Arrays;

import static com.application.product.myapplication.ET_Command.mgConnection;

//Reset the password with the 8 byte password code
public class ResetPassword extends AppCompatActivity implements TIOConnectionCallback {

    // region variable

    EditText editresetpassw;
    Button btnreset;
    private TIOPeripheral mPeripheral;
    private static final String TAG = "Reset Password";
    private boolean isEmptyData;
    private Context _context;
    private boolean isValidPasswCode;

    // Constructor

   public ResetPassword() {
        mgConnection.setListener(this);
    }


    // end region variable


    //Peripheral Call back

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

    // end Peripheral


    // private method

    private boolean isValidData(byte[] responseData) {
        return (responseData == null || responseData.length == 0);
    }

    public void processResponseData(byte[] responseData) throws IOException {

        int respCmdId = responseData[0];

        if (respCmdId == ET_Command.CMD_RESET_PASSWORD)     //02
        {
            Log.d(TAG, "Reset Password ACK" + Arrays.toString(responseData));//[2, 0, 1, 0]
            validateACKStatus(responseData);
        } else {
            Log.d(TAG, "Invalid Command Id" + respCmdId);
        }

    }

    private void validateACKStatus(byte[] responseData) {
        int ACKStatus = responseData[3];
        if (ACKStatus == ET_Command.ACK_FILE_STATUS.Success.statusVal) {
            Toast.makeText(_context, "Password reset Successfully !", Toast.LENGTH_LONG).show();
        } else if (ACKStatus == ET_Command.ACK_FILE_STATUS.Failure.statusVal) {
            Toast.makeText(_context, "Password Reset  Failed !", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(_context, "Warning:Invalid Response !", Toast.LENGTH_LONG).show();
        }
    }

    private byte[] prepareResetPasswPkt(byte[] resetCodeData) {
        Log.d(TAG, "prepareResetPasswPkt");
        byte[] resetPaswordReqPkt = new byte[11];
        int passwRequestlen=8;
        resetPaswordReqPkt[0] = ET_Command.CMD_RESET_PASSWORD;//02
        resetPaswordReqPkt[1] = 0;
        resetPaswordReqPkt[2] = (byte) passwRequestlen;//Total length
        for (int i = 0; i < passwRequestlen; i++) {
            resetPaswordReqPkt[i + 3] = resetCodeData[i];
        }
        Log.d(TAG, "resetPaswordReqPkt:" + Arrays.toString(resetPaswordReqPkt));//[2, 0, 8, 14, -113, -3, 61, -98, -91, 2, 58]

        return resetPaswordReqPkt;
    }

    public static byte[] hexStringToByteArray(String resetCode) {
        int len = resetCode.length();
        byte[] resetdata = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            resetdata[i / 2] = (byte) ((Character.digit(resetCode.charAt(i), 16) << 4)
                    + Character.digit(resetCode.charAt(i + 1), 16));
        }
        return resetdata;
    }

    private boolean validateResetPassword(String passw) {
        int passwlen = passwordlen(passw);
        if (passwlen > 16 || passwlen == 0)
            return false;
        else
            return true;
    }

    private int passwordlen(String password) {
        int passwordlen = password.getBytes().length;
        return passwordlen;
    }

    // end private method

    // UI event handlers

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password);
        setContentViews();
        setupActionBar();
        getconnection();
    }

    private void setContentViews() {
        btnreset = (Button) findViewById(R.id.btnresetPassw);
        editresetpassw = (EditText) findViewById(R.id.resetPassw);
        _context = getApplicationContext();

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
                    startActivity(new Intent(ResetPassword.this, PeripheralActivity.class));
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


    public void onResetPasswPressed(View sender) throws IOException {
        String resetCode = editresetpassw.getText().toString();//0e8ffd3d9ea5023a
        isValidPasswCode = validateResetPassword(resetCode);//Check for resetpaswwcode len =16 to or >16
        if (isValidPasswCode) {
            byte[] convertedHexStringByte = hexStringToByteArray(resetCode);
            byte[] sendResetPasswReqPkt = prepareResetPasswPkt(convertedHexStringByte);
            mgConnection.transmit(sendResetPasswReqPkt);//Send Reset Passw request pkt
        } else {
            Toast.makeText(_context, "PasswordCode exceeded length 16! Please try again:(!", Toast.LENGTH_LONG).show();
        }

    }


}
