package com.techint.reader.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.techint.reader.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class PairedAdapter extends ArrayAdapter <JSONObject> {
    List<JSONObject> jsonObjects;
    TextView item;


    private Activity myContext;
    public PairedAdapter(Context context, int resource, List<JSONObject> jsonObjects) {
        super(context, resource,jsonObjects);
        this.jsonObjects=jsonObjects;
        myContext = (Activity)context;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public JSONObject getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = myContext.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.result_item, null);
        item = rowView.findViewById(R.id.item);
        populateData();

        return rowView;
    }

    public void populateData(){
        StringBuilder name_address = new StringBuilder();
        for (int i = 0; i <jsonObjects.size() ; i++) {
            try {
              name_address.append(jsonObjects.get(i).getString("name"));
              name_address.append(" \n");
              name_address.append(jsonObjects.get(i).getString("address"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        item.setText(name_address);

    }
}
