package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class ListQuizzesActivity extends AppCompatActivity {
    private Spinner subjectSpinner;
    private ListView quizListView;
    private FirebaseAuth auth;
    private DatabaseReference usersRef, quizRef;
    private List<String> subjects = new ArrayList<>();
    private ArrayAdapter<String> subjAdapter, quizAdapter;
    private List<String> quizTitles = new ArrayList<>();
    private List<String> quizIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle s){ super.onCreate(s); setContentView(R.layout.activity_list_quizzes);
        subjectSpinner = findViewById(R.id.subjectSpinner);
        quizListView   = findViewById(R.id.quizListView);
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        quizRef  = FirebaseDatabase.getInstance().getReference("quizzes");

        subjAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, subjects);
        subjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjAdapter);

        quizAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, quizTitles);
        quizListView.setAdapter(quizAdapter);

        loadTuteeSubjects();
        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?>p, android.view.View v,int pos,long id){
                loadQuizzes(subjects.get(pos));
            }
            public void onNothingSelected(AdapterView<?>p){}
        });

        quizListView.setOnItemClickListener((p,v,pos,id)->{
            Intent i = new Intent(this, TakeQuizActivity.class);
            i.putExtra("subject", subjectSpinner.getSelectedItem().toString());
            i.putExtra("quizId", quizIds.get(pos));
            startActivity(i);
        });
    }

    private void loadTuteeSubjects(){
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).child("subjects")
            .addListenerForSingleValueEvent(new ValueEventListener(){
                public void onDataChange(@NonNull DataSnapshot snap){
                    subjects.clear();
                    for(DataSnapshot s: snap.getChildren()) subjects.add(s.getValue(String.class));
                    subjAdapter.notifyDataSetChanged();
                }
                public void onCancelled(@NonNull DatabaseError e){}
            });
    }

    private void loadQuizzes(String subject){
        quizRef.child(subject).addListenerForSingleValueEvent(new ValueEventListener(){
            public void onDataChange(@NonNull DataSnapshot snap){
                quizTitles.clear(); quizIds.clear();
                for(DataSnapshot q: snap.getChildren()){
                    quizIds.add(q.getKey());
                    quizTitles.add(q.child("title").getValue(String.class));
                }
                quizAdapter.notifyDataSetChanged();
            }
            public void onCancelled(@NonNull DatabaseError e){}
        });
    }
}
