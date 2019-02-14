package com.techint.reader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.techint.rfid.Auxiliares.Connector;
import com.techint.rfid.Auxiliares.Auxiliares.DataHandler;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.BuildConfig;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = "TSLBTDeviceActivity";
    private static final boolean D = BuildConfig.DEBUG;
    Connector c = new Connector(this);
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = DataHandler.DEVICE;


    protected AsciiCommander getCommander() {
        return c.getCommander();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            genericMessage("Bluetooth no disponible en este dispositivo \n Cerrando aplicación ");
            return;
        } else {

            if (getCommander() == null) {
                try {
                    AsciiCommander commander = new AsciiCommander(getApplicationContext());
                    c.setCommander(commander);

                } catch (Exception e) {
                    genericMessage("No fue posible crear AsciiCommander!");
                }
            }
        }
    }

    private void genericMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                finish();
            }
        }, 1300);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDevice == null) {
            Toast.makeText(this, "No es posible conectarse con el dispositivo, volviendo ... ", Toast.LENGTH_SHORT).show();
            getCommander().connect(null);

        } else {
            connectToDevice();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        getCommander().disconnect();
        mDevice = null;

    }


    private void connectToDevice() {
        Toast.makeText(this.getApplicationContext(), "Conectando ...", Toast.LENGTH_LONG).show();
        mDevice = mBluetoothAdapter.getRemoteDevice(DataHandler.ADDRESS);
        if (mDevice != null) {
            getCommander().connect(mDevice);
            Intent intent = new Intent(this, InventoryActivity.class);
            startActivity(intent);

        } else {
            if (D) Log.e(TAG, "No es posible obtener dispositivo Bluetooth");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (D)
            Log.d(TAG, "selectDevice() onActivityResult: " + resultCode + " for request: " + requestCode);

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectToDevice();
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectToDevice();
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    Log.d(TAG, "El Bluetooth no esta activo");
                    genericMessage("Bluetooth no esta encendido \nSaliendo de la aplicación...");
                }
        }
    }




}
