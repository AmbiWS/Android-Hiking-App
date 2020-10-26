package com.ambiwsstudio.hikingeverywhere;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    ArrayList<String> history = new ArrayList<>();
    ArrayAdapter<String> adapter;
    ListView listView;
    private String uId;
    ArrayList<History> histories = new ArrayList<>();

    static class History {

        String date;
        String success;
        String startLocation;
        String finishLocation;
        String photo;
        String photoId;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setTitle("History");

        uId = FirebaseAuth.getInstance().getUid();
        final Typeface mTypeface = ResourcesCompat.getFont(this, R.font.abeezee);

        listView = findViewById(R.id.historyView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, history){

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

                TextView item = (TextView) super.getView(position, convertView, parent);
                item.setTypeface(mTypeface);

                /*if (item.getText().toString().contains("Success")) {

                    item.setBackgroundColor(getResources().getColor(R.color.success));

                } else {

                    item.setBackgroundColor(getResources().getColor(R.color.fail));

                }*/

                return item;

            }

        };
        listView.setAdapter(adapter);

        FirebaseDatabase.getInstance().getReference().child("users").child(uId).child("History").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                FirebaseDatabase.getInstance().getReference().child("users").child(uId).child("History").removeEventListener(this);

                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {

                    History currentHistory = new History();
                    currentHistory.date = Objects.requireNonNull(postSnapshot.getKey()).substring(1, postSnapshot.getKey().length() - 1);
                    String result = currentHistory.date;

                    if (postSnapshot.child("Success").getValue() != null) {

                        if (Objects.requireNonNull(postSnapshot.child("Success").getValue()).equals("1")) {

                            currentHistory.success = "Success";
                            result += "                      " + currentHistory.success; // 22sp

                        } else {

                            currentHistory.success = "Fail";
                            result += "                            " + currentHistory.success; // 28sp

                        }

                    }

                    if (postSnapshot.child("StartLocation").getValue() != null) {

                        currentHistory.startLocation = String.valueOf(postSnapshot.child("StartLocation").getValue());

                    }

                    if (postSnapshot.child("FinishLocation").getValue() != null) {

                        currentHistory.finishLocation = String.valueOf(postSnapshot.child("FinishLocation").getValue());

                    }

                    if (postSnapshot.child("Photo").getValue() != null) {

                        currentHistory.photo = String.valueOf(postSnapshot.child("Photo").getValue());

                    }

                    if (postSnapshot.child("PhotoId").getValue() != null) {

                        currentHistory.photoId = String.valueOf(postSnapshot.child("PhotoId").getValue());

                    }

                    history.add(result);
                    histories.add(currentHistory);
                }

                Collections.reverse(history);
                Collections.reverse(histories); // Nightmare - is forget to add this lane
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String finishLoc = histories.get(position).finishLocation;

                if (histories.get(position).success.equals("Success")) {

                    Intent intent = new Intent(getApplicationContext(), PhotoViewerActivity.class);
                    intent.putExtra("location", finishLoc);
                    intent.putExtra("link", histories.get(position).photo);
                    intent.putExtra("photoId", histories.get(position).photoId);
                    intent.putExtra("date", histories.get(position).date);
                    startActivity(intent);

                } else {

                    Intent intent = new Intent(getApplicationContext(), LocationViewerActivity.class);
                    intent.putExtra("location", finishLoc);
                    startActivity(intent);

                }

            }
        });
    }
}
