package com.application.product.myapplication;

import android.content.Intent;
import android.util.Log;

import com.telit.terminalio.TIOConnection;
import com.telit.terminalio.TIOManager;


public class ET_Command {

    //Global Connection Object
    public static TIOConnection mgConnection;
    public static final int TIO_DEFAULT_UART_DATA_SIZE = 30;

    //Etracker2 Commands

    public static final int CMD_AUTHENTICATE_USER = (byte) 01;
    public static final int CMD_SDI_12_BUS = (byte) 14;
    public static final int CMD_SDI_12_BUS_POWOFF = (byte) 15;
    public static final int CMD_START_SET_CONFIG = (byte) 11;
    public static final int CMD_START_GET_CONFIG = (byte) 16;
    public static final int CMD_START_CONFIG_SUBCMD_ID = (byte) 01;
    public static final int CMD_START_CONFIG_ACK_SUBCMD_ID = (byte) 02;
    public static final int CMD_CONFIG_DATA_PACKET_SUBCMD_ID = (byte) 03;
    public static final int CMD_CONFIG_DATA_PACKET_ACK_SUBCMD_ID = (byte) 04;
    public static final int CMD_APPLY_CONFIG_CHANGES = (byte) 07;
    public static final int CMD_TIME_SYNC_REQUEST_PACKET = (byte) 19;
    public static final int CMD_DEVICE_DETAILS_REQUEST_PACKET = (byte) 18;
    public static final int CMD_SD_CARD_STATUS = (byte) 20;
    public static final int CMD_SET_PASSWORD = (byte) 03;
    public static final int CMD_SWITCH_TO_LM = (byte) 10;

    public static final int CMD_SCAN_SDI12_REQUEST_PACKET = (byte) 04;

    public static final int CMD_SCAN_SDI12_REQUEST_SUBCMD_ID = (byte) 01;
    public static final int CMD_SCAN_SDI12_PROGRESS_ACK_SUBCMD_ID = (byte) 02;
    public static final int CMD_SCAN_SDI12_INFO_PACKET_SUBCMD_ID = (byte) 03;
    public static final int CMD_SCAN_SDI12_INFO_PACKET_ACK_SUBCMD_ID = (byte) 04;
    public static final int CMD_SCAN_SDI12_DATA_PACKET_SUBCMD_ID = (byte) 05;
    public static final int CMD_SCAN_SDI12_DATA_PACKET_ACK_SUBCMD_ID = (byte) 06;

    public static final int CMD_APPLY_ACTION_COMMAND = (byte) 9;
    public static final int CMD_REPORT_REAL_TIME_SENSOR = (byte) 22;
    public static final int CMD_START_TEMPORARY_OVERRIDE = (byte) 8;
    public static final int CMD_START_TEMPORARY_OVERRIDE_SUBCMDID = (byte) 01;
    public static final int CMD_START_TEMPORARY_OVERRIDE_ACK_SUBCMDID = (byte) 02;
    public static final int CMD_START_TEMPORARY_OVERRIDE_DATA_PKT_SUBCMDID = (byte) 03;
    public static final int CMD_START_TEMPORARY_OVERRIDE_ACK_DATA_PKT_SUBCMDID = (byte) 04;
    public static final int CMD_Server_Cert_Update = (byte) 23;
    public static final int CMD_Client_Public_Key_Update = (byte) 24;
    public static final int CMD_Client_Private_Key_Update = (byte) 25;
    public static final int CMD_VERIFY_NETWORK_CONNECTION = (byte) 21;
    public static final int CMD_RESET_PASSWORD = (byte) 02;
    public static final int CMD_OTA_CMDID = (byte) 13;
    public static final int CMD_REPORTREALTIMESENSOR_SERVER_CMDID = (byte) 05;
    public static final int CMD_REPORTCURRENTCONFIG_SERVER_CMDID = (byte) 06;


    //  ACK Status

    public enum ACK_FILE_STATUS {
        Success(0),
        Failure(1);

        public int statusVal;

        ACK_FILE_STATUS(int statusVal) {
            this.statusVal = statusVal;
        }
    }

    public enum CRC_ACK_FILE_STATUS {
        Success(0),
        Failure(1);

        public int statusVal;

        CRC_ACK_FILE_STATUS(int statusVal) {
            this.statusVal = statusVal;
        }
    }

    public enum APPLY_ACTION_SUBCMDID {

        toggle_power24(1),

        toggle_power12_1(2),

        toggle_power12_2(3),

        toggle_power12_3(4),

        toggle_power5(5),

        toggle_power3v3(6),

        execute_rs485_cmd(7),

        execute_rs232_cmd(8),

        execute_sdi0_cmd(9),

        execute_sdi1_cmd(10);

        public int subCMDId;

        APPLY_ACTION_SUBCMDID(int subCMDId) {
            this.subCMDId = subCMDId;
        }
    }

    public enum REAL_TIME_SENSOR_SUBMCMDID {

        REAL_TIME_SENSOR_REQUEST_SUBCMD_ID(01),
        REAL_TIME_SENSOR_PROGRESS_ACK_SUBCMD_ID(02),
        REAL_TIME_SENSOR_INFO_PACKET_SUBCMD_ID(03),
        REAL_TIME_SENSOR_INFO_PACKET_ACK_SUBCMD_ID(04),
        REAL_TIME_SENSOR_DATA_PACKET_SUBCMD_ID(05),
        REAL_TIME_SENSOR_DATA_PACKET_ACK_SUBCMD_ID(06);

        public int SubCmdId;

        REAL_TIME_SENSOR_SUBMCMDID(int SubCmdId) {
            this.SubCmdId = SubCmdId;
        }
    }


}
