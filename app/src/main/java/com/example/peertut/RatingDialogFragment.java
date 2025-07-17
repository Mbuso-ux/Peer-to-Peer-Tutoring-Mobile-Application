package com.example.peertut;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RatingDialogFragment extends DialogFragment {
    private static final String ARG_RATEE_ID = "ratee_id";
    private static final String ARG_BOOKING_ID = "booking_id";

    public static RatingDialogFragment newInstance(String rateeId, String bookingId) {
        RatingDialogFragment fragment = new RatingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RATEE_ID, rateeId);
        args.putString(ARG_BOOKING_ID, bookingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof TutorHomeActivity)) {
            dismissAllowingStateLoss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_Dialog)
        );
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rate_tutor, null);
        
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText commentEditText = view.findViewById(R.id.commentEdit);

        builder.setView(view)
            .setTitle("Rate Your Student")
            .setPositiveButton("Submit", (dialog, which) -> {
                if (isActivityValid()) {
                    submitRating(ratingBar.getRating(), commentEditText.getText().toString());
                }
            })
            .setNegativeButton("Later", null);

        return builder.create();
    }

    private boolean isActivityValid() {
        return getActivity() != null && 
               !getActivity().isFinishing() && 
               !getActivity().isDestroyed();
    }

    private void submitRating(float rating, String comment) {
        if (!isActivityValid()) return;

        String rateeId = getArguments().getString(ARG_RATEE_ID);
        String bookingId = getArguments().getString(ARG_BOOKING_ID);
        
        DatabaseReference ratingsRef = FirebaseDatabase.getInstance()
            .getReference("ratings");
        String ratingId = ratingsRef.push().getKey();
        
        Rating ratingObj = new Rating(
            bookingId,
            FirebaseAuth.getInstance().getCurrentUser().getUid(),
            rateeId,
            rating,
            comment,
            System.currentTimeMillis()
        );

        ratingsRef.child(ratingId).setValue(ratingObj)
            .addOnSuccessListener(aVoid -> {
                if (isActivityValid()) {
                    Toast.makeText(getContext(), "Rating submitted!", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (isActivityValid()) {
                    Toast.makeText(getContext(), "Failed to submit rating", Toast.LENGTH_SHORT).show();
                }
            });
    }
    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        );

        // Stabilize window focus
        new Handler().postDelayed(() -> {
            if (getDialog() != null) {
                getDialog().getWindow()
                        .getDecorView()
                        .requestFocus();
            }
        }, 100);
    }
}