package com.techint.rfid.Auxiliares;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.techint.rfid.Auxiliares.Auxiliares.DataHandler;
import com.techint.rfid.Auxiliares.Auxiliares.InventoryModel;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.BuildConfig;
import com.uk.tsl.rfid.asciiprotocol.DeviceProperties;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.parameters.AntennaParameters;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Connector {
    public BluetoothAdapter mBtAdapter;
    public ArrayAdapter<String> mPairedDevicesArrayAdapter;
    public ArrayAdapter<String> mNewDevicesArrayAdapter;
    public String deviceName;
    public int mPowerLevel = AntennaParameters.MaximumCarrierPower;
    public static final boolean D = BuildConfig.DEBUG;
    public BluetoothDevice mDevice;
    public InventoryModel mModel = new InventoryModel();
    private Activity context;
    public SessionArrayAdapter mSessionArrayAdapter;
    public static int BT_PERMISSION;
    public static int REQUEST_LOCATION;
    int permissionCheck ;
    public int REQUEST_ENABLE_BT = 1;
    LocationManager lm;
    boolean gps_enabled = false;

    public Connector(@NonNull Context context) {
        this.context = (Activity) context;
    }

    public static AsciiCommander commander;

    public void setCommander(AsciiCommander _commander) {
        commander = _commander;
    }

    public AsciiCommander getCommander() {
        return commander;
    }



    public InventoryCommand mInventoryCommand;

    public void doDiscovery() {

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();


        if (mBtAdapter == null) {
            genericMessage("Bluetooth no disponible en este dispositivo \n Cerrando aplicación ");
            return;

        }else if (!mBtAdapter.isEnabled()) {
            crearDialog();
        }else {
            mBtAdapter.startDiscovery();
        }
    }

    public SeekBar.OnSeekBarChangeListener mPowerSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            updatePowerSetting(getCommander().getDeviceProperties().getMinimumCarrierPower() + seekBar.getProgress());
            mModel.getCommand().setOutputPower(mPowerLevel);
            mModel.updateConfiguration();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            updatePowerSetting(getCommander().getDeviceProperties().getMaximumCarrierPower() / progress);
        }
    };




    public void updateConfiguration() {

        if (getCommander().isConnected()) {
            mInventoryCommand.setTakeNoAction(TriState.YES);
            getCommander().executeCommand(mInventoryCommand);
        }
    }

    public void updatePowerSetting(int level) {
        mPowerLevel = level;
    }


    public void setPowerBarLimits() {
        DeviceProperties deviceProperties = getCommander().getDeviceProperties();
        mPowerLevel = deviceProperties.getMaximumCarrierPower();
    }


    public void UpdateUI() {

        boolean isConnected = getCommander().isConnected();

    }


    public void disconnectDevice() {
        mDevice = null;
        getCommander().disconnect();
    }

    public void reconnectDevice() {
        getCommander().connect(DataHandler.DEVICE);
    }

    public void resetReader() {
        try {
            FactoryDefaultsCommand fdCommand = FactoryDefaultsCommand.synchronousCommand();
            getCommander().executeCommand(fdCommand);
            String msg = "Reset " + (fdCommand.isSuccessful() ? "exitoso" : "fallido");
            Toast.makeText(this.getMyContext(), msg, Toast.LENGTH_SHORT).show();

            UpdateUI();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public class SessionArrayAdapter extends ArrayAdapter<QuerySession> {
        public final QuerySession[] mValues;

        public SessionArrayAdapter(Context context, int textViewResourceId, QuerySession[] objects) {
            super(context, textViewResourceId, objects);
            mValues = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setText(mValues[position].getDescription());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setText(mValues[position].getDescription());
            return view;
        }
    }

    public AdapterView.OnItemSelectedListener mActionSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (mModel.getCommand() != null) {
                QuerySession targetSession = (QuerySession) parent.getItemAtPosition(pos);
                mModel.getCommand().setQuerySession(targetSession);
                mModel.updateConfiguration();
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    public View.OnClickListener mFastIdCheckBoxListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                CheckBox fastIdCheckBox = (CheckBox) v;
                mModel.getCommand().setUsefastId(fastIdCheckBox.isChecked() ? TriState.YES : TriState.NO);
                mModel.updateConfiguration();

                UpdateUI();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (D) {
                Log.d(getClass().getName(), "El estado del AsciiCommander ha cambiado, conectado : " + getCommander().isConnected());
            } else if(!getCommander().isConnected()) {

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getCommander().connect(DataHandler.DEVICE);
                        } catch (Exception e) {
                            Log.e("CATCHED EXCEPTION", e.toString());
                        }
                    }

                });
                    thread.start();
                try {
                    thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


        }
            String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);
            Toast.makeText(context, "Connector status : " + connectionStateMsg, Toast.LENGTH_SHORT).show();




            //displayReaderState();
            if (getCommander().isConnected()) {
                setPowerBarLimits();
                mModel.getCommand().setOutputPower(mPowerLevel);
                mModel.resetDevice();
                mModel.updateConfiguration();
            }


            UpdateUI();
            //displayReaderState();
        }

    };

    public Activity getMyContext() {
        return context;
    }


    private void genericMessage(String message) {
        Toast.makeText(this.getMyContext(), message, Toast.LENGTH_LONG).show();
    }

    


    public final void populateTags(List<JSONObject> jsonObjects){
        for (int i = 0; i <jsonObjects.size() ; i++) {

            mNewDevicesArrayAdapter.add(jsonObjects.get(i).toString());

        }

    }
    private AlertDialog crearDialog()
    {
        AlertDialog visualizarDialog = new AlertDialog.Builder(context)
                .setTitle("Activar bluetooth")
                .setMessage("Es necesario activar bluetooth para poder iniciar la búsqueda.\n" +
                        "¿Desea activar?.\n")

                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mBtAdapter.enable();
                        Toast.makeText(context, "Bluetooth encendido. Puede iniciar búsqueda", Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(context, "Se requiere tener el bluetooth encendido para continuar", Toast.LENGTH_LONG).show();
                    }
                })


                .create();
                visualizarDialog.show();

        return visualizarDialog;
    }

    public void checkPermission(){

        if (ContextCompat.checkSelfPermission(getMyContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat .requestPermissions(this.context,new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1000);
        }
        if (ActivityCompat.checkSelfPermission(getMyContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat .requestPermissions(this.context,new String[]{Manifest.permission.BLUETOOTH}, 1000);
        }
        if (ActivityCompat.checkSelfPermission(getMyContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat .requestPermissions(this.context,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
        if (ContextCompat.checkSelfPermission(getMyContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat .requestPermissions(this.context,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
        else {
            Log.e("Permisos", "Cuenta con permisos");
        }

    }

}


