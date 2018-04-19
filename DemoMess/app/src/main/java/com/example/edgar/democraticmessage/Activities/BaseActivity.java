package com.example.edgar.democraticmessage.Activities;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {
    //Superclass activity
    //Method for obtaining the current user ID
    @NonNull
    String getUid() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    //Method for obtaining the current users Username using their email
    @NonNull
    String getUName(){
        String temp = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        return  temp.split("@")[0];
    }
}
