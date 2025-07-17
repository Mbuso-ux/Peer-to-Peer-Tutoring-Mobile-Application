package com.example.peertut;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    public interface BookingActionListener {
        void onCompleteBooking(Booking booking);
        void onRateTutor(Booking booking);
        void onViewBooking(Booking booking);
    }

    private final List<Booking> bookings;
    private final SimpleDateFormat dateFormat;
    private final BookingActionListener listener;

    public BookingsAdapter(@NonNull List<Booking> bookings,
                           @NonNull SimpleDateFormat dateFormat,
                           @NonNull BookingActionListener listener) {
        this.bookings = bookings != null ? bookings : new ArrayList<>();
        this.dateFormat = dateFormat;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        if (booking != null) {
            holder.bind(booking);
        }
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public void updateBookings(List<Booking> newBookings) {
        if (newBookings != null) {
            bookings.clear();
            // Sort by startTime descending to show latest first
            newBookings.sort((b1, b2) -> Long.compare(b2.getStartTime(), b1.getStartTime()));
            bookings.addAll(newBookings);
            notifyDataSetChanged();
        }
    }

    class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView subjectText, dateText, statusText;
        private final Button completeBtn, rateBtn;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectText = itemView.findViewById(R.id.subjectTextView);
            dateText = itemView.findViewById(R.id.dateTextView);
            statusText = itemView.findViewById(R.id.statusTextView);
            completeBtn = itemView.findViewById(R.id.completeButton);
            rateBtn = itemView.findViewById(R.id.rateButton);
        }

        public void bind(@NonNull Booking booking) {
            subjectText.setText(booking.getSubject() != null ? booking.getSubject() : "N/A");

            try {
                dateText.setText(dateFormat.format(new Date(booking.getStartTime())));
            } catch (IllegalArgumentException e) {
                dateText.setText("Invalid date");
            }

            statusText.setText(booking.getStatus() != null ?
                    booking.getStatus().toUpperCase(Locale.getDefault()) : "N/A");

            // Show complete button only if booking is active
            boolean showCompleteBtn = "active".equalsIgnoreCase(booking.getStatus());
            completeBtn.setVisibility(showCompleteBtn ? View.VISIBLE : View.GONE);

            // Hide rate button for tutors
            rateBtn.setVisibility(View.GONE);

            completeBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompleteBooking(booking);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewBooking(booking);
                }
            });

            String desc = String.format(Locale.getDefault(),
                    "Booking for %s at %s, Status: %s",
                    booking.getSubject() != null ? booking.getSubject() : "Unknown subject",
                    dateFormat.format(new Date(booking.getStartTime())),
                    booking.getStatus() != null ? booking.getStatus() : "Unknown status");
            itemView.setContentDescription(desc);
        }
    }
}
