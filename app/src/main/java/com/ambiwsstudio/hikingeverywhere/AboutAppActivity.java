package com.ambiwsstudio.hikingeverywhere;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();
    }

    public void onPlayPage(View view) {

        final String googlePlayLink = "https://play.google.com/store/apps/developer?id=Ambi+WS+Studio";
        Uri uri = Uri.parse(googlePlayLink);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(launchBrowser);

    }
}
