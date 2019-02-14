package com.techint.reader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import com.techint.reader.Adapters.PairedAdapter;
import com.techint.reader.Adapters.PairedAdapter;
import com.techint.rfid.Auxiliares.Auxiliares.DataHandler;
import com.techint.rfid.Auxiliares.Auxiliares.JSONProcess;
import com.techint.rfid.Auxiliares.Connector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    Connector c = new Connector(this);
    public static final String TAG = "DeviceListActivity";
    public static final boolean D = true;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    public ArrayAdapter<String> mPairedDevicesArrayAdapter;
    public ArrayAdapter<String> mNewDevicesArrayAdapter;
    public PairedAdapter pairedAdapter;
    List<JSONObject> pairedList = new ArrayList<JSONObject>();
    List<JSONObject> newList = new ArrayList<JSONObject>();


    public Button mScanButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        c.checkPermission();
        mScanButton = (Button) findViewById(R.id.button_scan);
        listPairedDevices();
        populatepairedDevices(pairedList);

        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);





        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        populateNewDevices(newList);



        mScanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.setEnabled(true);
                c.doDiscovery();





            }
        });
    }

    public void listPairedDevices() {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        if (mBtAdapter != null) {
            try {
                pairedDevices = mBtAdapter.getBondedDevices();
            } catch (Exception e) {
                Log.e("ERROR", e.toString());
            }
        }
        if (pairedDevices != null) {
            try {
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        DataHandler.ADDRESS = device.getAddress();
                        DataHandler.DEVICE_NAME = device.getName();
                        //CREA EL OBJETO JSON Y LO LLENA A MEDIDA QUE SE VAN ENCONTRANDO DISPOSITIVOS PAREADOS
                        JSONObject jsonObjectPaired = JSONProcess.getSingletonInstance().jsonCreatorPairedDevice(DataHandler.DEVICE_NAME,DataHandler.ADDRESS);
                        JSONProcess.getSingletonInstance().setPairedDevicesList(pairedList);
                        pairedList.add(jsonObjectPaired);


                    }
                } else {
                    pairedAdapter.clear();
                    c.doDiscovery();
                }
            } catch (Exception e) {
                Log.e("Error", e.toString());
            }
        } else {
            c.doDiscovery();
        }
    }

    public AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info;
            JSONObject jsonObject= null;
            String address ="";
            info = ((TextView) v).getText().toString();
            if (info.length() >= 17) {
                try {
                    jsonObject = new JSONObject(info);
                    address = jsonObject.getString("address");
                    DataHandler.ADDRESS = address;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                boolean hasCorrectColons = true;
                for (int i = 0; i < 5; ++i) {
                    if (address.charAt(2 + 3 * i) != ':') {
                        hasCorrectColons = false;
                        Toast.makeText(getApplicationContext(),"Bluetooth inválido",Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                if (hasCorrectColons) {
                    //mBtAdapter.cancelDiscovery();
                    DataHandler.DEVICE = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(DataHandler.ADDRESS);
                    Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                    startActivityForResult(intent, Activity.RESULT_OK);
                    Toast.makeText(getApplicationContext(), "Iniciando conexión", Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            TextView otherDevicesHeader = (TextView) findViewById(R.id.title_new_devices);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String listText = device.getName() + "\n" + device.getAddress();
                    boolean isNew = true;
                    for (int i = 0; i < mNewDevicesArrayAdapter.getCount(); ++i) {
                        if (mNewDevicesArrayAdapter.getItem(i).equals(listText)) {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {

                        DataHandler.DEVICE_NAME = device.getName();
                        DataHandler.ADDRESS = device.getAddress();
                        JSONObject jsonObjectNew = JSONProcess.getSingletonInstance().jsonCreatorNewDevice(DataHandler.DEVICE_NAME,DataHandler.ADDRESS);
                        JSONProcess.getSingletonInstance().setNewDevicesList(newList);
                        newList.add(jsonObjectNew);
                        populateNewDevices(newList);
                    }

                    String deviceCountFormat = getResources().getText(R.string.title_other_devices_count_format).toString();
                    String someDevices = String.format(deviceCountFormat, mNewDevicesArrayAdapter.getCount());
                    otherDevicesHeader.setText(someDevices);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                Button scanButton = findViewById(R.id.button_scan);
                scanButton.setEnabled(true);
            }
        }


    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    public void populatepairedDevices(List<JSONObject> jsonObjects){
        for (int i = 0; i <jsonObjects.size() ; i++) {

                mPairedDevicesArrayAdapter.add(jsonObjects.get(i).toString());

        }
    }
    public final void populateNewDevices(List<JSONObject> jsonObjects){
        for (int i = 0; i <jsonObjects.size() ; i++) {

            mNewDevicesArrayAdapter.add(jsonObjects.get(i).toString());

        }

    }
}


