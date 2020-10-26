package com.ambiwsstudio.hikingeverywhere;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class PhotoViewerActivity extends AppCompatActivity {

    String location;
    ImageView photoView;
    ImageView nextButtonImage;
    ImageView likeButton;
    ImageView reportButton;
    TextView likesCount;
    TextView photoBy;
    TextView dateTextView;
    Button showOnMapsButton;

    private boolean isLiked = false;
    private boolean isReported = false;

    ArrayList<Photo> photos = new ArrayList<>();

    private int currentIndex = 0;
    private String currentPhotoId;
    private String uId;

    static class Photo {

        String link;
        String id;
        String user;
        String location;
        ArrayList<String> likes;
        ArrayList<String> reports;

        Photo() {

            likes = new ArrayList<>();
            reports = new ArrayList<>();

        }

    }

    public void nextPhoto(View view) {

        if (currentIndex >= photos.size() - 2) {

            ViewGroup layoutNextImg = (ViewGroup) nextButtonImage.getParent();
            layoutNextImg.removeView(nextButtonImage);

        }

        setupViewPhoto(++currentIndex);

    }

    public void like(View view) {

        int likesCountInt = Integer.parseInt(String.valueOf(likesCount.getText()));

        if (isLiked) {

            photos.get(currentIndex).likes.remove(uId);

            FirebaseDatabase.getInstance().getReference()
                    .child("photos").child(currentPhotoId)
                    .child("likes").setValue(photos.get(currentIndex).likes);

            likesCountInt--;

            likeButton.setImageResource(R.drawable.like);
            isLiked = false;

        } else {

            photos.get(currentIndex).likes.add(uId);

            FirebaseDatabase.getInstance().getReference()
                    .child("photos").child(currentPhotoId)
                    .child("likes").setValue(photos.get(currentIndex).likes);

            likesCountInt++;

            likeButton.setImageResource(R.drawable.like2);
            isLiked = true;
        }

        likesCount.setText(String.valueOf(likesCountInt));

    }

    public void report(View view) {

        if (isReported) {

            photos.get(currentIndex).reports.remove(uId);

            FirebaseDatabase.getInstance().getReference()
                    .child("photos").child(currentPhotoId)
                    .child("reports").setValue(photos.get(currentIndex).reports);

            reportButton.setImageResource(R.drawable.report);
            isReported = false;

        } else {

            photos.get(currentIndex).reports.add(uId);

            FirebaseDatabase.getInstance().getReference()
                    .child("photos").child(currentPhotoId)
                    .child("reports").setValue(photos.get(currentIndex).reports);

            reportButton.setImageResource(R.drawable.report2);
            isReported = true;

        }

    }

    @SuppressLint("SetTextI18n")
    private void setupViewPhoto(int idx) {

        String downloadUrl = photos.get(idx).link;
        Picasso.get().load(downloadUrl).into(photoView);

        location = photos.get(idx).location;

        photoBy.setText(" Photo by " + photos.get(idx).user + " ");
        likesCount.setText(String.valueOf(photos.get(idx).likes.size()));

        uId = FirebaseAuth.getInstance().getUid();
        currentPhotoId = photos.get(idx).id;

        if (Collections.frequency(photos.get(idx).likes, uId) > 0) {

            likeButton.setImageResource(R.drawable.like2);
            isLiked = true;

        } else {

            likeButton.setImageResource(R.drawable.like);
            isLiked = false;

        }

        if (Collections.frequency(photos.get(idx).reports, uId) > 0) {

            reportButton.setImageResource(R.drawable.report2);
            isReported = true;

        } else {

            reportButton.setImageResource(R.drawable.report);
            isReported = false;

        }
    }

    private void loadPhotos(final boolean forShuffling) {

        FirebaseDatabase.getInstance().getReference().child("photos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                FirebaseDatabase.getInstance().getReference().child("photos").removeEventListener(this);

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                    Photo currentPhoto = new Photo();

                    currentPhoto.id = postSnapshot.getKey();

                    if (postSnapshot.child("link").getValue() != null) {

                        currentPhoto.link = String.valueOf(postSnapshot.child("link").getValue());

                    }

                    if (postSnapshot.child("user").getValue() != null) {

                        currentPhoto.user = String.valueOf(postSnapshot.child("user").getValue());

                    }

                    if (postSnapshot.child("location").getValue() != null) {

                        currentPhoto.location = String.valueOf(postSnapshot.child("location").getValue());

                    }

                    /*
                        Likes parsing
                    */

                    for (DataSnapshot likesSnapshot : postSnapshot.child("likes").getChildren()) {

                        currentPhoto.likes.add(String.valueOf(likesSnapshot.getValue()));

                    }

                    /*
                        Reports parsing
                    */

                    for (DataSnapshot reportsSnapshot : postSnapshot.child("reports").getChildren()) {

                        currentPhoto.reports.add(String.valueOf(reportsSnapshot.getValue()));

                    }

                    photos.add(currentPhoto);

                }

                if (!forShuffling) {

                    HikingEverywhereTools.timSortPhotos(photos, photos.size());
                    Collections.reverse(photos);

                } else {

                    Collections.shuffle(photos);

                }

                setupViewPhoto(currentIndex);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }

        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        Intent currentIntent = getIntent();
        photoView = findViewById(R.id.photoView);
        nextButtonImage = findViewById(R.id.nextButton);
        likeButton = findViewById(R.id.likeButton);
        reportButton = findViewById(R.id.reportButton);
        likesCount = findViewById(R.id.likesCount);
        photoBy = findViewById(R.id.photoBy);
        showOnMapsButton = findViewById(R.id.showOnMapsButton);
        dateTextView = findViewById(R.id.dateTextView);

        if (savedInstanceState != null) {

            currentIndex = savedInstanceState.getInt("count");

        }

        if (currentIntent.getStringExtra("mode") != null) {

            if (Objects.requireNonNull(currentIntent.getStringExtra("mode")).equals("topPhotos")) {

                loadPhotos(false);

            } else if (Objects.requireNonNull(currentIntent.getStringExtra("mode")).equals("randomPhotos")) {

                loadPhotos(true);

            }

        } else {

            /*
                'History' Photo View
             */

            ViewGroup layoutNextImg = (ViewGroup) nextButtonImage.getParent();
            layoutNextImg.removeView(nextButtonImage);

            ViewGroup layoutLikeBtn = (ViewGroup) likeButton.getParent();
            layoutLikeBtn.removeView(likeButton);

            ViewGroup layoutReportBtn = (ViewGroup) reportButton.getParent();
            layoutReportBtn.removeView(reportButton);

            ViewGroup layoutLikesCount = (ViewGroup) likesCount.getParent();
            layoutLikesCount.removeView(likesCount);

            /*
                Layout style for history
             */

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(1, -4, 0, 0);

            showOnMapsButton.setLayoutParams(params);

            if (currentIntent.getStringExtra("date") != null) {

                dateTextView.setText(currentIntent.getStringExtra("date"));

            }

            if (currentIntent.getStringExtra("link") != null) {

                String downloadUrl = currentIntent.getStringExtra("link");
                Picasso.get().load(downloadUrl).into(photoView);

            }

            if (currentIntent.getStringExtra("location") != null) {

                location = currentIntent.getStringExtra("location");

            }

        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("count", currentIndex);
    }

    public void showOnMaps(View view) {

        Intent intent = new Intent(getApplicationContext(), LocationViewerActivity.class);
        intent.putExtra("location", location);
        startActivity(intent);

    }
}
