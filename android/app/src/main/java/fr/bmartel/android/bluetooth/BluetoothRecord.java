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

import android.bluetooth.BluetoothDevice;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Bluetooth record object containing
 *
 * <ul>
 *     <li>Bluetooth device object </li>
 *     <li>RSSI value</li>
 *     <li>Scan record : full Advertizing frame</li>
 *     <li>Date</li>
 * </ul>
 */
public class BluetoothRecord {


    private BluetoothDevice device = null;

    private int rssi = 0;

    private byte[] scanRecord = new byte[]{};

    private String date = "";

    /**
     * Build Bluetooth record
     *
     * @param device
     *      Bluetooth device
     * @param rssi
     *      RSSI value
     * @param scanRecord
     *      scan record
     */
    public BluetoothRecord(BluetoothDevice device, int rssi, byte[] scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm:ss");
        this.date=ft.format(dNow);
    }

    public String getDeviceName() {
        if (device != null && device.getName() != null)
            return device.getName();
        return "";
    }

    public String getAddress() {
        if (device != null)
            return device.getAddress();
        return "";
    }

    public int getRssi() {
        return rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public String getDate(){
        return date;
    }
}
