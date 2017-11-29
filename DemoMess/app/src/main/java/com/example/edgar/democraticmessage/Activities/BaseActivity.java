package com.example.edgar.democraticmessage.Activities;

/**
 * Created by Edgar on 13/11/2017.
 */

import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
