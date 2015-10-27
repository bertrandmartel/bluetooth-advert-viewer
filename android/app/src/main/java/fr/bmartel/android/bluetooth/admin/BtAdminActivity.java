/**
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2015 Bertrand Martel
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.bmartel.android.bluetooth.admin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fr.bmartel.android.bluetooth.BluetoothRecord;
import fr.bmartel.android.bluetooth.BtAdminService;
import fr.bmartel.android.bluetooth.IBluetoothManagerEventListener;
import fr.bmartel.android.bluetooth.IScanListListener;
import fr.bmartel.android.bluetooth.shared.StableArrayAdapter;

/**
 * Bluetooth advertizing frame admin main activity
 *
 * @author Bertrand Martel
 */
public class BtAdminActivity extends Activity {

    /**
     * debug tag
     */
    private String TAG = this.getClass().getName();

    private String deviceAddress = "";

    private String filter = "";

    private ProgressDialog dialog = null;

    private boolean toSecondLevel = false;

    private boolean bound = false;

    private Button button_stop_scanning = null;

    private Button button_start_scanning = null;

    public static final String preferences = "btAdminPref" ;

    /**
     * define if bluetooth is enabled on device
     */
    private final static int REQUEST_ENABLE_BT = 1;

    /**
     * Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * list of device to display
     */
    private ListView device_list_view = null;

    /**
     * current index of connecting device item in device list
     */
    private int list_item_position = 0;

    private BtAdminService currentService = null;

    private SharedPreferences sharedpreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_frame);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Smart is not supported on your device", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        final ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);

        sharedpreferences = getSharedPreferences(preferences, Context.MODE_PRIVATE);
        filter = sharedpreferences.getString("filterName", "");

        button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);
        button_start_scanning = (Button) findViewById(R.id.scanning_button);

        final EditText filter_scanning_editext = (EditText) findViewById(R.id.filter_scanning_editext);

        filter_scanning_editext.setText(filter);

        filter_scanning_editext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!s.toString().equals("")) {
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString("filterName", filter);
                            editor.commit();
                            currentService.getListViewAdapter().setFilter(s.toString().toLowerCase());
                            currentService.getListViewAdapter().notifyDataSetChanged();
                        }
                    }
                });
            }
        });

        if (button_stop_scanning != null)
            button_stop_scanning.setEnabled(false);

        final TextView scanText = (TextView) findViewById(R.id.scanText);

        if (scanText != null)
            scanText.setText("");

        button_stop_scanning.setEnabled(false);

        final Button button_find_accessory = (Button) findViewById(R.id.scanning_button);

        button_stop_scanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentService != null && currentService.isScanning()) {

                    button_start_scanning.setEnabled(true);
                    button_start_scanning.requestFocus();

                    currentService.stopScan();

                    if (progress_bar != null) {
                        progress_bar.setEnabled(false);
                        progress_bar.setVisibility(View.GONE);
                    }

                    if (scanText != null)
                        scanText.setText("");

                    if (button_stop_scanning != null)
                        button_stop_scanning.setEnabled(false);
                }
            }
        });

        button_find_accessory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                triggerNewScan();
            }
        });

        if (mBluetoothAdapter.isEnabled()) {

            Intent intent = new Intent(this, BtAdminService.class);

            // bind the service to current activity and create it if it didnt exist before
            startService(intent);
            bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {

            if (mBluetoothAdapter.isEnabled()) {


                Intent intent = new Intent(this, BtAdminService.class);

                // bind the service to current activity and create it if it didnt exist before
                startService(intent);
                bound = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

            } else {

                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show();

            }

        }
    }


    /**
     * trigger a BLE scan
     */
    public void triggerNewScan() {

        ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);
        TextView scanText = (TextView) findViewById(R.id.scanText);

        if (button_stop_scanning != null && progress_bar != null && scanText != null) {
            if (currentService != null && !currentService.isScanning()) {

                Toast.makeText(BtAdminActivity.this, "Looking for new accessories", Toast.LENGTH_SHORT).show();

                if (progress_bar != null) {
                    progress_bar.setEnabled(true);
                    progress_bar.setVisibility(View.VISIBLE);
                }

                if (scanText != null)
                    scanText.setText("Scanning ...");

                //start scan so clear list view
                currentService.getListViewAdapter().clear();
                currentService.getListViewAdapter().notifyDataSetChanged();
                currentService.clearListAdapter();

                button_start_scanning.setEnabled(false);
                button_stop_scanning.setEnabled(true);
                button_stop_scanning.requestFocus();

                currentService.scanLeDevice(true);

            } else {
                Toast.makeText(BtAdminActivity.this, "Scanning already engaged...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //currentService.disconnect(deviceAddress);
    }

    @Override
    public void onResume() {
        super.onResume();
        toSecondLevel = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!toSecondLevel) {

            if (device_list_view != null) {
                device_list_view.setAdapter(null);
            }

            if (currentService != null) {

                currentService.getListViewAdapter().clear();
                currentService.getListViewAdapter().notifyDataSetChanged();
                currentService.clearListAdapter();
            }
        }

        if (dialog != null) {
            dialog.cancel();
            dialog = null;
        }

        if (currentService != null) {
            currentService.removeScanListeners();
            if (currentService.isScanning())
                currentService.stopScan();
        }

        try {
            if (bound) {
                // unregister receiver or you will have strong exception
                unbindService(mServiceConnection);
                bound = false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Manage Bluetooth Service lifecycle
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.i(TAG, "Connected to service");

            currentService = ((BtAdminService.LocalBinder) service).getService();

            currentService.addScanListListener(new IScanListListener() {
                @Override
                public void onItemAddedInList(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device != null && device.getName() != null && (device.getName().toLowerCase().contains(filter) || filter.equals(""))) {

                                currentService.getListViewAdapter().replaceElement(0, new BluetoothRecord(device, rssi, scanRecord));
                                currentService.getListViewAdapter().notifyDataSetChanged();
                            }
                        }
                    });
                }

                @Override
                public void onNotifyChangeInList() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentService.getListViewAdapter().notifyDataSetChanged();
                        }
                    });
                }
            });
            currentService.addEventListener(new IBluetoothManagerEventListener() {

                @Override
                public void onBluetoothAdapterNotEnabled() {
                    //beware of Android SDK used on this Android device
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                @Override
                public void onEndOfScan() {

                    final Button button_stop_scanning = (Button) findViewById(R.id.stop_scanning_button);

                    final ProgressBar progress_bar = (ProgressBar) findViewById(R.id.scanningProgress);
                    final TextView scanText = (TextView) findViewById(R.id.scanText);

                    button_start_scanning.setEnabled(false);

                    Toast.makeText(BtAdminActivity.this, "End of scanning...", Toast.LENGTH_SHORT).show();

                    button_stop_scanning.setEnabled(false);
                    button_start_scanning.setEnabled(true);
                    button_start_scanning.requestFocus();

                    if (progress_bar != null) {
                        progress_bar.setVisibility(View.GONE);
                    }

                    if (scanText != null)
                        scanText.setText("");

                }

                @Override
                public void onStartOfScan() {

                }

            });

            device_list_view = (ListView) findViewById(R.id.listView);

            final ArrayList<BluetoothRecord> list = new ArrayList<>();

            currentService.setListViewAdapter(new StableArrayAdapter(BtAdminActivity.this, list));

            device_list_view.setAdapter(currentService.getListViewAdapter());

            device_list_view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // selected item
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            triggerNewScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    };

}