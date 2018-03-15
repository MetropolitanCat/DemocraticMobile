package com.example.edgar.democraticmessage.Activities;

import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    String getUName(){
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }
}
