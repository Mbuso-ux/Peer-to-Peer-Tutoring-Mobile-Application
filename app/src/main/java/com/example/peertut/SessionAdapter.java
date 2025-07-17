package com.example.peertut.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.BaseAdapter;

import com.example.peertut.R;
import com.example.peertut.Session;

import java.util.ArrayList;

public class SessionAdapter extends BaseAdapter {

    private final Activity context;
    private final ArrayList<Session> sessions;

    public SessionAdapter(Activity context, ArrayList<Session> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    @Override
    public int getCount() {
        return sessions.size();
    }

    @Override
    public Object getItem(int position) {
        return sessions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int i, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        view = inflater.inflate(R.layout.session_item, null);

        TextView subject = view.findViewById(R.id.sessionSubject);
        TextView dateTime = view.findViewById(R.id.sessionDateTime);
        TextView person = view.findViewById(R.id.sessionPerson);

        Session session = sessions.get(i);
        subject.setText("Subject: " + session.getSubject());
        dateTime.setText("Date: " + session.getDate() + " | Time: " + session.getTime());
        person.setText("With: " + session.getTutorName()); // or change this dynamically

        return view;
    }
}
