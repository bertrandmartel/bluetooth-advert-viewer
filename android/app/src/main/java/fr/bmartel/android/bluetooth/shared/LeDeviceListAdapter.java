/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Bertrand Martel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fr.bmartel.android.bluetooth.shared;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Bluetooth devices list adapter
 */
public class LeDeviceListAdapter extends BaseAdapter {

    /**
     * list of bluetooth devices
     */
    private ArrayList<BluetoothDevice> mLeDevices;

    /**
     * main activity object
     */
    private ISharedActivity sharedActivity = null;


    /**
     * Build bluetooth device adapter
     *
     * @param sharedActivity
     *      main activity view
     */
    public LeDeviceListAdapter(ISharedActivity sharedActivity) {
        super();
        mLeDevices = new ArrayList<BluetoothDevice>();
        this.sharedActivity=sharedActivity;
    }

    /**
     * Add a bluetooth device to list view
     *
     * @param device
     */
    public void dispatchScanRecord(BluetoothDevice device, int rssi, final byte[] scanRecord) {

        this.sharedActivity.dispatchScanRecord(device,rssi,scanRecord);

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return convertView;
    }

    /**
     * Retrieve Bluetooth Device by position id
     *
     * @param position
     *      position id
     * @return
     *      Bluetooth device objet | null if not found
     */
    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    /**
     * Clear Bluetooth device list
     */
    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

}