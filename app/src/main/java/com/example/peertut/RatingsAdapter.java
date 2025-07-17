// RatingsAdapter.java
package com.example.peertut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RatingsAdapter extends ArrayAdapter<Rating> {
    public RatingsAdapter(Context context, List<Rating> ratings) {
        super(context, 0, ratings);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Rating rating = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_rating, parent, false);
        }
        
        TextView raterName = convertView.findViewById(R.id.raterName);
        RatingBar ratingBar = convertView.findViewById(R.id.ratingBar);
        TextView commentText = convertView.findViewById(R.id.commentText);
        TextView dateText = convertView.findViewById(R.id.dateText);
        
        // In a real app, you would look up the rater's name from usersRef
        raterName.setText("Student");
        ratingBar.setRating(rating.getRatingValue());
        commentText.setText(rating.getComment());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dateText.setText(sdf.format(new Date(rating.getTimestamp())));
        
        return convertView;
    }
}