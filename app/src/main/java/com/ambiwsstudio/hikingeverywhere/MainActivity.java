package com.ambiwsstudio.hikingeverywhere;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference database;
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent currentIntent = getIntent();

        if (currentIntent.getStringExtra("signOut") != null) {

            FirebaseAuth.getInstance().signOut();

        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        emailEditText.requestFocus();

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            logIn();

        }

    }

    public void goClicked(View view) {

        final String email = emailEditText.getText().toString();
        final String pass = passwordEditText.getText().toString();

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            logIn();

                        } else {

                            mAuth.createUserWithEmailAndPassword(email, pass)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {

                                            if (task.isSuccessful()
                                                    && task.getResult() != null
                                                    && task.getResult().getUser() != null) {

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("Email").setValue(email);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("Picture").setValue("https://firebasestorage.googleapis.com/v0/b/" +
                                                        "hiking-everywhere.appspot.com/o/default-profile-picture.png" +
                                                        "?alt=media&token=5b6cbcaa-2304-42ad-8e12-baa7a688c8f1");

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("ChallengesAccepted").setValue(0);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("ChallengesDone").setValue(0);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("ChallengesFailed").setValue(0);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("HighestRank").setValue("TBD");

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("OverallDistance").setValue(0.0);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("CurrentLevel").setValue(1);

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("Mode").setValue("default");

                                                database.child("users")
                                                        .child(task.getResult().getUser().getUid())
                                                        .child("IsChallenging").setValue("0");

                                                logIn();

                                            } else {

                                                Toast.makeText(getApplicationContext(), "Login failed. Please, try again later.", Toast.LENGTH_SHORT).show();

                                            }

                                        }
                                    });

                        }

                    }
                });

    }

    private void logIn() {

        if (mAuth.getCurrentUser() != null) {

            final Intent intent = new Intent(getApplicationContext(), UserProfileActivity.class);
            final String uId = mAuth.getCurrentUser().getUid();

            database.child("users").child(uId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    database.child("users").child(uId).removeEventListener(this);
                    finish();

                    intent.putExtra("picture", Objects.requireNonNull(dataSnapshot.child("Picture").getValue()).toString());
                    intent.putExtra("email", Objects.requireNonNull(dataSnapshot.child("Email").getValue()).toString());
                    intent.putExtra("challengesAccepted", Objects.requireNonNull(dataSnapshot.child("ChallengesAccepted").getValue()).toString());
                    intent.putExtra("challengesDone", Objects.requireNonNull(dataSnapshot.child("ChallengesDone").getValue()).toString());
                    intent.putExtra("challengesFailed", Objects.requireNonNull(dataSnapshot.child("ChallengesFailed").getValue()).toString());
                    intent.putExtra("overallDistance", Objects.requireNonNull(dataSnapshot.child("OverallDistance").getValue()).toString());
                    intent.putExtra("currentLevel", Objects.requireNonNull(dataSnapshot.child("CurrentLevel").getValue()).toString());
                    intent.putExtra("isChallenging", Objects.requireNonNull(dataSnapshot.child("IsChallenging").getValue()).toString());
                    intent.putExtra("userId", uId);

                    startActivity(intent);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    databaseError.toException().printStackTrace();
                }
            });

        }

    }
}
