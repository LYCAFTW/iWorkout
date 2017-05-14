/******************************************************************************
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.iotlyca.app.wicedsense;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.iotlyca.app.ledevicepicker.DevicePicker;
import com.iotlyca.app.ledevicepicker.DevicePickerActivity;
import com.iotlyca.app.wicedsense.Settings.SettingChangeListener;
import com.iotlyca.app.wicedsmart.ota.OtaAppInfo;
import com.iotlyca.app.wicedsmart.ota.ui.OtaAppInfoFragment;
import com.iotlyca.app.wicedsmart.ota.ui.OtaResource;
import com.iotlyca.app.wicedsmart.ota.ui.OtaUiHelper;
import com.iotlyca.app.wicedsmart.ota.ui.OtaUiHelper.OtaUiCallback;
import com.iotlyca.ui.BluetoothEnabler;
import com.iotlyca.ui.ExitConfirmUtils;
import com.iotlyca.ui.ExitConfirmFragment.ExitConfirmCallback;
import com.shinelw.library.ColorArcProgressBar;

import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Manaages the main view and gauges for each sensor
 *
 */
public class MainActivity extends AppCompatActivity implements
        DevicePicker.Callback, Handler.Callback, OnClickListener, ExitConfirmCallback,
        OtaUiCallback, SettingChangeListener {
    private static final String TAG = Settings.TAG_PREFIX + "MainActivity";
    private static final boolean DBG_LIFECYCLE = true;
    private static final boolean DBG = Settings.DBG;

    private static final int COMPLETE_INIT = 800;
    private static final int PROCESS_SENSOR_DATA_ON_UI = 801;
    private static final int PROCESS_BATTERY_STATUS_UI = 802;
    private static final int PROCESS_EVENT_DEVICE_UNSUPPORTED = 803;private static final int PROCESS_CONNECTION_STATE_CHANGE_UI = 804;

    private static final String FRAGMENT_TEMP = "fragment_temp";

    private static final String FRAGMENT_HUMD = "fragment_humd";
    private static final String FRAGMENT_PRES = "fragment_pres";
    private static final String FRAGMENT_GYRO = "fragment_gyro";
    private static final String FRAGMENT_COMPASS = "fragment_compass";
    private static final String FRAGMENT_ACCELEROMETER = "fragment_accelerometer";


    /**
     * Handles Bluetooth on/off events. If Bluetooth is turned off, exit this
     * app
     *
     * @author Fred Chen
     *
     */
    private class BluetoothStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mSensorDataEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (btState) {

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        exitApp();
                        break;
                    }
                }
            });
        }
    }

    private class UiHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {

            // These events run on the mUiHandler on the UI Main Thread
            case COMPLETE_INIT:
                initResourcesAndResume();
                break;
            case PROCESS_EVENT_DEVICE_UNSUPPORTED:
                Toast.makeText(getApplicationContext(), R.string.error_unsupported_device,
                        Toast.LENGTH_SHORT).show();
                break;
            case PROCESS_CONNECTION_STATE_CHANGE_UI:
                updateConnectionStateWidgets();
                break;
            case PROCESS_BATTERY_STATUS_UI:
                updateBatteryLevelWidget(msg.arg1);
                break;
            case PROCESS_SENSOR_DATA_ON_UI:
                processSensorData((byte[]) msg.obj);
                break;
            }
            return true;
        }
    };

    private Button mButtonConnectDisconnect;
    // private TemperatureFragment mTemperatureFrag;

    // private PressureFragment mPressureFrag;

    private GyroFragment mGyroFrag;
    // private CompassFragment mCompassFrag;
    private AccelerometerFragment mAccelerometerFrag;
//    private ImageView mBatteryStatusIcon;
//    private TextView mBatteryStatusText;
//    private View mBatteryStatusView;
    private DevicePicker mDevicePicker;
    private String mDevicePickerTitle;
    private int mLastBatteryStatus = -1;
    private boolean mConnectDisconnectPending;
    private SenseManager mSenseManager;
    private Handler mUiHandler;
    private final BluetoothStateReceiver mBtStateReceiver = new BluetoothStateReceiver();
    private ExitConfirmUtils mExitConfirm;
    private int mInitState;
    private Handler mSensorDataEventHandler;
    private HandlerThread mSensorDataEventThread;
    private final AnimationManager mAnimation = new AnimationManager(
            Settings.ANIMATION_FRAME_DELAY_MS, Settings.ANIMATE_TIME_INTERVAL_MS);
//    private final AnimationManager mAnimationSlower = new AnimationManager(
//            Settings.ANIMATION_FRAME_DELAY_MS, Settings.ANIMATE_TIME_INTERVAL_MS);
    private final OtaUiHelper mOtaUiHelper = new OtaUiHelper();
    private boolean mShowAppInfoDialog;
    private boolean mFirmwareUpdateCheckPending = false;
    private boolean mCanAskForFirmwareUpdate = false;
    private boolean mMandatoryUpdateRequired = false;
//    private boolean mIsTempScaleF = false;

    /**
     * Initialize async resources in series
     *
     * @return
     */
    private boolean initResourcesAndResume() {
        switch (mInitState) {
        case 0:
            // Check if license accepted. If not, prompt user
            mInitState++;
        case 1:
            // Check if BT is on, If not, prompt user
            if (!BluetoothEnabler.checkBluetoothOn(this)) {
                return false;
            }
            mInitState++;
            SenseManager.init(this);
        case 2:
            // Check if sense manager initialized. If not, keep waiting
            if (waitForSenseManager()) {
                return false;
            }
            mInitState = -1;
            checkDevicePicked();
        }
        mSenseManager.registerEventCallbackHandler(mSensorDataEventHandler);

        if (mSenseManager.isConnectedAndAvailable()) {
            mSenseManager.enableNotifications(true);
        }
        updateConnectionStateWidgets();
        //updateTemperatureScaleType();
        //updateGyroState();
        updateAccelerometerState();
        //updateCompassState();
        Settings.addChangeListener(this);
        return true;
    }

    private void updateGyroState() {
        if (mGyroFrag != null) {
            mGyroFrag.setEnabled(Settings.gyroEnabled());
        }
    }

    private void updateAccelerometerState() {
        if (mAccelerometerFrag != null) {
            mAccelerometerFrag.setEnabled(Settings.accelerometerEnabled());
        }

    }

//    private void updateCompassState() {
//        if (mCompassFrag != null) {
//            mCompassFrag.setEnabled(Settings.compassEnabled());
//        }
//    }

    /**
     * Acquire reference to the SenseManager serivce....This is asynchronous
     *
     * @return
     */
    private boolean waitForSenseManager() {
        // Check if the SenseManager is available. If not, keep retrying
        mSenseManager = SenseManager.getInstance();
        if (mSenseManager == null) {
            mUiHandler.sendEmptyMessageDelayed(COMPLETE_INIT, Settings.SERVICE_INIT_TIMEOUT_MS);
            return true;
        }
        return false;
    }

    /**
     * Exit the application and cleanup resources
     */
    protected void exitApp() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "exitApp");
        }
        SenseManager.destroy();
        finish();
    }

    /**
     * Update the battery level UI widgets
     *
     * @param batteryLevel
     */
    private void updateBatteryLevelWidget(int batteryLevel) {
        mLastBatteryStatus = batteryLevel;
        invalidateOptionsMenu();
    }

    /**
     * Update all UI components related to the connection state
     */
    private void updateConnectionStateWidgets() {
        mConnectDisconnectPending = false;
        if (mButtonConnectDisconnect != null) {
            if (mSenseManager.getDevice() == null) {
                mButtonConnectDisconnect.setEnabled(false);
                mButtonConnectDisconnect.setText(R.string.no_device);
                return;
            }
            if (!mButtonConnectDisconnect.isEnabled()) {
                mButtonConnectDisconnect.setEnabled(true);
            }
            if (mSenseManager.isConnectedAndAvailable()) {
                mButtonConnectDisconnect.setText(R.string.disconnect);
            } else {
                mButtonConnectDisconnect.setText(R.string.connect);
                postData();
            }
            mButtonConnectDisconnect.setEnabled(true);
        }
        invalidateOptionsMenu();
    }

    private void postData() {
        if (process > 1) {
            String postdata = exerciseName + '/' + Integer.toString((int)(this.process/2)) + '/' + weight;
            Log.d("postdata", postdata);
            process = 0;
            bar.setCurrentValues(process);
            gravity = new int[3];
            gravityLabel = 0;
            max = 0;
            min = 0;
            leisure = 0;
            window = new ArrayList<Integer>();
            HashMap<String,String> data = new HashMap<>();
            //                post
            data.put("body",postdata);
            data.put("email",email);
            data.put("password",password);
            new Post().execute(data);
        }
    }

    private class Post extends AsyncTask<HashMap<String,String>, Void, String> {

        //  The API_Endpoint is hold on an old AWS EC2 and no longer valid, please re-deploy the end point
        String API_Endpoint = Settings.API_Endpoint;
        public Post() {
            super();
        }

        @Override
        protected String doInBackground(HashMap<String,String>... params) {
            String status = "";

            HttpURLConnection urlConnection = null;
            try {
                JSONObject payload = new JSONObject();
                for(Map.Entry<String,String> entry: params[0].entrySet())
                    payload.put(entry.getKey(),entry.getValue());
                System.out.println(payload.toString());
                URL url;
                url = new URL(API_Endpoint+"/posts/");
                System.out.println(url);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setDoInput(true);

                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");

                String authStr = params[0].get("email")+":"+params[0].get("password");
                System.out.println(authStr);

                // encode data using BASE64
                String authEncoded = Base64.encodeToString(authStr.getBytes(), Base64.DEFAULT);

                urlConnection.setRequestProperty("Authorization", "Basic "+authEncoded);

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());

                wr.write(payload.toString());

                wr.flush();

                //display what returns the POST request

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    System.out.println("OK lalala" + sb.toString());

                } else {
                    System.out.println(urlConnection.getResponseMessage());
                }

                status = Integer.toString(urlConnection.getResponseCode());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("HTTPPost", e.toString());
            } finally {
                //Disconnect. Once the response body has been read, the HttpURLConnection should be closed by calling disconnect(). Disconnecting releases the resources held by a connection so they may be closed or reused.
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return status;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("HTTPPost", "Payload:" + result);
        }
    }


    /**
     * Initialize the exit confirmation dialog
     */
    private void initExitConfirm() {
        mExitConfirm = new ExitConfirmUtils(this);
    }

    /*
     * Initialize the device picker
     *
     * @return
     */
    private void initDevicePicker() {
        mDevicePickerTitle = getString(R.string.title_devicepicker);
        mDevicePicker = new DevicePicker(this, Settings.PACKAGE_NAME,
                DevicePickerActivity.class.getName(), this,
                Uri.parse("content://com.brodcom.app.wicedsense/device/pick"));
        mDevicePicker.init();
    }

    /**
     * Launch the device picker
     */
    private void launchDevicePicker() {
        mDevicePicker.launch(mDevicePickerTitle, null, null);
    }

    /**
     * Cleanup the device picker
     */
    private void cleanupDevicePicker() {
        if (mDevicePicker != null) {
            mDevicePicker.cleanup();
            mDevicePicker = null;
        }
    }

    /**
     * Check if a device has been picked, and launch the device picker if not...
     *
     * @return
     */
    private boolean checkDevicePicked() {
        if (mSenseManager != null && mSenseManager.getDevice() != null) {
            return true;
        }
        // Launch device picker
        launchDevicePicker();
        return false;
    }

    /**
     * Start the connect or disconnect, based on the current state of the device
     */
    private void doConnectDisconnect() {
        if (!mSenseManager.isConnectedAndAvailable()) {
            if (!mSenseManager.connect()) {
                updateConnectionStateWidgets();
            }
        } else {
            if (!mSenseManager.disconnect()) {
                updateConnectionStateWidgets();
            }
        }
    }

    private ColorArcProgressBar bar;
    private String exerciseName;
    String email;
    String password;
    String weight;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onCreate " + this);
        }

        Intent intent = getIntent();
        exerciseName = intent.getStringExtra("exercise");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        email = sharedPreferences.getString("email", "");
        password = sharedPreferences.getString("password", "");

        FragmentManager fMgr = getFragmentManager();
        //mPressureFrag = (PressureFragment) fMgr.findFragmentByTag(FRAGMENT_PRES);
        mGyroFrag = (GyroFragment) fMgr.findFragmentByTag(FRAGMENT_GYRO);
        //mCompassFrag = (CompassFragment) fMgr.findFragmentByTag(FRAGMENT_COMPASS);
        mButtonConnectDisconnect = (Button) findViewById(R.id.connection_state);
        if (mButtonConnectDisconnect != null) {
            mButtonConnectDisconnect.setOnClickListener(this);
            mButtonConnectDisconnect.setEnabled(false);
        } else {
            // large screen sizes do not have button in the main layout. Instead
            // it's an action button in the menu button
        }
        mAccelerometerFrag = (AccelerometerFragment) fMgr.findFragmentByTag(FRAGMENT_ACCELEROMETER);

        // Initialize dialogs
        initDevicePicker();
        initExitConfirm();

        mInitState = 0;

        // Register bluetooth state receiver
        registerReceiver(mBtStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Start event handler thread
        mSensorDataEventThread = new HandlerThread("WicedSenseEvent");
        mSensorDataEventThread.start();
        mSensorDataEventHandler = new Handler(mSensorDataEventThread.getLooper(), this);

        // Start ui handler
        mUiHandler = new Handler(new UiHandlerCallback());

        // Register components for frequent animation
        mAnimation.addAnimated(mAccelerometerFrag);
        //mAnimation.addAnimated(mCompassFrag);
        mAnimation.addAnimated(mGyroFrag);

        // set up the spinner view
        initSpinner();

        bar = (ColorArcProgressBar) findViewById(R.id.bar1);
        bar.setMaxValues(20);

    }

    private void initSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner_time);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                String[] array = getResources().getStringArray(R.array.times);
                bar.setMaxValues(Integer.parseInt(array[pos]));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        spinner = (Spinner) findViewById(R.id.spinner_weight);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                String[] array = getResources().getStringArray(R.array.weight);
                weight = array[pos];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    @Override
    protected void onResume() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onResume " + this);
        }
        super.onResume();
        initResourcesAndResume();
    }

    @Override
    protected void onPause() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onPause " + this);
        }
        mExitConfirm.dismiss();

        Settings.removeChangeListener(this);

        // Disable notifications if the application is backgrounded, but don't
        // disconnect from the device
        if (mSenseManager != null) {
            if (mSenseManager.isConnectedAndAvailable()) {
                mSenseManager.enableNotifications(false);
            }
            mSenseManager.unregisterEventCallbackHandler(mSensorDataEventHandler);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDestroy " + this);
        }

        mSensorDataEventThread.quit();
        cleanupDevicePicker();
        unregisterReceiver(mBtStateReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean isDeviceSelected = (mSenseManager != null && mSenseManager.getDevice() != null);
        boolean isDeviceConnected = isDeviceSelected && mSenseManager.isConnectedAndAvailable();

        // Check if we are in landscape mode. If so, update the state of the
        // connect/disconnect action
        if (mButtonConnectDisconnect == null) {
            // Get Connect/disconnect button
            MenuItem menuConnectDisconnect = menu.findItem(R.id.action_connectdisconnect);
            if (menuConnectDisconnect != null) {
                // Landscape mode
                if (!isDeviceSelected) {
                    // No device selected: hide connect/disconnect button
                    // menuConnectDisconnect.setVisible(false);
                    menuConnectDisconnect.setEnabled(false);

                } else {
                    menuConnectDisconnect.setEnabled(!mConnectDisconnectPending);
                    if (isDeviceConnected) {
                        menuConnectDisconnect.setTitle(R.string.disconnect);
                    } else {
                        menuConnectDisconnect.setTitle(R.string.connect);
                    }
                }
            }
        }

        // Update the update firmware button
        MenuItem updateFw = menu.findItem(R.id.update_fw);
        if (updateFw != null) {
            updateFw.setEnabled(isDeviceSelected);
        }

        // Update the get firmware info button
        MenuItem getFwInfo = menu.findItem(R.id.get_fw_info);
        if (getFwInfo != null) {
            getFwInfo.setEnabled(isDeviceConnected);
        }

        // Update the pick device menu: only allow a pick device from the
        // disconnected state
        MenuItem pick = menu.findItem(R.id.action_pick);
        if (pick != null) {
            pick.setEnabled(!isDeviceConnected);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Invoked when a menu option is picked
     */

    /**
     * Callback invoked when a device is selected from the device picker
     */
    @Override
    public void onDevicePicked(BluetoothDevice device) {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDevicePicked");
        }
        if (Settings.CHECK_FOR_UPDATES_ON_CONNECT) {
            mCanAskForFirmwareUpdate = true;
        } else {
            mCanAskForFirmwareUpdate = false;
        }
        mSenseManager.setDevice(device);
        updateConnectionStateWidgets();
    }

    /**
     * Callback invoked when the user aborts picking a device from the device
     * picker
     */
    @Override
    public void onDevicePickCancelled() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDevicePickCancelled");
        }
        updateConnectionStateWidgets();
    }

    /**
     * Handler callback used for two purposes
     *
     * 1. This callback is invoked by the event handler loop when the
     * SenseManager service sends a event from the sensor tag using the
     * mEventHandler object. The event handler loop runs in a child thread, so
     * that it can queue up events and allow the SenseManager (and Bluetooth
     * callbacks) to return asynchronously before the UI processes the event.
     * The event handler loop reposts the event to the main UI handler loop via
     * the mUiHandler Handler
     *
     * 2. This callback is invoked by the mEventHandler object to run a UI
     * operation in the main event loop of the application
     *
     *
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case SenseManager.EVENT_DEVICE_UNSUPPORTED:
            mUiHandler.sendEmptyMessage(PROCESS_EVENT_DEVICE_UNSUPPORTED);
            break;
        case SenseManager.EVENT_CONNECTED:
            mUiHandler.sendEmptyMessage(PROCESS_CONNECTION_STATE_CHANGE_UI);
            onConnected();
            break;
        case SenseManager.EVENT_DISCONNECTED:
            mUiHandler.sendEmptyMessage(PROCESS_CONNECTION_STATE_CHANGE_UI);
            break;
        case SenseManager.EVENT_BATTERY_STATUS:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_BATTERY_STATUS_UI, msg.arg1,
                    msg.arg1));
            break;
        case SenseManager.EVENT_SENSOR_DATA:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_SENSOR_DATA_ON_UI, msg.obj));
            break;
        case SenseManager.EVENT_APP_INFO:
            boolean success = msg.arg1 == 1;
            OtaAppInfo appInfo = (OtaAppInfo) msg.obj;
            if (DBG) {
                Log.d(TAG, "EVENT_APP_INFO: success=" + success + ",otaAppInfo=" + appInfo);
            }
            if (mFirmwareUpdateCheckPending) {
                mFirmwareUpdateCheckPending = false;
                checkForFirmwareUpdate(appInfo);
                break;
            }

            if (mShowAppInfoDialog) {
                mShowAppInfoDialog = false;
                if (success) {
                    OtaAppInfoFragment mOtaAppInfoFragment = OtaAppInfoFragment.createDialog(
                            mSenseManager.getDevice(), appInfo);
                    mOtaAppInfoFragment.show(getFragmentManager(), null);
                }
            }
            break;
        }
        return true;
    }

    private long mLastRefreshTimeMs;
    private long mLastRefreshSlowerTimeMs;

    /**
     * Parses the sensor data bytes and updates the corresponding sensor(s) UI
     * component
     *
     * @param sensorData
     */
    // add some operations in this function by guangyang

    int [] gravity = new int [3]; // this is the gravity
    int gravityLabel = 0;
    int process = 0;
    ArrayList<Integer> window = new ArrayList<>();
    int max = 0;
    int min = 0;
    int leisure = 0;
    private void processSensorData(byte[] sensorData) {
        if (mAnimation != null && mAnimation.useAnimation()) {
            mAnimation.init();
        }

//        if (mAnimationSlower != null && mAnimationSlower.useAnimation()) {
//            mAnimationSlower.init();
//        }

        int maskField = sensorData[0];
        int offset = 0;
        int[] values = new int[3];
        boolean updateView = false;
        long currentTimeMs = System.currentTimeMillis();

        if (sensorData.length == 19) {

            if (currentTimeMs - mLastRefreshTimeMs < Settings.REFRESH_INTERVAL_MS) {
                return;
            } else {
                mLastRefreshTimeMs = currentTimeMs;
            }

            // packet type specifying accelerometer, gyro, magno
            offset = 1;
            int[] temp = new int[3];
            if (SensorDataParser.accelerometerHasChanged(maskField)) {
                if (Settings.accelerometerEnabled()) {
                    SensorDataParser.getAccelorometerData(sensorData, offset, values);
                    //mAccelerometerFrag.setValue(mAnimation, values[0], values[1], values[2]);
                    //updateView = true;
//                    double [] linear_acceleration = new double[3];
//                    temp = values;
// here we try version two to use high frequency filter
//                    Log.v("accerler", Integer.toString(values[0])
//                                    + " " + Integer.toString(values[1])
//                                    + " " + Integer.toString(values[2]));
//                    if (gravity[0] == 0 && gravity[1] == 0 && gravity[2] == 0) {}
//                    else {
//                        double alpha = 0.9;
//
//                        // 用低通滤波器分离出重力加速度
//                        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
//                        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
//                        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];
//
//                        // 用高通滤波器剔除重力干扰
//                        linear_acceleration[0] = values[0] - gravity[0];
//                        linear_acceleration[1] = values[1] - gravity[1];
//                        linear_acceleration[2] = values[2] - gravity[2];
//
//                        double acc_g = ( gravity[0] * linear_acceleration[0]
//                                + gravity[1] * linear_acceleration[1]
//                                + gravity[2] * linear_acceleration[2] );
//                        if (gravityLabel == 0) gravityLabel = acc_g > 0 ? 1 : -1;
//                        if (gravityLabel * acc_g < 0) {
//                            gravityLabel = acc_g > 0 ? 1 : -1;;
//                            process += 0.5;
//                            bar.setCurrentValues(process);
//
//                        }
//                    }
// this is version one and did not work well
                    // detect a peak -- way1

                    int square = values[0] * values[0] + values[1] * values[1] + values[2] * values[2];
                    if (max == 0) {
                        max = square; min = square;
                    } else {
                        if (leisure < 7) leisure ++;
                        else {
                            max = square > max ? square : max;
                            min = square < min ? square : min;
                            Log.d("max0min", Integer.toString(max - min) + " " + Integer.toString(square));
                            if (max - min > 3000) {
                                max = 0;
                                min = 0;
                                process += 1;
                                if (process % 2 == 0) bar.setCurrentValues(process/2);
                                leisure = 0;
                            }
                        }
                    }
//                    int square = ( gravity[0] * ( values[0] - gravity[0])
//                                + gravity[1] * ( values[1] - gravity[1])
//                                + gravity[2] * ( values[2] - gravity[2]) );

//                    if (window.size() < 3 ) window.add(square);
//                    else {
//                        window.remove(0);
//                        window.add(square);
//                        int interval1 = window.get(0) - window.get(1);
//                        int interval2 = window.get(1) - window.get(2);
////                        int interval3 = window.get(3) - window.get(2);
////                        int interval4 = window.get(4) - window.get(3);
//                        Log.d("interval", Integer.toString(interval1) + " " + Integer.toString(interval2));
//                        //if (interval1 * interval2 > 0 && interval2 * interval3 < 0 && interval3 * interval4 > 0) {
//                        if (interval1 * interval2 < 0 && Math.abs(interval1) > 200 && Math.abs(interval2) > 200) {
//                            process += 0.5;
//                            bar.setCurrentValues(process);
//                            window = new ArrayList<Integer>();
//                        }
//                    }

                   temp = values;
//                    if (gravity[0] == 0 && gravity[1] == 0 && gravity[2] == 0) {
//                    } else {
//                        int acc_g = ( gravity[0] * ( values[0] - gravity[0])
//                                + gravity[1] * ( values[1] - gravity[1])
//                                + gravity[2] * ( values[2] - gravity[2]) );
//                        if (gravityLabel == 0) gravityLabel = acc_g;
//                        if (gravityLabel * acc_g < 0) {
//                            gravityLabel = acc_g;
//                            process += 0.5;
//                            bar.setCurrentValues(process);
//
//                        }
//                        Log.v("label",Integer.toString(acc_g));
//                    }
                    Log.v("gravity", Integer.toString(values[0]) + " " + Integer.toString(values[1]) + " " + Integer.toString(values[2]));
                }
                offset += SensorDataParser.SENSOR_ACCEL_DATA_SIZE;
            }

            if (SensorDataParser.gyroHasChanged(maskField)) {
                if (Settings.gyroEnabled()) {
                    SensorDataParser.getGyroData(sensorData, offset, values);
                    if (gravity[0] == 0 && gravity[1] == 0 && gravity[2] == 0) {
                        gravity[0] = temp[0];
                        gravity[1] = temp[1];
                        gravity[2] = temp[2];
                    }
                    //mGyroFrag.setValue(mAnimation, values[0], values[1], values[2]);
                    updateView = true;
                }
                offset += SensorDataParser.SENSOR_GYRO_DATA_SIZE;
            }

//            if (SensorDataParser.magnetometerHasChanged(maskField)) {
//                SensorDataParser.getMagnometerData(sensorData, offset, values);
//                Log.v("magno", Integer.toString(values[0])
//                        + " " + Integer.toString(values[1])
//                        + " " + Integer.toString(values[2]));
//                offset += SensorDataParser.SENSOR_MAGNO_DATA_SIZE;
//            }

            if (updateView && mAnimation != null) {
                mAnimation.animate();
            }
        }

        // If animation is enabled, call animate...
    }

    /**
     * Callback invoked when the connect/disconnect button is clicked or the
     * battery status button is clicked
     */
    @Override
    public void onClick(View v) {

        // Process connect/disconnect request
        if (v == mButtonConnectDisconnect) {
            // Temporary disable the button while a connect/disconnect is
            // pending
            mConnectDisconnectPending = true;
            mButtonConnectDisconnect.setEnabled(false);
            doConnectDisconnect();
        }

        // Process battery status request
//        else if (v == mBatteryStatusView) {
//            if (mSenseManager != null) {
//                mSenseManager.getBatteryStatus();
//            }
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothEnabler.REQUEST_ENABLE_BT) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                exitApp();
                return;
            }
            initResourcesAndResume();
        }
    }

    /**
     * Show exit confirmation dialog if user presses the button
     */
    @Override
    public void onBackPressed() {
        mExitConfirm.show(getFragmentManager());
    }

    /**
     * Callback invoked when the user selects "ok" from the exit confirmation
     * dialog
     */
    @Override
    public void onExit() {
        exitApp();
    }

    /**
     * Callback invoked when the user cancels exitting the application.
     */
    @Override
    public void onExitCancelled() {
    }

    /**
     * Callback invoked when the OTA firmware update has completed
     *
     * @param completed
     *            : if true, OTA upgrade was successful, false otherwise.
     */
    @Override
    public void onOtaFinished(boolean completed) {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onOtaFinished");
        }

        // If OTA did not complete and the patch was mandatory, disconnect
        if (!completed && mMandatoryUpdateRequired) {
            if (mSenseManager != null) {
                mSenseManager.disconnect();
            }
            return;
        }

        // Enable notifications
        if (mSenseManager != null) {
            mSenseManager.setOtaUpdateMode(false);
        }
    }

    /**
     * Start a request to read application id, major version,minor version of a
     * connected WICED Sense tag...
     */
    private void getFirmwareInfo() {
        if (mSenseManager != null) {
            mShowAppInfoDialog = true;
            mSenseManager.getAppInfo();
        }
    }

    /**
     * Check for firmware update, if the user allows it or if there is a
     * mandatory update. The connection is assumed to be up
     */
    private void checkForFirmwareUpdate() {
        if (mSenseManager == null) {
            return;
        }
        // Check if we are connected
        if (!mSenseManager.isConnectedAndAvailable()) {
            mCanAskForFirmwareUpdate = true;
            boolean success = mSenseManager.connect();
            if (!success) {
                mCanAskForFirmwareUpdate = false;
            }
        } else {
            mCanAskForFirmwareUpdate = true;
            checkForFirmwareUpdateIfAllowed();
        }
    }

    /**
     * Check for firmware update, if the user allows it. The connection is
     * assumed to be up
     *
     * @return
     */
    private boolean checkForFirmwareUpdateIfAllowed() {
        if (!mCanAskForFirmwareUpdate && !Settings.hasMandatoryUpdate()) {
            if (DBG) {
                Log.d(TAG, "firmwareUpdateCheck(): user opted out...skipping..");
            }
            return false;
        }
        mFirmwareUpdateCheckPending = true;
        if (!mSenseManager.getAppInfo()) {
            mFirmwareUpdateCheckPending = false;
            if (DBG) {
                Log.d(TAG, "checkForFirmwareUpdates(): unable to get app info");
            }
            return false;
        }
        if (DBG) {
            Log.d(TAG, "firmwareUpdateCheck(): getting app info");
        }
        return true;
    }

    private void onConnected() {
        if (checkForFirmwareUpdateIfAllowed()) {
            // Wait for firmware check...
            if (DBG) {
                Log.d(TAG, "onConnected:Checking for firmware updates..");
            }
        } else {
            if (DBG) {
                Log.d(TAG, "onConnected: enabling notifications");
            }
            if (mSenseManager != null) {
                mSenseManager.enableNotifications(true);
            }
        }
    }

    private boolean canUpdateToFirmware(OtaAppInfo appInfo, OtaResource otaResource) {
        if (otaResource == null || appInfo == null) {
            return false;
        }
        if (otaResource.getMajor() <= 0) {
            return true;
        }
        if (appInfo.mMajorVersion < otaResource.getMajor()) {
            return true;
        } else if (appInfo.mMajorVersion == otaResource.getMajor()
                && appInfo.mMinorVersion < otaResource.getMinor()) {
            return true;
        }
        return false;
    }

    private void checkForFirmwareUpdate(OtaAppInfo appInfo) {
        mCanAskForFirmwareUpdate = false;
        mMandatoryUpdateRequired = false;

        ArrayList<OtaResource> otaResources = new ArrayList<OtaResource>();
        OtaResource defaultResource = Settings.getDefaultOtaResource();
        if (defaultResource != null && canUpdateToFirmware(appInfo, defaultResource)) {
            mMandatoryUpdateRequired = defaultResource.isMandatory();
            otaResources.add(defaultResource);
        }
        if (!mMandatoryUpdateRequired) {
            OtaUiHelper.createOtaResources(Settings.getOtaDirectory(), Settings.getOtaFileFilter(),
                    otaResources);
            Iterator<OtaResource> i = otaResources.iterator();
            while (i.hasNext()) {
                OtaResource otaResource = i.next();
                if (!canUpdateToFirmware(appInfo, otaResource)) {
                    if (DBG) {
                        Log.d(TAG, "Skipping OTA firmware " + otaResource.getName());
                    }
                    i.remove();
                }
            }
        }
        if (otaResources.size() > 0) {
            mSenseManager.setOtaUpdateMode(true);
            mOtaUiHelper.startUpdate(getApplicationContext(), mSenseManager.getDevice(),
                    mSenseManager.getGattManager(), getFragmentManager(), otaResources, this, true);
        } else {
            mSenseManager.setOtaUpdateMode(false);
            mSenseManager.enableNotifications(true);
        }
    }

    @Override
    public void onSettingsChanged(String settingName) {
        if (Settings.SETTINGS_KEY_TEMPERATURE_SCALE_TYPE.equals(settingName)) {
            //updateTemperatureScaleType();
        }

    }


}
