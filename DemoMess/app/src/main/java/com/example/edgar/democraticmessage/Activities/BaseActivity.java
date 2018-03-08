package com.example.edgar.democraticmessage.Activities;

import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    public String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public String getUName(){
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }
}
