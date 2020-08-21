package com.application.product.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOConnectionCallback;
import com.telit.terminalio.TIOManager;
import com.telit.terminalio.TIOPeripheral;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.application.product.myapplication.ET_Command.mgConnection;

public class PeripheralActivity extends AppCompatActivity implements TIOConnectionCallback {
    // region variables

    private static final int RSSI_INTERVAL = 1670;
    private static final String TAG = "PeripheralActivity";

    TextView txtaddress;

    private TIOPeripheral mPeripheral;
    private TIOConnection mConnection;
    private AlertDialog mConnectingDialog;

    private Context _context;
    private String mErrorMessage;

    private int mRssi;
    //Authentication data
    private byte bufferdata[] = {0};

    // end region varibles

    //  UI event Handlers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);

        setupActionBar();
        connectViews();
        connectPeripheral();
        displayVersionNumber();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.resetPassw:
                onResetPasswordPressed();
                return true;
            case R.id.disconnect:
                try {
                    onDisconnectButtonPressed();
                } catch (IOException e) {
                    Log.d(TAG, " Warning! Disconnect option not Working! ");
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onConnectButtonPressed(View sender) throws IOException {
        Log.d(TAG, "onConnectButtonPressed");
        mConnection = mPeripheral.connect(this);
        mConnection.setListener(this);
        showConnectionMessage();
    }

    private void onDisconnectButtonPressed() throws IOException {
        Log.d(TAG, "onDisconnectButtonPressed");
        stopRssiListener();
        mgConnection.disconnect();
        Intent i = new Intent(PeripheralActivity.this, MainActivity.class);
        startActivity(i);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void onResetPasswordPressed() {
        Log.d(TAG, "onResetPasswordPressed");
        Intent intent = new Intent(PeripheralActivity.this, ResetPassword.class);
        startActivity(intent);
    }


    // end  UI event Handlers

    //  private methods

    private void connectViews() {
        _context = getApplicationContext();
        txtaddress = (TextView) findViewById(R.id.txtaddress);
    }


    public void connectPeripheral() {
        // extract peripheral id (address) from intent
        Intent intent = getIntent();
        String address = intent.getStringExtra(TIOSample.PERIPHERAL_ID_NAME);

        // retrieve peripheral instance from TIOManager
        mPeripheral = TIOManager.getInstance().findPeripheral(address);

        if (mPeripheral != null) {
            mConnection = mPeripheral.getConnection();
            try {
                txtaddress.setText(mPeripheral.getName() + "  " + mPeripheral.getAddress());
                onConnectButtonPressed(getWindow().getDecorView().getRootView());
            } catch (Exception ex) {
                Log.e(TAG, "! Connect to peripheral failed, " + ex.toString());
            }
        }
    }

    private void startRssiListener() {
        if (mPeripheral.getConnectionState() == TIOConnection.STATE_CONNECTED) {

            Log.d(TAG, "startRssiListener");
            try {
                mConnection.readRemoteRssi(RSSI_INTERVAL);
            } catch (Exception ex) {

            }
        }
    }

    private void stopRssiListener() {
        if (mConnection != null) {
            Log.d(TAG, "stopRssiListener");
            try {
                mConnection.readRemoteRssi(0);
            } catch (Exception ex) {

            }
        }
    }


    private void displayVersionNumber() {
        PackageInfo packageInfo;
        String version = "";
        try {
            packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
        setTitle(getTitle() + " " + version);
    }

    void showErrorAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle(Html.fromHtml("<font color='#FF7F27'>Error</font>"))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    void showConnectionMessage() {
        final AlertDialog.Builder dialog;
        if (mConnectingDialog == null) {
            // Create dialog
            dialog = new AlertDialog.Builder(this)
                    .setMessage("Connecting...")
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (mConnection != null) {
                                try {
                                    mConnection.cancel();
                                    mConnection = null;
                                } catch (Exception ex) {
                                    Log.e(TAG, ex.toString());
                                }
                            }
                        }
                    });
            mConnectingDialog = dialog.create();

            // Disable click event outside a dialog
            Window window = mConnectingDialog.getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        mConnectingDialog.show();
    }

    private byte[] authenticate(String text) {
        bufferdata = new byte[ET_Command.TIO_DEFAULT_UART_DATA_SIZE];
        int passwlen = text.getBytes().length;//11
        Log.d(TAG, "Length of String " + passwlen);//11

        //Byte data
        bufferdata[0] = ET_Command.CMD_AUTHENTICATE_USER;//0
        bufferdata[1] = (byte) ((passwlen >> 8) & 0XFF);//0
        bufferdata[2] = (byte) (passwlen & 0XFF);//17

        byte[] asciidata = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 3; i < passwlen + 3; i++) {
            bufferdata[i] = asciidata[i - 3];
            Log.d(TAG, "bufferdata3:" + bufferdata[i]);//Data
        }

        return bufferdata;
    }

    private void showPasswordAlert() {
        LayoutInflater li = LayoutInflater.from(PeripheralActivity.this);
        View promptsView = li.inflate(R.layout.prompt, null);

        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(
                PeripheralActivity.this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String password = userInput.getText().toString();//Stevens1911(11)
                                try {
                                    if (password != null) {
                                        byte passworddata[] = authenticate(password);
                                        mgConnection.transmit(passworddata);//send data
                                    } else {
                                        Log.d(TAG, "Password is empty");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Warning:Operation Failed");
                                }


                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    // end private methods

    // Peripheral callbacks

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        if (mConnection != null) {
            mConnection.setListener(this);
            startRssiListener();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        stopRssiListener();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mgConnection != null) {
            mgConnection.setListener(null);
        }

        super.onDestroy();
    }

    @Override
    public void onConnected(TIOConnection tioConnection) {

        Log.d(TAG, "onConnected");

        if ((mConnectingDialog != null) && mConnectingDialog.isShowing()) {
            mConnectingDialog.dismiss();
            // enableViews();
            showPasswordAlert();
        }
        mgConnection = mPeripheral.getConnection();
        startRssiListener();

    }

    @Override
    public void onConnectFailed(TIOConnection tioConnection, String s) {
        Log.d(TAG, "onConnectFailed " + s);
        if ((mConnectingDialog != null) && mConnectingDialog.isShowing()) {
            mConnectingDialog.dismiss();
        }
        mErrorMessage = s;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mErrorMessage.length() > 0) {
                    showErrorAlert("Failed to connect with error message: " + mErrorMessage);
                }
            }
        };
        runOnUiThread(runnable);

    }

    @Override
    public void onDisconnected(TIOConnection tioConnection, String s) {
        Log.d(TAG, "onDisconnected" + s);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(_context, "Disconnected from BLE", Toast.LENGTH_LONG).show();
            }
        };
        runOnUiThread(runnable);
    }

    @Override
    public void onDataReceived(TIOConnection tioConnection, final byte[] data) {
        Log.d(TAG, "onDataReceived len  " + data.length);

        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    if (data.length != 0) {
                        if (data[0] != ET_Command.CMD_AUTHENTICATE_USER)//0
                        {
                            Log.d(TAG, "Incorrect Command ID");
                        } else if (data[1] == 0) {
                            Toast.makeText(_context, "Authentication Success!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PeripheralActivity.this, BleAction.class);
                            intent.putExtra(TIOSample.PERIPHERAL_ID_NAME, mPeripheral.getAddress());
                            startActivity(intent);

                        } else if (data[1] == 1) {
                            Toast.makeText(_context, "Authentication Failed :(", Toast.LENGTH_LONG).show();
                            showPasswordAlert();
                        } else {
                            Toast.makeText(_context, "Something went wrong :(", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "Invalid data");

                    }
                }
            };
            runOnUiThread(runnable);
        } catch (Exception ex) {
            Log.d(TAG, ex.toString());
        }

    }

    @Override
    public void onDataTransmitted(TIOConnection tioConnection, int i, int rssi) {

    }


    @Override
    public void onReadRemoteRssi(TIOConnection tioConnection, int i, final int rssi) {

    }

    @Override
    public void onLocalUARTMtuSizeUpdated(TIOConnection tioConnection, int i) {

    }

    @Override
    public void onRemoteUARTMtuSizeUpdated(TIOConnection tioConnection, int i) {

    }

    // end Peripheral callbacks

}
