package com.SMU.DevSec;

import android.provider.BaseColumns;

public class SideChannelContract {
    static final String TABLE_NAME = "Side_Channel_Info";
    static final String GROUND_TRUTH = "Ground_Truth";
    static final String USER_FEEDBACK = "User_Feedback";
    /**
     * Static class to return column names for the database
     */
    public static class Columns {
        //public static final String _ID = BaseColumns._ID;
        public static final String TIMESTAMP = "Timestamp";
        public static final String SYSTEM_TIME = "System_Time";
        public static final String VOLUME = "Volume";
        public static final String ALLOCATABLE_BYTES = "Allocatable_Bytes";
        public static final String CACHE_QUOTA_BYTES = "Cache_Quota_Bytes";
        public static final String CACHE_SIZE = "Cache_Size";
        public static final String FREE_SPACE = "Free_Space";
        public static final String USABLE_SPACE = "Usable_Space";
        public static final String ELAPSED_CPU_TIME = "Elapsed_CPU_Time";
        public static final String CURRENT_BATTERY_LEVEL = "Current_Battery_Level";
        public static final String BATTERY_CHARGE_COUNTER = "Battery_Charge_Counter";
        public static final String MOBILE_TX_BYTES = "Mobile_Tx_Bytes";
        public static final String TOTAL_TX_BYTES = "Total_Tx_Bytes";
        public static final String MOBILE_TX_PACKETS = "Mobile_Tx_Packets";
        public static final String TOTAL_TX_PACKETS = "Total_Tx_Packets";
        public static final String MOBILE_RX_BYTES = "Mobile_Rx_Bytes";
        public static final String TOTAL_RX_BYTES = "Total_Rx_Bytes";
        public static final String MOBILE_RX_PACKETS = "Mobile_Rx_Packets";
        public static final String TOTAL_RX_PACKETS = "Total_Rx_Packets";
        //label
        public static final String CURRENT_APP = "Current_App";
        public static final String LABELS = "Labels";
        //user feedback
        public static final String CHOICES = "Choices";
        private Columns() {}
            // private constructor to prevent instantiation
        }



        public static String[] CLASSES = new String[]{
                "QueryInformation",
                "Camera",
                //"Calendar",
                "AudioRecording",
                //"ReadSMS",
                "RequestLocation",
                //"ReadContacts"
        };
}