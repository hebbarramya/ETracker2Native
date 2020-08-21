package com.application.product.myapplication;

import android.app.Activity;
import android.util.Log;

import org.json.simple.parser.ParseException;

import java.io.IOException;
// Process the data for 20 bytes
public class CommonProcessResponse {

    private int MAX_CMDID = 25;
    private byte[] appended_Data = new byte[250];
    private int index = 0;
    private static int recvdpktlen = 0;
    private static int packet_length = 0;
    private static final String TAG = "CommonProcessResponse";
    private boolean isPacketrecvd;

    public void validateRecvdData(byte[] resp_data, Activity activity) throws IOException,ParseException {
        Log.d(TAG,"Inside Vaidate Recvd Data");
        int CmdId = resp_data[0];
        if (isPacketrecvd == false) {
            recvdpktlen = resp_data[1] + (resp_data[2] & 0XFF);//131

            Log.d(TAG, "Received packet len-" + recvdpktlen);
            if (CmdId <= MAX_CMDID)
            {
                if (resp_data.length < recvdpktlen) //Append the 1st pkt if received 20bytes
                {
                    Log.d(TAG, "Response Data length-" + resp_data.length);
                    isPacketrecvd = true;
                    append_recvdData(resp_data, resp_data.length);
                    packet_length = packet_length + resp_data.length;
                } else {
                    //Recvd expected pkt length
                    if (activity instanceof ReadConfig) {
                        ((ReadConfig) activity).processResponseData(resp_data);
                    }
                    else if(activity instanceof DeviceDetailsTimeSync){
                        ((DeviceDetailsTimeSync) activity).processResponseData(resp_data);
                    }
                    else if(activity instanceof Scan_SDI12){
                        ((Scan_SDI12) activity).processResponseData(resp_data);
                    }
                    else if(activity instanceof ReportRealTimeSensorReading){
                        ((ReportRealTimeSensorReading) activity).processResponseData(resp_data);
                    }

                }
            }
        } else//Append till the data is reached to expected pktlen
        {
            append_recvdData(resp_data, resp_data.length);
            packet_length = packet_length + resp_data.length;

            if (isValidRecvdData()) {
                isPacketrecvd = false;
                if (activity instanceof ReadConfig) {
                    ((ReadConfig) activity).processResponseData(appended_Data);
                }
                else if(activity instanceof DeviceDetailsTimeSync){
                    ((DeviceDetailsTimeSync) activity).processResponseData(appended_Data);
                }
                else if(activity instanceof Scan_SDI12){
                    ((Scan_SDI12) activity).processResponseData(appended_Data);
                }
                else if(activity instanceof ReportRealTimeSensorReading){
                    ((ReportRealTimeSensorReading) activity).processResponseData(appended_Data);
                }

                packet_length = 0;
                index = 0;
            }
        }
    }

    //Append 20 bytes data
    public byte[] append_recvdData(byte[] recvdData, int len) {
        System.arraycopy(recvdData, 0, appended_Data, index, len);//Append data(20bytes)
        index += len;
        return appended_Data;
    }

    private boolean isValidRecvdData() {
        return packet_length == (recvdpktlen + 3);//= to 134
    }

}







