package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginBtn;
    private MaterialCardView loginCard;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        loginBtn = findViewById(R.id.loginButton);
        loginCard = findViewById(R.id.loginCard);
        progressBar = findViewById(R.id.progressBar);
        TextView goToSignUp = findViewById(R.id.goToSignUpText);

        mAuth = FirebaseAuth.getInstance();

        // Set up click listeners
        loginBtn.setOnClickListener(v -> loginUser());
        goToSignUp.setOnClickListener(v -> navigateToSignUp());

        // Start entry animations
        animateViewsOnStart();
    }

    private void animateViewsOnStart() {
        ImageView logo = findViewById(R.id.logo);
        MaterialButton loginBtn = findViewById(R.id.loginButton);
        TextView signUpText = findViewById(R.id.goToSignUpText);

        // Logo animation
        logo.animate()
                .alpha(1)
                .scaleX(1)
                .scaleY(1)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Card animation
        loginCard.animate()
                .translationY(0)
                .alpha(1)
                .setStartDelay(200)
                .setDuration(600)
                .start();

        // Button animation
        loginBtn.animate()
                .translationY(0)
                .alpha(1)
                .setStartDelay(400)
                .setDuration(600)
                .start();

        // Signup text animation
        signUpText.animate()
                .translationY(0)
                .alpha(1)
                .setStartDelay(600)
                .setDuration(600)
                .start();

        // Button press animation
        loginBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            }
            return false;
        });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showErrorShake();
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            showLoading(false);

            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();

                if (firebaseUser != null && firebaseUser.isEmailVerified()) {
                    fetchUserRoleAndNavigate(firebaseUser.getUid());
                } else {
                    showErrorShake();
                    Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                }

            } else {
                showErrorShake();
                String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserRoleAndNavigate(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User user = task.getResult().getValue(User.class);

                if (user != null) {
                    String role = user.getRole();

                    Intent intent;
                    if ("Tutor".equals(role)) {
                        intent = new Intent(this, TutorHomeActivity.class);
                    } else if ("Tutee".equals(role)) {
                        intent = new Intent(this, TuteeHomeActivity.class);
                    } else {
                        Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();

                } else {
                    showToast("User data is corrupted.");
                }
            } else {
                showToast("Failed to retrieve user profile.");
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginBtn.setAlpha(loading ? 0.5f : 1f);
        loginBtn.setEnabled(!loading);
    }

    private void showErrorShake() {
        loginCard.animate()
                .translationXBy(20f)
                .setInterpolator(new CycleInterpolator(2))
                .setDuration(400)
                .start();
    }

    private void navigateToSignUp() {
        startActivity(new Intent(this, SignupActivity.class));
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
