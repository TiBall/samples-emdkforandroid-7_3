/*
 * Copyright (C) 2015-2019 Zebra Technologies Corporation and/or its affiliates
 * All rights reserved.
 */
package com.symbol.barcodesample1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.pm.ActivityInfo;
import com.symbol.emdk.ProfileManager;
//import com.symbol.emdk.ProfileConfig;


import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Xml;

public class MainActivity extends Activity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private TextView textViewData = null;
    private TextView textViewStatus = null;

    private CheckBox checkBoxEAN8 = null;
    private CheckBox checkBoxEAN13 = null;
    private CheckBox checkBoxCode39 = null;
    private CheckBox checkBoxCode128 = null;

    private Spinner spinnerScannerDevices = null;

    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0; // Keep the selected scanner
    private int defaultIndex = 0; // Keep the default scanner
    private int dataLength = 0;
    private String statusString = "";

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();


    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "ScannerTest";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    private TextView statusTextView = null;

    private String SSID = "";

    // Provides the error type for characteristic-error
    private String errorType = "";

    // Provides the parm name for parm-error
    private String parmName = "";

    // Provides error description
    private String errorDescription = "";

    // Provides error string with type/name + description
    private String errorString = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        textViewData = (TextView)findViewById(R.id.textViewData);
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        checkBoxEAN8 = (CheckBox)findViewById(R.id.checkBoxEAN8);
        checkBoxEAN13 = (CheckBox)findViewById(R.id.checkBoxEAN13);
        checkBoxCode39 = (CheckBox)findViewById(R.id.checkBoxCode39);
        checkBoxCode128 = (CheckBox)findViewById(R.id.checkBoxCode128);
        spinnerScannerDevices = (Spinner)findViewById(R.id.spinnerScannerDevices);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            updateStatus("EMDKManager object request failed!");
            return;
        }

        checkBoxEAN8.setOnCheckedChangeListener(this);
        checkBoxEAN13.setOnCheckedChangeListener(this);
        checkBoxCode39.setOnCheckedChangeListener(this);
        checkBoxCode128.setOnCheckedChangeListener(this);

        addSpinnerScannerDevicesListener();

        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        updateStatus("EMDK open success!");
        this.emdkManager = emdkManager;
        // Acquire the barcode manager resources
        initBarcodeManager();
        // Enumerate scanner devices
        enumerateScannerDevices();
        // Set default scanner
        spinnerScannerDevices.setSelection(defaultIndex);

        //Create the Profile Config object
        //ProfileConfig profileConfigObj = new ProfileConfig();

        //Get the Profile Manager
        profileManager = (ProfileManager)emdkManager.getInstance(FEATURE_TYPE.PROFILE);

        //Create the new profile
        try{
            //EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET,profileConfigObj);
        }
        catch(Exception e){
            Log.d("scanner123", e.getMessage());
        }
        //Call process profile to modify the profile of specified profile name
        //EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, new String[]{});
    }

    private void modifyProfile_XMLString() {

        //Prepare XML to modify the existing profile
        String[] modifyData = new String[1];
        modifyData[0]=
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!--This is an auto generated document. Changes to this document may cause incorrect behavior.--><wap-provisioningdoc>\n" +
                        "  <characteristic type=\"ProfileInfo\">\n" +
                        "    <parm name=\"created_wizard_version\" value=\"7.3.2\"/>\n" +
                        "  </characteristic>\n" +
                        "  <characteristic type=\"Profile\">\n" +
                        "    <parm name=\"ProfileName\" value=\"ScannerTest\"/>\n" +
                        "    <parm name=\"ModifiedDate\" value=\"2019-07-11 13:28:47\"/>\n" +
                        "    <parm name=\"TargetSystemVersion\" value=\"4.2\"/>\n" +
                        "    <characteristic type=\"Barcode\" version=\"6.8\">\n" +
                        "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                        "      <parm name=\"scanner_input_enabled\" value=\"Default\"/>\n" +
                        "      <parm name=\"ScannerSelection\" value=\"AUTO\"/>\n" +
                        "      <parm name=\"use_auto\" value=\"Default\"/>\n" +
                        "      <parm name=\"trigger-wakeup\" value=\"Default\"/>\n" +
                        "      <characteristic type=\"Decoders\">\n" +
                        "        <parm name=\"decoder_upca\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_upce0\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_ean13\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_ean8\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_code128\" value=\"true\"/>\n" +
                        "        <parm name=\"decoder_code39\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_i2of5\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_gs1_databar\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_gs1_databar_lim\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_gs1_databar_exp\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_datamatrix\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_qrcode\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_pdf417\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_composite_ab\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_composite_c\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_microqr\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_aztec\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_maxicode\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_micropdf\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_uspostnet\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_usplanet\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_uk_postal\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_japanese_postal\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_australian_postal\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_canadian_postal\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_dutch_postal\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_us4state\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_us4state_fics\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_codabar\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_msi\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_code93\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_trioptic39\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_d2of5\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_chinese_2of5\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_korean_3of5\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_code11\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_tlc39\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_mailmark\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_hanxin\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_signature\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_webcode\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_matrix_2of5\" value=\"Default\"/>\n" +
                        "        <parm name=\"decoder_upce1\" value=\"Default\"/>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"DecoderParams\">\n" +
                        "        <characteristic type=\"UPCA\">\n" +
                        "          <parm name=\"decoder_upca_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_upca_preamble\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"UPCE0\">\n" +
                        "          <parm name=\"decoder_upce0_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_upce0_preamble\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_upce0_convert_to_upca\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"EAN8\">\n" +
                        "          <parm name=\"decoder_ean8_convert_to_ean13\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Code128\">\n" +
                        "          <parm name=\"decoder_code128_length1\" value=\"0\"/>\n" +
                        "          <parm name=\"decoder_code128_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_code128_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_enable_plain\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_enable_ean128\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_enable_isbt128\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_isbt128_concat_mode\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_check_isbt_table\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code128_security_level\" value=\"Default\"/>\n" +
                        "          <parm name=\"code128_enable_marginless_decode\" value=\"Default\"/>\n" +
                        "          <parm name=\"code128_ignore_fnc4\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Code39\">\n" +
                        "          <parm name=\"decoder_code39_length1\" value=\"0\"/>\n" +
                        "          <parm name=\"decoder_code39_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_code39_verify_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_full_ascii\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_convert_to_code32\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_report_code32_prefix\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code39_security_level\" value=\"Default\"/>\n" +
                        "          <parm name=\"code39_enable_marginless_decode\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Interleaved_2of5\">\n" +
                        "          <parm name=\"decoder_i2of5_length1\" value=\"14\"/>\n" +
                        "          <parm name=\"decoder_i2of5_length2\" value=\"10\"/>\n" +
                        "          <parm name=\"decoder_i2of5_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_i2of5_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_i2of5_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_itf14_convert_to_ean13\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_i2of5_security_level\" value=\"Default\"/>\n" +
                        "          <parm name=\"i20f5_enable_marginless_decode\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"GS1_Databar_Limited\">\n" +
                        "          <parm name=\"decoder_gs1_lim_security_level\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Composite_AB\">\n" +
                        "          <parm name=\"decoder_composite_ab_ucc_link_mode\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"US_Postnet\">\n" +
                        "          <parm name=\"decoder_uspostnet_report_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"US_Planet\">\n" +
                        "          <parm name=\"decoder_usplanet_report_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"UK_Postal\">\n" +
                        "          <parm name=\"decoder_uk_postal_report_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Codabar\">\n" +
                        "          <parm name=\"decoder_codabar_length1\" value=\"6\"/>\n" +
                        "          <parm name=\"decoder_codabar_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_codabar_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_codabar_clsi_editing\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_codabar_notis_editing\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"MSI\">\n" +
                        "          <parm name=\"decoder_msi_length1\" value=\"4\"/>\n" +
                        "          <parm name=\"decoder_msi_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_msi_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_msi_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_msi_check_digit_scheme\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_msi_report_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Code93\">\n" +
                        "          <parm name=\"decoder_code93_length1\" value=\"0\"/>\n" +
                        "          <parm name=\"decoder_code93_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_code93_redundancy\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Trioptic_39\">\n" +
                        "          <parm name=\"decoder_trioptic39_redundancy\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Discrete_2of5\">\n" +
                        "          <parm name=\"decoder_d2of5_length1\" value=\"0\"/>\n" +
                        "          <parm name=\"decoder_d2of5_length2\" value=\"14\"/>\n" +
                        "          <parm name=\"decoder_d2of5_redundancy\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Code11\">\n" +
                        "          <parm name=\"decoder_code11_length1\" value=\"4\"/>\n" +
                        "          <parm name=\"decoder_code11_length2\" value=\"55\"/>\n" +
                        "          <parm name=\"decoder_code11_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code11_verify_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_code11_report_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Han_Xin\">\n" +
                        "          <parm name=\"decoder_hanxin_inverse\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"Matrix_2of5\">\n" +
                        "          <parm name=\"decoder_matrix_2of5_length1\" value=\"10\"/>\n" +
                        "          <parm name=\"decoder_matrix_2of5_length2\" value=\"0\"/>\n" +
                        "          <parm name=\"decoder_matrix_2of5_redundancy\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_matrix_2of5_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_matrix_2of5_verify_check_digit\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "        <characteristic type=\"UPCE1\">\n" +
                        "          <parm name=\"decoder_upce1_report_check_digit\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_upce1_preamble\" value=\"Default\"/>\n" +
                        "          <parm name=\"decoder_upce1_convert_to_upca\" value=\"Default\"/>\n" +
                        "        </characteristic>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"UpcEanParams\">\n" +
                        "        <parm name=\"upcean_security_level\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_supplemental2\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_supplemental5\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_supplemental_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_retry_count\" value=\"10\"/>\n" +
                        "        <parm name=\"upcean_random_weight_check_digit\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_linear_decode\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_bookland\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_coupon\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_coupon_report\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_ean_zero_extend\" value=\"Default\"/>\n" +
                        "        <parm name=\"upcean_bookland_format\" value=\"Default\"/>\n" +
                        "        <parm name=\"databar_to_upc_ean\" value=\"Default\"/>\n" +
                        "        <parm name=\"upc_enable_marginless_decode\" value=\"Default\"/>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"ReaderParams\">\n" +
                        "        <parm name=\"aim_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"beam_timer\" value=\"15000\"/>\n" +
                        "        <parm name=\"Adaptive_Scanning\" value=\"Default\"/>\n" +
                        "        <parm name=\"Beam_Width\" value=\"Default\"/>\n" +
                        "        <parm name=\"power_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"mpd_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"reader_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"linear_security_level\" value=\"Default\"/>\n" +
                        "        <parm name=\"picklist\" value=\"Default\"/>\n" +
                        "        <parm name=\"aim_type\" value=\"5\"/>\n" +
                        "        <parm name=\"aim_timer\" value=\"1000\"/>\n" +
                        "        <parm name=\"same_barcode_timeout\" value=\"500\"/>\n" +
                        "        <parm name=\"different_barcode_timeout\" value=\"500\"/>\n" +
                        "        <parm name=\"illumination_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"keep_pairing_info_after_reboot\" value=\"Default\"/>\n" +
                        "        <parm name=\"lcd_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"low_power_timeout\" value=\"250\"/>\n" +
                        "        <parm name=\"delay_to_low_power_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"illumination_brightness\" value=\"10\"/>\n" +
                        "        <parm name=\"inverse_1d_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"viewfinder_size\" value=\"100\"/>\n" +
                        "        <parm name=\"viewfinder_posx\" value=\"0\"/>\n" +
                        "        <parm name=\"viewfinder_posy\" value=\"0\"/>\n" +
                        "        <parm name=\"1d_marginless_decode_effort_level\" value=\"Default\"/>\n" +
                        "        <parm name=\"poor_quality_bcdecode_effort_level\" value=\"Default\"/>\n" +
                        "        <parm name=\"charset_name\" value=\"Default\"/>\n" +
                        "        <parm name=\"viewfinder_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"scanning_mode\" value=\"Default\"/>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"ScanParams\">\n" +
                        "        <parm name=\"code_id_type\" value=\"Default\"/>\n" +
                        "        <parm name=\"volume_slider_type\" value=\"Default\"/>\n" +
                        "        <parm name=\"decode_audio_feedback_uri\" value=\"/system/media/audio/notifications/optimized-beep.ogg\"/>\n" +
                        "        <parm name=\"decode_haptic_feedback\" value=\"Default\"/>\n" +
                        "        <parm name=\"bt_disconnect_on_exit\" value=\"Default\"/>\n" +
                        "        <parm name=\"connection_idle_time\" value=\"600\"/>\n" +
                        "        <parm name=\"establish_connection_time\" value=\"45\"/>\n" +
                        "        <parm name=\"remote_scanner_audio_feedback_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"remote_scanner_led_feedback_mode\" value=\"Default\"/>\n" +
                        "        <parm name=\"display_bt_address_barcode\" value=\"Default\"/>\n" +
                        "        <parm name=\"good_decode_led_timer\" value=\"75\"/>\n" +
                        "        <parm name=\"decoding_led_feedback\" value=\"Default\"/>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"UDIParams\">\n" +
                        "        <parm name=\"enable_udi_gs1\" value=\"Default\"/>\n" +
                        "        <parm name=\"enable_udi_hibcc\" value=\"Default\"/>\n" +
                        "        <parm name=\"enable_udi_iccbba\" value=\"Default\"/>\n" +
                        "      </characteristic>\n" +
                        "      <characteristic type=\"MultiBarcodeParams\">\n" +
                        "        <parm name=\"multi_barcode_count\" value=\"5\"/>\n" +
                        "      </characteristic>\n" +
                        "    </characteristic>\n" +
                        "  </characteristic>\n" +
                        "</wap-provisioningdoc>\n";

        new ProcessProfileTask().execute(modifyData[0]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground
        if (emdkManager != null) {
            // Acquire the barcode manager resources
            initBarcodeManager();
            // Enumerate scanner devices
            enumerateScannerDevices();
            // Set selected scanner
            spinnerScannerDevices.setSelection(scannerIndex);
            // Initialize scanner
            initScanner();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background
        // Release the barcode manager resources
        deInitScanner();
        deInitBarcodeManager();
    }

    @Override
    public void onClosed() {
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {
                updateData("<font color='gray'>" + data.getLabelType() + "</font> : " + data.getData());
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                updateStatus(statusString);
                // set trigger type
                if(bSoftTriggerSelected) {
                    scanner.triggerType = TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    scanner.triggerType = TriggerType.HARD;
                }
                // set decoders
                if(bDecoderSettingsChanged) {
                    setDecoders();
                    bDecoderSettingsChanged = false;
                }
                // submit read
                if(!scanner.isReadPending() && !bExtScannerDisconnected) {
                    try {
                        scanner.read();
                    } catch (ScannerException e) {
                        updateStatus(e.getMessage());
                    }
                }
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                updateStatus(statusString);
                break;
            case SCANNING:
                statusString = "Scanning...";
                updateStatus(statusString);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                updateStatus(statusString);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                updateStatus(statusString);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {
        String status;
        String scannerName = "";
        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();
        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }
        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {
            switch(connectionState) {
                case CONNECTED:
                    bSoftTriggerSelected = false;
                    synchronized (lock) {
                        initScanner();
                        bExtScannerDisconnected = false;
                    }
                    break;
                case DISCONNECTED:
                    bExtScannerDisconnected = true;
                    synchronized (lock) {
                        deInitScanner();
                    }
                    break;
            }
            status = scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
        else {
            bExtScannerDisconnected = false;
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
    }

    private void initScanner() {
        if (scanner == null) {
            if ((deviceList != null) && (deviceList.size() != 0)) {
                if (barcodeManager != null)
                    scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
            }
            else {
                updateStatus("Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                    deInitScanner();
                }
            }else{
                updateStatus("Failed to initialize the scanner device.");
            }
        }
    }

    private void deInitScanner() {
        if (scanner != null) {
            try{
                scanner.disable();
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }

            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }

            try{
                scanner.release();
            } catch (Exception e) {
                updateStatus(e.getMessage());
            }
            scanner = null;
        }
    }

    private void initBarcodeManager(){
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }
    }

    private void deInitBarcodeManager(){
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }

    private void addSpinnerScannerDevicesListener() {
        spinnerScannerDevices.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {
                if ((scannerIndex != position) || (scanner==null)) {
                    scannerIndex = position;
                    bSoftTriggerSelected = false;
                    bExtScannerDisconnected = false;
                    deInitScanner();
                    initScanner();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void enumerateScannerDevices() {
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();
            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                updateStatus("Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }

    private void setDecoders() {
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                // Set EAN8
                config.decoderParams.ean8.enabled = checkBoxEAN8.isChecked();
                // Set EAN13
                config.decoderParams.ean13.enabled = checkBoxEAN13.isChecked();
                // Set Code39
                config.decoderParams.code39.enabled= checkBoxCode39.isChecked();
                //Set Code128
                config.decoderParams.code128.enabled = checkBoxCode128.isChecked();

                //config.ReaderParams.ReaderSpecific.LaserSpecific =
                //myBarcode2.Config.Reader.ReaderSpecific.ImagerSpecific.AimType = AIM_TYPE.AIM_TYPE_TRIGGER;

                scanner.setConfig(config);
            } catch (ScannerException e) {
                updateStatus(e.getMessage());
            }
        }
    }

    public void softScan(View view) {
        bSoftTriggerSelected = true;
        cancelRead();
    }
    public void modifyProfile(View view) {
        //new ProcessProfileTask().execute();
        modifyProfile_XMLString();
    }

    private void cancelRead(){
        if (scanner != null) {
            if (scanner.isReadPending()) {
                try {
                    scanner.cancelRead();
                } catch (ScannerException e) {
                    updateStatus(e.getMessage());
                }
            }
        }
    }

    private void updateStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText("" + status);
            }
        });
    }

    private void updateData(final String result){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result != null) {
                    if(dataLength ++ > 100) { //Clear the cache after 100 scans
                        textViewData.setText("");
                        dataLength = 0;
                    }
                    textViewData.append(Html.fromHtml(result));
                    textViewData.append("\n");
                    ((View) findViewById(R.id.scrollViewData)).post(new Runnable()
                    {
                        public void run()
                        {
                            ((ScrollView) findViewById(R.id.scrollViewData)).fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private void setDefaultOrientation(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if(width > height){
            setContentView(R.layout.activity_main_landscape);
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        bDecoderSettingsChanged = true;
        cancelRead();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }




    // Method to parse the XML response using XML Pull Parser
    public void parseXML(XmlPullParser myParser) {
        int event;
        try {
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = myParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:

                        if (name.equals("parm-error")) {
                            parmName = myParser.getAttributeValue(null, "name");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";
                            return;
                        }

                        if (name.equals("characteristic-error")) {
                            errorType = myParser.getAttributeValue(null, "type");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Type: " + errorType + ", Error Description: " + errorDescription + ")";
                            return;
                        }

                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }
                event = myParser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ProcessProfileTask extends AsyncTask<String, Void, EMDKResults> {

        @Override
        protected EMDKResults doInBackground(String... params) {

            EMDKResults results = null;
            try {
                //Call process profile to modify the profile of specified profile name
                results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);
            }
            catch(Exception e)
            {
                ;
            }
            return results;
        }

        @Override
        protected void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    parseXML(parser);

                    if ( TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription) ) {

                        resultString = "Profile update success.";
                    }
                    else {

                        resultString = "Profile update failed." + errorString;
                    }

                } catch (XmlPullParserException e) {
                    resultString =  e.getMessage();
                }
            }
            Log.e("scanner123", resultString);
            //statusTextView.setText(resultString);
        }
    }
}
