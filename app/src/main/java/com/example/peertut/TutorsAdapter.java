package com.example.peertut;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TutorsAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private List<Tutor> originalList;
    private List<Tutor> filteredList;
    private TutorFilter filter;

    public TutorsAdapter(Context context, List<Tutor> tutorsList) {
        this.context = context;
        this.originalList = new ArrayList<>(tutorsList);
        this.filteredList = new ArrayList<>(tutorsList);
        this.filter = new TutorFilter();
    }

    public void updateData(List<Tutor> newList) {
        this.originalList = new ArrayList<>(newList);
        this.filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return filteredList.size(); }
    @Override public Tutor getItem(int position) { return filteredList.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.tutor_item, parent, false);
        }

        Tutor tutor = filteredList.get(position);
        TextView nameTextView = convertView.findViewById(R.id.tutorName);
        TextView subjectsTextView = convertView.findViewById(R.id.tutorSubjects);

        nameTextView.setText(tutor.getEmail());

        if (tutor.getSubjects() != null && !tutor.getSubjects().isEmpty()) {
            subjectsTextView.setText(TextUtils.join(", ", tutor.getSubjects()));
            subjectsTextView.setVisibility(View.VISIBLE);
        } else {
            subjectsTextView.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private class TutorFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Tutor> filtered = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(originalList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Tutor tutor : originalList) {
                    if (tutor.getEmail().toLowerCase().contains(filterPattern) ||
                            TextUtils.join(", ", tutor.getSubjects()).toLowerCase().contains(filterPattern)) {
                        filtered.add(tutor);
                    }
                }
            }

            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList.clear();
            filteredList.addAll((List<Tutor>) results.values);
            notifyDataSetChanged();
        }
    }
}