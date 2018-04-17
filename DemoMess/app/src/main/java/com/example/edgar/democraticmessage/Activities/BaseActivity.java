package com.example.edgar.democraticmessage.Activities;

import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {
    //Superclass activity
    //Method for obtaining the current user ID
    String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    //Method for obtaining the current users Username using their email
    String getUName(){
        return FirebaseAuth.getInstance().getCurrentUser().getEmail();
    }
}
