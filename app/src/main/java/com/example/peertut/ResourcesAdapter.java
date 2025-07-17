package com.example.peertut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ResourcesAdapter extends ArrayAdapter<Resource> {

    private final LayoutInflater inflater;

    public ResourcesAdapter(Context context, List<Resource> resources) {
        super(context, 0, resources);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate row if needed
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_resource, parent, false);
        }

        Resource resource = getItem(position);

        TextView titleView = convertView.findViewById(R.id.resourceTitle);
        TextView descView  = convertView.findViewById(R.id.resourceDescription);

        if (resource != null) {
            titleView.setText(resource.getTitle());
            descView.setText(resource.getDescription());
        }

        return convertView;
    }
}
