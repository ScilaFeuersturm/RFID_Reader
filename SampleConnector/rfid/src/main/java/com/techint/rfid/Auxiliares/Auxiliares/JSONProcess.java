package com.techint.rfid.Auxiliares.Auxiliares;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static android.content.ContentValues.TAG;

public class JSONProcess {
    private static JSONProcess JSON_PROCESS;
    private List<JSONObject> pairedDevicesList;
    private List<JSONObject> newDevicesList;
    private List<JSONObject> tagList;



    private JSONProcess() {

    }

    public static JSONProcess getSingletonInstance() {

        if (JSON_PROCESS == null){
            JSON_PROCESS = new JSONProcess();
        }
        else{
            Log.e(TAG, "No se puede crear el objeto "+ JSON_PROCESS.toString() + " porque ya existe un objeto de la clase JSONProcess");
        }


        return JSON_PROCESS;
    }



    public JSONObject jsonCreatorPairedDevice(String name, String address) {

        JSONObject JSONdevice = new JSONObject();
        try {
            JSONdevice.put("name", DataHandler.DEVICE_NAME);
            JSONdevice.put("address", DataHandler.ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return JSONdevice;
    }

    public JSONObject jsonCreatorNewDevice(String name, String address) {

        JSONObject JSONdevice = new JSONObject();
        try {
            JSONdevice.put("name", DataHandler.DEVICE_NAME);
            JSONdevice.put("address", DataHandler.ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return JSONdevice;
    }

    public  JSONObject jsonCreatorRFID(String msg, String info, int order) {
        JSONObject jsonRFID = new JSONObject();
        try {
            jsonRFID.put("tidMsg", DataHandler.TID_MESSAGE);
            jsonRFID.put("infoMsg", DataHandler.INFO_MESSAGE);
            jsonRFID.put("numberTag", DataHandler.TAG_SEEN);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonRFID;
    }


    public List<JSONObject> getPairedDevicesList() {
        return pairedDevicesList;
    }

    public List<JSONObject> getNewDevicesList() {
        return newDevicesList;
    }

    public List<JSONObject> getTagList() {
        return tagList;
    }

    public void setPairedDevicesList(List<JSONObject> pairedDevicesList) {
        this.pairedDevicesList = pairedDevicesList;
    }

    public void setNewDevicesList(List<JSONObject> newDevicesList) {
        this.newDevicesList = newDevicesList;
    }

    public void setTagList(List<JSONObject> tagList) {
        this.tagList = tagList;
    }
}
