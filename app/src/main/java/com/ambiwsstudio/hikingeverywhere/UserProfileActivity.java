package com.ambiwsstudio.hikingeverywhere;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Objects;

public class UserProfileActivity extends AppCompatActivity {

    ImageView userPicture;
    TextView userHowdyView;
    TextView userChallengesView;
    TextView userWinRateView;
    TextView userDistanceView;
    TextView userLevelView;

    public static final int PICK_IMAGE = 1;
    public static final int CHALLENGE_DONE = 2;
    private String uId;
    private String level;
    private String challengesAcceptedStr;
    private String challengesFailedStr;
    private String challengesDoneStr;
    private String overallDistanceStr;
    private String userName;

    public void viewTopPhotos(View view) {

        Intent intent = new Intent(getApplicationContext(), PhotoViewerActivity.class);
        intent.putExtra("mode", "topPhotos");
        startActivity(intent);

    }

    public void viewRandomPhotos(View view) {

        Intent intent = new Intent(getApplicationContext(), PhotoViewerActivity.class);
        intent.putExtra("mode", "randomPhotos");
        startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.aboutApp) {

            Intent intent = new Intent(getApplicationContext(), AboutAppActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.credits) {

            Intent intent = new Intent(getApplicationContext(), CreditsActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.logout) {

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            finish();
            intent.putExtra("signOut", "1");
            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void setupOnCreate() {

        Intent currentIntent = getIntent();

        userPicture = findViewById(R.id.userPicture);
        userHowdyView = findViewById(R.id.userHowdyView);
        userChallengesView = findViewById(R.id.userChallengesView);
        userWinRateView = findViewById(R.id.userWinrateView);
        userDistanceView = findViewById(R.id.userDistanceView);
        userLevelView = findViewById(R.id.userLevelView);

        Picasso.get().load(currentIntent.getStringExtra("picture")).into(userPicture);
        userHowdyView.setText("Howdy, " + userName);

        int challengesAccepted = Integer.parseInt(Objects.requireNonNull(challengesAcceptedStr));
        int challengesDone = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("challengesDone")));
        @SuppressLint("DefaultLocale") String winRate = challengesAccepted > 0 ?
                String.format("%.2f", challengesDone * 1.0f / challengesAccepted * 100) :
                "TBD";

        userChallengesView.setText("Challenges accepted: " + challengesAccepted);

        userWinRateView.setText("Win rate: " + winRate + " %");
        userDistanceView.setText("Overall distance: " + currentIntent.getStringExtra("overallDistance") + " m");
        userLevelView.setText("Current level: " + level);

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        setTitle("Profile");

        Intent currentIntent = getIntent();
        uId = currentIntent.getStringExtra("userId");
        level = currentIntent.getStringExtra("currentLevel");
        challengesAcceptedStr = currentIntent.getStringExtra("challengesAccepted");
        challengesFailedStr = currentIntent.getStringExtra("challengesFailed");
        challengesDoneStr = currentIntent.getStringExtra("challengesDone");
        overallDistanceStr = currentIntent.getStringExtra("overallDistance");
        userName = currentIntent.getStringExtra("email");

        FirebaseDatabase.getInstance().getReference().child("users").child(uId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                FirebaseDatabase.getInstance().getReference().child("users").child(uId).removeEventListener(this);

                if (Objects.requireNonNull(dataSnapshot.child("IsChallenging").getValue()).toString().equals("1")) {

                    /*
                        Redirect on challenge activity
                     */

                    if (Objects.requireNonNull(dataSnapshot.child("CurrentChallenge").child("Active").getValue()).toString().equals("1")) {

                        Intent intent = new Intent(getApplicationContext(), ChallengeActivity.class);

                        intent.putExtra("active", "1");
                        intent.putExtra("startLocation", Objects.requireNonNull(dataSnapshot.child("CurrentChallenge")
                                .child("StartLocation").getValue()).toString());
                        intent.putExtra("finishLocation", Objects.requireNonNull(dataSnapshot.child("CurrentChallenge")
                                .child("FinishLocation").getValue()).toString());


                        startChallengeActivity(intent);

                    } else {

                        Intent intent = new Intent(getApplicationContext(), ChallengeActivity.class);
                        startChallengeActivity(intent);

                    }

                } else {

                    setupOnCreate();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE) {

            FirebaseStorage storage = FirebaseStorage.getInstance("gs://hiking-everywhere.appspot.com");
            StorageReference reference = storage.getReference();

            if (data == null)
                return;

            Uri pickedImage = data.getData();
            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), pickedImage);
                userPicture.setImageBitmap(bitmap);

                if (pickedImage != null) {
                    StorageReference userPictureRef = reference.child("userPics/" + pickedImage.getLastPathSegment());
                    UploadTask uploadTask = userPictureRef.putFile(pickedImage);

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            if (taskSnapshot.getMetadata() != null
                                    && taskSnapshot.getMetadata().getReference() != null) {

                                final Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                downloadUrl.addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {

                                        if (downloadUrl.getResult() != null) {

                                            String url = downloadUrl.getResult().toString();
                                            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                                            mDatabase.child("users").child(uId).child("Picture").setValue(url);

                                        }

                                    }
                                });

                            }
                        }
                    });

                }

            } catch (IOException e) {

                e.printStackTrace();

            }

        } else if (requestCode == CHALLENGE_DONE) {

            setupOnCreate();

        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FirebaseAuth.getInstance().signOut();
    }

    @SuppressLint("IntentReset")
    public void changePicture(View view) {

        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    public void viewHistory(View view) {

        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);

    }

    public void startNewChallenge(View view) {

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(uId).child("IsChallenging").setValue("1");
        Intent intent = new Intent(getApplicationContext(), ChallengeActivity.class);
        startChallengeActivity(intent);

    }

    private void startChallengeActivity(Intent intent) {

        finish();
        intent.putExtra("level", level);
        intent.putExtra("challengesAccepted", challengesAcceptedStr);
        intent.putExtra("challengesFailed", challengesFailedStr);
        intent.putExtra("challengesDone", challengesDoneStr);
        intent.putExtra("overallDistance", overallDistanceStr);
        intent.putExtra("username", userName);
        startActivity(intent);

    }
}
