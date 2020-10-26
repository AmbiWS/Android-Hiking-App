package com.ambiwsstudio.hikingeverywhere;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class ChallengeActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSION_ID = 1;

    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_ID = 2;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String currentTime;
    private String userName;

    private boolean isChallengeSetup = true;
    private LatLng startLocationLatLng;
    private LatLng finishLocationLatLng;

    private static final double latitudeMinStep = 0.0001;
    private static final double latitudeMinStepDistance = 11.11870;

    private static final double longitudeMinStep = 0.0001;
    private static final double longitudeMinStepDistance = 7.48651;

    private int maxDistanceMeters;
    private int distanceFromStartToFinish;
    private int challengesAccepted;
    private int challengesFailed;
    private int challengesDone;
    private int level;
    private int overallDistance;
    private String uId;
    private static final int distancePerLevelMultiplier = 500;
    private static final int maxDistanceDeviation = 50;

    Intent currentIntent;
    TextView distanceToFinishTextView;
    Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        currentIntent = getIntent();

        uId = FirebaseAuth.getInstance().getUid();
        maxDistanceMeters = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("level"))) * distancePerLevelMultiplier;
        challengesAccepted = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("challengesAccepted")));
        challengesFailed = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("challengesFailed")));
        challengesDone = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("challengesDone")));
        level = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("level")));
        overallDistance = Integer.parseInt(Objects.requireNonNull(currentIntent.getStringExtra("overallDistance")));
        userName = Objects.requireNonNull(currentIntent.getStringExtra("username"));

        if (savedInstanceState != null) {

            if (savedInstanceState.getInt("active") == 1) {

                isChallengeSetup = false;

                double startLat = savedInstanceState.getDouble("startLocationLat");
                double startLng = savedInstanceState.getDouble("startLocationLng");

                double finishLat = savedInstanceState.getDouble("finishLocationLat");
                double finishLng = savedInstanceState.getDouble("finishLocationLng");

                startLocationLatLng = new LatLng(startLat, startLng);
                finishLocationLatLng = new LatLng(finishLat, finishLng);

            }

        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("active", 1);
        outState.putDouble("startLocationLat", startLocationLatLng.latitude);
        outState.putDouble("startLocationLng", startLocationLatLng.longitude);
        outState.putDouble("finishLocationLat", finishLocationLatLng.latitude);
        outState.putDouble("finishLocationLng", finishLocationLatLng.longitude);
    }

    public static double parseLatLng(String latOrLng, String data) {

        if (latOrLng.equals("lat")) {

            data = data.substring(data.indexOf("latitude=") + 9, data.indexOf(","));
            return Double.parseDouble(data);

        } else if (latOrLng.equals("lng")) {

            data = data.substring(data.indexOf("longitude=") + 10, data.length() - 1);
            return Double.parseDouble(data);

        } else return 0;

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        distanceToFinishTextView = findViewById(R.id.distanceToFinishTextView);
        finishButton = findViewById(R.id.finishButton);

        if (currentIntent.getStringExtra("active") != null &&
                Objects.requireNonNull(currentIntent.getStringExtra("active")).equals("1")) {

            isChallengeSetup = false;

            double startLat = parseLatLng("lat", currentIntent.getStringExtra("startLocation"));
            double startLng = parseLatLng("lng", currentIntent.getStringExtra("startLocation"));

            double finishLat = parseLatLng("lat", currentIntent.getStringExtra("finishLocation"));
            double finishLng = parseLatLng("lng", currentIntent.getStringExtra("finishLocation"));

            startLocationLatLng = new LatLng(startLat, startLng);
            finishLocationLatLng = new LatLng(finishLat, finishLng);

            mapSetup();

        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                updateMap(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);

        } else {


            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLoc != null) {

                updateMap(lastKnownLoc);

            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLoc != null) {

                    updateMap(lastKnownLoc);

                }

            }

        } else if (requestCode == CAMERA_PERMISSION_ID) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);

            }

        }

    }

    @SuppressLint("SetTextI18n")
    public void updateMap(Location location) {

        if (!isChallengeSetup) {

            LatLng userLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();

            mMap.addMarker(new MarkerOptions().position(startLocationLatLng).title("Start Location").icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions().position(userLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addMarker(new MarkerOptions().position(finishLocationLatLng).title("Finish Location").icon(BitmapDescriptorFactory
                    .defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            Location userLocation = new Location("");
            userLocation.setLatitude(userLocationLatLng.latitude);
            userLocation.setLongitude(userLocationLatLng.longitude);

            Location finishLocation = new Location("");
            finishLocation.setLatitude(finishLocationLatLng.latitude);
            finishLocation.setLongitude(finishLocationLatLng.longitude);

            int distanceLeft = (int) Math.ceil(userLocation.distanceTo(finishLocation));
            distanceToFinishTextView.setText("Distance to finish: " + distanceLeft + " m");

            if (distanceLeft > maxDistanceDeviation) {
                finishButton.setEnabled(false);
            } else {
                finishButton.setEnabled(true);
            }

        } else {

            isChallengeSetup = false;
            startLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            finishLocationLatLng = generateFinishLocation(maxDistanceMeters, startLocationLatLng);

            mapSetup();

            /*
                Save to Firebase
             */

            FirebaseDatabase.getInstance().getReference()
                    .child("users").child(uId).child("CurrentChallenge")
                    .child("StartLocation").setValue(startLocationLatLng);

            FirebaseDatabase.getInstance().getReference()
                    .child("users").child(uId).child("CurrentChallenge")
                    .child("FinishLocation").setValue(finishLocationLatLng);

            FirebaseDatabase.getInstance().getReference()
                    .child("users").child(uId).child("CurrentChallenge")
                    .child("Active").setValue("1");
        }

    }

    private void mapSetup() {

        mMap.clear();

        /*
            Correct zoom between two locations
        */

        final RelativeLayout mapLayout = findViewById(R.id.mapLayout);
        mapLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                mapLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                ArrayList<Marker> markers = new ArrayList<>();
                markers.add(mMap.addMarker(new MarkerOptions().position(startLocationLatLng).title("Start Location").icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
                markers.add(mMap.addMarker(new MarkerOptions().position(finishLocationLatLng).title("Finish Location").icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {

                    builder.include(marker.getPosition());

                }
                LatLngBounds bounds = builder.build();

                int padding = 300;
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.animateCamera(cu);

            }
        });

        Location startLocation = new Location("");
        startLocation.setLatitude(startLocationLatLng.latitude);
        startLocation.setLongitude(startLocationLatLng.longitude);

        Location finishLocation = new Location("");
        finishLocation.setLatitude(finishLocationLatLng.latitude);
        finishLocation.setLongitude(finishLocationLatLng.longitude);

        distanceFromStartToFinish = (int) Math.ceil(startLocation.distanceTo(finishLocation));

    }

    private LatLng generateFinishLocation(int maxDistanceMeters, LatLng startLocationLatLng) {

        int maxLatitudeInjections = (int) Math.ceil(maxDistanceMeters * 1.0 / latitudeMinStepDistance);

        int latitudeInjections = (int) (Math.random() * maxLatitudeInjections);
        int unusedDistanceMeters = (int) ((maxLatitudeInjections - latitudeInjections) * latitudeMinStepDistance);
        int longitudeInjections = (int) (unusedDistanceMeters * 1.0 / longitudeMinStepDistance);

        double latInjection = latitudeMinStep * latitudeInjections;
        double lngInjection = longitudeMinStep * longitudeInjections;

        double startLat = startLocationLatLng.latitude;
        double startLng = startLocationLatLng.longitude;

        latInjection = Math.random() < 0.5 ? -latInjection : latInjection;
        lngInjection = Math.random() < 0.5 ? -lngInjection : lngInjection;

        return new LatLng(startLat + latInjection, startLng + lngInjection);

    }

    @Override
    public void onBackPressed() {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:

                        String uId = FirebaseAuth.getInstance().getUid();

                        if (uId != null) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("IsChallenging").setValue("0");

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("ChallengesAccepted").setValue(challengesAccepted + 1);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("ChallengesFailed").setValue(challengesFailed + 1);

                            Date now = new Date();
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            currentTime = "\"" + sdf.format(now) + "\"";

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("History").child(currentTime)
                                    .child("Success").setValue("0");

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("History").child(currentTime)
                                    .child("StartLocation").setValue(startLocationLatLng);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("History").child(currentTime)
                                    .child("FinishLocation").setValue(finishLocationLatLng);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("History").child(currentTime)
                                    .child("Distance").setValue(distanceFromStartToFinish);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("users").child(uId).child("CurrentChallenge")
                                    .child("Active").setValue("0");
                        }

                        backToProfile();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        /*
                            Challenge Continuation
                         */

                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to deny this challenge?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK && data != null) {

            if (data.getExtras() != null) {

                Date now = new Date();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                currentTime = "\"" + sdf.format(now) + "\"";

                Bitmap photo = (Bitmap) data.getExtras().get("data");
                FirebaseStorage storage = FirebaseStorage.getInstance("gs://hiking-everywhere.appspot.com");
                StorageReference reference = storage.getReference();
                final String imageName = UUID.randomUUID().toString();
                StorageReference userPhotoRef = reference.child("userPhotos/" + imageName + ".jpg");

                assert photo != null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] photoBytes = baos.toByteArray();

                UploadTask uploadTask = userPhotoRef.putBytes(photoBytes);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        exception.printStackTrace();
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
                                        String uId = Objects.requireNonNull(FirebaseAuth.getInstance().getUid());

                                        /*
                                            History record
                                         */

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("Success").setValue("1");

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("StartLocation").setValue(startLocationLatLng);

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("FinishLocation").setValue(finishLocationLatLng);

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("Photo").setValue(url);

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("PhotoId").setValue(imageName);

                                        mDatabase.child("users").child(uId)
                                                .child("History").child(currentTime).child("Distance").setValue(distanceFromStartToFinish);

                                        /*
                                            User data
                                         */

                                        mDatabase.child("users").child(uId)
                                                .child("ChallengesAccepted").setValue(challengesAccepted + 1);

                                        mDatabase.child("users").child(uId)
                                                .child("ChallengesDone").setValue(challengesDone + 1);

                                        mDatabase.child("users").child(uId)
                                                .child("IsChallenging").setValue("0");

                                        mDatabase.child("users").child(uId)
                                                .child("CurrentLevel").setValue(level + 1);

                                        mDatabase.child("users").child(uId)
                                                .child("OverallDistance").setValue(overallDistance + distanceFromStartToFinish);

                                        FirebaseDatabase.getInstance().getReference()
                                                .child("users").child(uId).child("CurrentChallenge")
                                                .child("Active").setValue("0");

                                        /*
                                            Photos data
                                         */

                                        mDatabase.child("photos").child(imageName)
                                                .child("link").setValue(url);

                                        ArrayList<String> likes = new ArrayList<>();
                                        likes.add(uId);

                                        ArrayList<String> reports = new ArrayList<>();
                                        reports.add("null");

                                        mDatabase.child("photos").child(imageName)
                                                .child("likes").setValue(likes);

                                        mDatabase.child("photos").child(imageName)
                                                .child("reports").setValue(reports);

                                        mDatabase.child("photos").child(imageName)
                                                .child("location").setValue(finishLocationLatLng);

                                        mDatabase.child("photos").child(imageName)
                                                .child("user").setValue(userName);

                                        /*
                                            Return to profile
                                         */

                                        backToProfile();

                                    }

                                }
                            });

                        }

                    }
                });


            }

        }
    }

    private void backToProfile() {

        Intent mainIntent = new Intent(getBaseContext(), MainActivity.class);
        finish();
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(mainIntent);

    }

    public void finishChallenge(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_ID);

        } else {

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);

        }

    }
}
