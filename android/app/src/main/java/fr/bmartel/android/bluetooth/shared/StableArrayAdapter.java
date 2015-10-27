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
package fr.bmartel.android.bluetooth.shared;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import fr.bmartel.android.bluetooth.BluetoothRecord;
import fr.bmartel.android.bluetooth.ScanRecordDecoder;
import fr.bmartel.android.bluetooth.admin.R;
import fr.bmartel.android.utils.ByteUtils;

/**
 * Adapter for new device list
 *
 * @author Bertrand Martel
 */
public class StableArrayAdapter extends BaseAdapter {

    static class ViewHolder {
        HorizontalScrollView mainLayout;
        TextView device_name;
        TextView device_address;
        TextView device_rssi;
        ;
        TextView device_scanRecord;
    }

    /**
     * list view item hash map
     */
    private ArrayList<BluetoothRecord> recordList = new ArrayList<>();

    private ArrayList<BluetoothRecord> filteredRecordList = new ArrayList<>();

    private String filterName = "";

    /**
     * Build adapter for list view items
     *
     * @param context Android context
     * @param objects objects to be put in list view
     */
    public StableArrayAdapter(Context context,
                              ArrayList<BluetoothRecord> objects) {
        recordList = objects;
        for (int i = 0; i < recordList.size(); i++) {
            if (recordList.get(i).getDeviceName().toLowerCase().contains(filterName) || filterName.equals("")) {
                filteredRecordList.add(recordList.get(i));
            }
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return filteredRecordList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredRecordList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Set specific view items in list
     *
     * @param position    item selected position
     * @param convertView view to be displayed
     * @param parent      parent view
     * @return new view to be displayed
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.new_device_layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mainLayout = (HorizontalScrollView) convertView.findViewById(R.id.mainLayout);
            viewHolder.device_name = (TextView) convertView.findViewById(R.id.device_name);
            viewHolder.device_address = (TextView) convertView.findViewById(R.id.device_address);
            viewHolder.device_rssi = (TextView) convertView.findViewById(R.id.device_rssi);
            viewHolder.device_scanRecord = (TextView) convertView.findViewById(R.id.device_record);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (filteredRecordList.size() > 0) {

            /*display bluetooth device name in list view*/
            viewHolder.device_name.setText(filteredRecordList.get(position).getDate() + " | " + filteredRecordList.get(position).getDeviceName());
            viewHolder.device_address.setText(filteredRecordList.get(position).getAddress());
            viewHolder.device_rssi.setText(filteredRecordList.get(position).getRssi() + "dB");

            ScanRecordDecoder decoder = new ScanRecordDecoder();
            decoder.parseScanRecord(filteredRecordList.get(position).getScanRecord());

            SpannableString txtToSpan = new SpannableString(ByteUtils.byteArrayToStringMessage("", filteredRecordList.get(position).getScanRecord(), ' '));

            if (decoder.getManufacturerFrame() != null && decoder.getManufacturerFrame().getFullFrame() != null) {

                int indexStart = 0;

                if (decoder.getManufacturerFramePos() > 0) {
                    indexStart = decoder.getManufacturerFramePos() * 2 + (decoder.getManufacturerFramePos() - 1) + 1;
                }

                int indexEnd = indexStart + (decoder.getManufacturerFrame().getFullFrame().length + 1) * 3 + 2;

                txtToSpan.setSpan(new BackgroundColorSpan(Color.GREEN), indexStart, indexEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                viewHolder.device_scanRecord.setText(txtToSpan);
            } else {
                viewHolder.device_scanRecord.setText(ByteUtils.byteArrayToStringMessage("", filteredRecordList.get(position).getScanRecord(), ' '));
            }
        }
        return convertView;
    }

    public void replaceElement(int i, BluetoothRecord bluetoothRecord) {
        recordList.add(i, bluetoothRecord);
        if (bluetoothRecord.getDeviceName().toLowerCase().contains(filterName) || filterName.equals("")) {
            filteredRecordList.add(i, bluetoothRecord);
        }
        if (recordList.size() > 50) {
            recordList.subList(49, recordList.size()).clear();
        }
        if (filteredRecordList.size() > 50) {
            filteredRecordList.subList(49, filteredRecordList.size()).clear();
        }
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilter(String filter) {
        this.filterName = filter;
        filteredRecordList.clear();
        for (int i = 0; i < recordList.size(); i++) {
            if (recordList.get(i).getDeviceName().toLowerCase().contains(filterName) || filterName.equals("")) {
                filteredRecordList.add(recordList.get(i));
            }
        }
    }

    public void clear() {
        recordList.clear();
        filteredRecordList.clear();
    }
}