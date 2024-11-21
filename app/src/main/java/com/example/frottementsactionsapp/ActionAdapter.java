package com.example.frottementsactionsapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ActionAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<ActionItem> actions;

    public ActionAdapter(Context context, ArrayList<ActionItem> actions) {
        this.context = context;
        this.actions = actions;
    }

    @Override
    public int getCount() {
        return actions.size();
    }

    @Override
    public Object getItem(int position) {
        return actions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        }

        TextView title = convertView.findViewById(R.id.tv_action_title);
        TextView subtitle = convertView.findViewById(R.id.tv_action_subtitle);

        ActionItem action = actions.get(position);
        title.setText(action.getTitle());
        subtitle.setText("Nombre de frottements : " + action.getFrottements());

        return convertView;
    }
}
