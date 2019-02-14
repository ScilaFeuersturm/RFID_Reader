package com.techint.reader;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.techint.rfid.Auxiliares.Auxiliares.DataHandler;
import com.techint.rfid.Auxiliares.Auxiliares.JSONProcess;
import com.techint.rfid.Auxiliares.Auxiliares.ModelBase;
import com.techint.rfid.Auxiliares.Auxiliares.WeakHandler;
import com.techint.rfid.Auxiliares.Connector;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;

import com.uk.tsl.rfid.asciiprotocol.enumerations.QuerySession;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.techint.rfid.Auxiliares.Auxiliares.InventoryModel;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {
    Connector c = new Connector(this);
    public ArrayAdapter<String> mResultsArrayAdapter;
    public ListView mResultsListView;
    Button reconectar;
    Button conectar;
    Button desconectar;
    Button reset;
    public TextView mPowerLevelTextView;
    public SeekBar mPowerSeekBar;
    public TextView mResultTextView;
    List<JSONObject> tagList = new ArrayList<JSONObject>();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        mResultsArrayAdapter = new ArrayAdapter<String>(this, R.layout.result_item);
        mResultTextView = (TextView) findViewById(R.id.resultTextView);

        mResultsListView = (ListView) findViewById(R.id.resultListView);
        mResultsListView.setAdapter(mResultsArrayAdapter);
        mResultsListView.setFastScrollEnabled(true);
        conectar = findViewById(R.id.conectar);
        reconectar = findViewById(R.id.reconectar);
        desconectar = findViewById(R.id.desconectar);
        reset = findViewById(R.id.reset);

        Button sButton = (Button) findViewById(R.id.scanButton);
        sButton.setOnClickListener(mScanButtonListener);
        Button cButton = (Button) findViewById(R.id.clearButton);

        cButton.setOnClickListener(mClearButtonListener);

        mPowerLevelTextView = (TextView) findViewById(R.id.powerTextView);
        mPowerSeekBar = (SeekBar) findViewById(R.id.powerSeekBar);
        mPowerSeekBar.setOnSeekBarChangeListener(c.mPowerSeekBarListener);
        c.setPowerBarLimits();
        mSessionArrayAdapter = new SessionArrayAdapter(this, android.R.layout.simple_spinner_item, mSessions);
        // Find and set up the sessions spinner
        Spinner spinner = (Spinner) findViewById(R.id.sessionSpinner);
        mSessionArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(c.mSessionArrayAdapter);
        spinner.setOnItemSelectedListener(c.mActionSelectedListener);
        spinner.setSelection(0);

        CheckBox cb = (CheckBox) findViewById(R.id.fastIdCheckBox);
        cb.setOnClickListener(c.mFastIdCheckBoxListener);


        AsciiCommander commander = c.getCommander();


        commander.addResponder(new LoggerResponder());


        commander.addSynchronousResponder();
        mModel = new InventoryModel();
        c.mModel.setCommander(c.getCommander());
        c.mModel.setHandler(mGenericModelHandler);


        reconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                c.reconnectDevice();
                c.UpdateUI();


            }
        });
        desconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(InventoryActivity.this, "Volviendo a emparejamiento", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);



            }
        });
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                c.resetReader();
                c.UpdateUI();
                Log.e("ESTADO", c.getCommander().getConnectionState().toString());

            }
        });

    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        c.mModel.setEnabled(true);
        LocalBroadcastManager.getInstance(this).registerReceiver(c.mCommanderMessageReceiver,
                new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));
        c.UpdateUI();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        c.mModel.setEnabled(false);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(c.mCommanderMessageReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //TODO verificar comportamiento
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public final WeakHandler<InventoryActivity> mGenericModelHandler = new WeakHandler<InventoryActivity>(this) {

        @Override
        public void handleMessage(Message msg, InventoryActivity thisActivity) {
            try {
                switch (msg.what) {
                    case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
                        break;

                    case ModelBase.MESSAGE_NOTIFICATION:
                        String message = (String) msg.obj; //todo aca pasar el JSON
                        JSONObject jsonObjectTag = JSONProcess.getSingletonInstance().jsonCreatorRFID(DataHandler.TID_MESSAGE,DataHandler.INFO_MESSAGE, DataHandler.TAG_SEEN );
                        JSONProcess.getSingletonInstance().setNewDevicesList(tagList);
                        tagList.add(jsonObjectTag);



                        if (message.startsWith("ER:")) {
                            mResultTextView.setText(message.substring(3));
                        } else {


                            populateNewDevices(tagList);
                            scrollResultsListViewToBottom();
                        }
                        c.UpdateUI();
                        break;

                    default:
                        break;
                }

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error no permite continuar", Toast.LENGTH_LONG).show();
                finish();
            }

        }


    };


    public void scrollResultsListViewToBottom() {
        mResultsListView.post(new Runnable() {
            @Override
            public void run() {
                mResultsListView.setSelection(mResultsArrayAdapter.getCount() - 1);
            }
        });
    }

    public View.OnClickListener mScanButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                mModel.scan();
                c.UpdateUI();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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

    public QuerySession[] mSessions = new QuerySession[]{
            QuerySession.SESSION_0,
            QuerySession.SESSION_1,
            QuerySession.SESSION_2,
            QuerySession.SESSION_3
    };
    public SessionArrayAdapter mSessionArrayAdapter;

    public InventoryModel mModel;
    public View.OnClickListener mClearButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                mResultsArrayAdapter.clear();
                c.UpdateUI();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    public final void populateNewDevices(List<JSONObject> jsonObjects){
        for (int i = 0; i <jsonObjects.size() ; i++) {

            mResultsArrayAdapter.add(jsonObjects.get(i).toString());

        }

    }
}
