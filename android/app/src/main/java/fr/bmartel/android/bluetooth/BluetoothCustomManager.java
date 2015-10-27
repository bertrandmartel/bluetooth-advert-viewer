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
package fr.bmartel.android.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.bmartel.android.bluetooth.shared.ISharedActivity;
import fr.bmartel.android.bluetooth.shared.LeDeviceListAdapter;
import fr.bmartel.android.utils.ManualResetEvent;


/**
 * Bluetooth android API processing : contains all android bluetooth api
 * <p/>
 * alternative to this is using an Android Service that you can bind to your main activity
 *
 * @author Bertrand Martel
 */
public class BluetoothCustomManager implements IBluetoothCustomManager {

    private final static String TAG = BluetoothCustomManager.class.getName();

    // set init pool size
    private static final int CORE_POOL_SIZE = 1;

    // set max pool size
    private static final int MAXIMUM_POOL_SIZE = 1;

    // Sets the amount of time an idle thread will wait for a task before terminating
    private static final int KEEP_ALIVE_TIME = 5;

    // set time unit in seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;

    static {

        // The time unit for "keep alive" is in seconds
        KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    }

    LinkedBlockingQueue gattWorkingQueue = new LinkedBlockingQueue<Runnable>();

    /*
     * Creates a new pool of Thread objects for the download work queue
     */
    ThreadPoolExecutor gattThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, gattWorkingQueue);

    /**
     * timeout for waiting for response frame from the device
     */
    private final static int BT_TIMEOUT = 2000;

    /**
     * set bluetooth scan period
     */
    private final int SCAN_PERIOD = 30000;

    /**
     * event manager used to block / release process
     */
    private ManualResetEvent eventManager = new ManualResetEvent(false);

    /**
     * Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * list of event listener for this bluetooth manager
     */
    private ArrayList<IBluetoothManagerEventListener> bluetoothManagerEventListenersList = new ArrayList<>();

    /**
     * List of bluetooth device manager
     */
    private LeDeviceListAdapter mLeDeviceListAdapter = null;

    /**
     * message handler
     */
    private Handler mHandler = null;

    /**
     * set bluetooth scan
     */
    private volatile boolean scanning = false;

    /**
     * Callback for Bluetooth adapter
     * This will be called when a bluetooth device has been discovered
     */
    private BluetoothAdapter.LeScanCallback scanCallback = null;

    private Context context = null;

    /**
     * Build bluetooth manager
     */
    public BluetoothCustomManager(Context context) {
        this.context = context;
    }


    @SuppressLint("NewApi")
    public void init(final ISharedActivity sharedActivity) {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) sharedActivity.getContext().getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            for (int i = 0; i < bluetoothManagerEventListenersList.size(); i++) {
                bluetoothManagerEventListenersList.get(i).onBluetoothAdapterNotEnabled();
            }
        }

        //init message handler
        mHandler = null;
        mHandler = new Handler();

        scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, final byte[] scanRecord) {
                mLeDeviceListAdapter.dispatchScanRecord(device, rssi, scanRecord);
                //sharedActivity.addDeviceToList(device);
            }
        };
    }

    /**
     * add an event listener to list
     *
     * @param listener event listener
     */
    public void addEventListener(IBluetoothManagerEventListener listener) {
        bluetoothManagerEventListenersList.add(listener);
    }

    /**
     * initialize list adapter called when one bluetooth device is detected on Bluetooth scan
     *
     * @param activity
     */
    public void initListAdapter(ISharedActivity activity) {

        /*initialize bluetooth scanning objects*/
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
        } else {
            mLeDeviceListAdapter = new LeDeviceListAdapter(activity);
        }
    }

    /**
     * clear list adapter (usually before rescanning)
     */
    public void clearListAdapter() {
        //clear custom list adapter
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    /**
     * Scan new Bluetooth device
     *
     * @param enable true if bluetooth start scanning / stop scanning if false
     */
    @SuppressLint("NewApi")
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            //notify start of scan
            for (int i = 0; i < bluetoothManagerEventListenersList.size(); i++) {
                bluetoothManagerEventListenersList.get(i).onStartOfScan();
            }

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (scanning) {
                                //notify end of scan
                                for (int i = 0; i < bluetoothManagerEventListenersList.size(); i++) {
                                    bluetoothManagerEventListenersList.get(i).onEndOfScan();
                                }
                                scanning = false;
                                mBluetoothAdapter.stopLeScan(scanCallback);
                            }
                        }
                    }, SCAN_PERIOD);

            scanning = true;
            mBluetoothAdapter.startLeScan(scanCallback);
        } else {
            scanning = false;
            mBluetoothAdapter.stopLeScan(scanCallback);
        }
    }

    /**
     * Stop Bluetooth LE scanning
     */
    @SuppressLint("NewApi")
    public void stopScan() {
        mHandler.removeCallbacksAndMessages(null);
        scanning = false;
        mBluetoothAdapter.stopLeScan(scanCallback);

    }

    public boolean isScanning() {
        return scanning;
    }

    @Override
    public ManualResetEvent getEventManager() {
        return eventManager;
    }

    /**
     * Send broadcast data through broadcast receiver
     *
     * @param action action to be sent
     */
    @Override
    public void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    /**
     * broadcast characteristic value
     *
     * @param action action to be sent (data available)
     */
    @Override
    public void broadcastUpdateStringList(String action, ArrayList<String> valueList) {

        String valueName = "";
        final Intent intent = new Intent(action);
        intent.putStringArrayListExtra(valueName, valueList);
        context.sendBroadcast(intent);
    }

}
