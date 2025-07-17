package com.example.peertut;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class UploadQuizActivity extends AppCompatActivity {

    // UI Components
    private EditText quizTitleInput, questionInput, option1, option2, option3, option4, attemptsInput;
    private Spinner subjectSpinner, correctAnswerSpinner;
    private Button pickDateButton, addQuestionButton, uploadQuizButton;
    private TextView selectedDateText, questionsCountText;
    private CheckBox showAnswersCheckbox;

    // Data
    private Date dueDate;
    private List<QuizQuestion> questions = new ArrayList<>();
    private List<String> subjectList = new ArrayList<>();

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference quizzesRef, usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_quiz);

        initViews();
        setupFirebase();
        setupSpinners();
        loadTutorSubjects();
        setListeners();
    }

    private void initViews() {
        quizTitleInput = findViewById(R.id.quizTitleInput);
        subjectSpinner = findViewById(R.id.quizSubjectSpinner);
        pickDateButton = findViewById(R.id.pickDueDateButton);
        selectedDateText = findViewById(R.id.selectedDateText);
        questionInput = findViewById(R.id.quizQuestionInput);
        option1 = findViewById(R.id.option1Input);
        option2 = findViewById(R.id.option2Input);
        option3 = findViewById(R.id.option3Input);
        option4 = findViewById(R.id.option4Input);
        correctAnswerSpinner = findViewById(R.id.correctAnswerSpinner);
        addQuestionButton = findViewById(R.id.addQuestionButton);
        uploadQuizButton = findViewById(R.id.uploadQuizButton);
        questionsCountText = findViewById(R.id.questionsCountText);
        attemptsInput = findViewById(R.id.attemptsInput);
        showAnswersCheckbox = findViewById(R.id.showAnswersCheckbox);
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        quizzesRef = FirebaseDatabase.getInstance().getReference("quizzes");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    private void setupSpinners() {
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, subjectList);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);

        ArrayAdapter<String> answerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Arrays.asList("Option 1", "Option 2", "Option 3", "Option 4"));
        answerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        correctAnswerSpinner.setAdapter(answerAdapter);
    }

    private void loadTutorSubjects() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).child("subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectList.clear();
                for (DataSnapshot s : snapshot.getChildren()) {
                    subjectList.add(s.getValue(String.class));
                }
                ((ArrayAdapter)subjectSpinner.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UploadQuizActivity.this, "Failed to load subjects", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setListeners() {
        pickDateButton.setOnClickListener(v -> showDatePicker());
        addQuestionButton.setOnClickListener(v -> addQuestion());
        uploadQuizButton.setOnClickListener(v -> uploadQuiz());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            c.set(year, month, day);
            dueDate = c.getTime();
            selectedDateText.setText(day + "/" + (month+1) + "/" + year);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addQuestion() {
        String question = questionInput.getText().toString().trim();
        String opt1 = option1.getText().toString().trim();
        String opt2 = option2.getText().toString().trim();
        String opt3 = option3.getText().toString().trim();
        String opt4 = option4.getText().toString().trim();
        int correctIndex = correctAnswerSpinner.getSelectedItemPosition();

        if (TextUtils.isEmpty(question) || TextUtils.isEmpty(opt1) ||
                TextUtils.isEmpty(opt2) || TextUtils.isEmpty(opt3) || TextUtils.isEmpty(opt4)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        questions.add(new QuizQuestion(question, Arrays.asList(opt1, opt2, opt3, opt4), correctIndex));
        clearQuestionFields();
        questionsCountText.setText(String.valueOf(questions.size()));
        Toast.makeText(this, "Question added", Toast.LENGTH_SHORT).show();
    }

    private void clearQuestionFields() {
        questionInput.setText("");
        option1.setText("");
        option2.setText("");
        option3.setText("");
        option4.setText("");
    }

    private void uploadQuiz() {
        String title = quizTitleInput.getText().toString().trim();
        String subject = (String) subjectSpinner.getSelectedItem();
        String attemptsStr = attemptsInput.getText().toString().trim();
        int attempts = attemptsStr.isEmpty() ? 1 : Integer.parseInt(attemptsStr);
        boolean showAnswers = showAnswersCheckbox.isChecked();

        if (TextUtils.isEmpty(title) || subject == null || dueDate == null || questions.isEmpty()) {
            Toast.makeText(this, "Please complete all fields and add at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        String tutorUid = auth.getCurrentUser().getUid();
        String quizId = quizzesRef.push().getKey();

        Quiz quiz = new Quiz(
                quizId, title, tutorUid, subject, dueDate.getTime(),
                new ArrayList<>(questions), attempts, showAnswers
        );

        quizzesRef.child(quizId).setValue(quiz)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Quiz uploaded successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    public static class Quiz implements Parcelable {
        public String quizId, title, createdBy, subject;
        public long dueDate;
        public List<QuizQuestion> questions;
        public int maxAttempts;
        public boolean showAnswers;
        public Map<String, Integer> scores;
        public Map<String, Integer> attempts;

        public Quiz() {
            scores = new HashMap<>();
            attempts = new HashMap<>();
        }

        public Quiz(String quizId, String title, String createdBy, String subject,
                    long dueDate, List<QuizQuestion> questions, int maxAttempts, boolean showAnswers) {
            this();
            this.quizId = quizId;
            this.title = title;
            this.createdBy = createdBy;
            this.subject = subject;
            this.dueDate = dueDate;
            this.questions = questions;
            this.maxAttempts = maxAttempts;
            this.showAnswers = showAnswers;
        }

        protected Quiz(Parcel in) {
            quizId = in.readString();
            title = in.readString();
            createdBy = in.readString();
            subject = in.readString();
            dueDate = in.readLong();
            questions = in.createTypedArrayList(QuizQuestion.CREATOR);
            maxAttempts = in.readInt();
            showAnswers = in.readByte() != 0;
            scores = new HashMap<>();
            attempts = new HashMap<>();
            in.readMap(scores, Integer.class.getClassLoader());
            in.readMap(attempts, Integer.class.getClassLoader());
        }

        public static final Creator<Quiz> CREATOR = new Creator<Quiz>() {
            @Override public Quiz createFromParcel(Parcel in) { return new Quiz(in); }
            @Override public Quiz[] newArray(int size) { return new Quiz[size]; }
        };

        @Override public int describeContents() { return 0; }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(quizId);
            dest.writeString(title);
            dest.writeString(createdBy);
            dest.writeString(subject);
            dest.writeLong(dueDate);
            dest.writeTypedList(questions);
            dest.writeInt(maxAttempts);
            dest.writeByte((byte) (showAnswers ? 1 : 0));
            dest.writeMap(scores);
            dest.writeMap(attempts);
        }
    }

    public static class QuizQuestion implements Parcelable {
        public String question;
        public List<String> options;
        public int correctAnswerIndex;

        public QuizQuestion() {}
        public QuizQuestion(String question, List<String> options, int correctAnswerIndex) {
            this.question = question;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }

        protected QuizQuestion(Parcel in) {
            question = in.readString();
            options = in.createStringArrayList();
            correctAnswerIndex = in.readInt();
        }

        public static final Creator<QuizQuestion> CREATOR = new Creator<QuizQuestion>() {
            @Override public QuizQuestion createFromParcel(Parcel in) { return new QuizQuestion(in); }
            @Override public QuizQuestion[] newArray(int size) { return new QuizQuestion[size]; }
        };

        @Override public int describeContents() { return 0; }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(question);
            dest.writeStringList(options);
            dest.writeInt(correctAnswerIndex);
        }
    }
}