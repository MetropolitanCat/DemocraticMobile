package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import android.os.Vibrator;

import com.example.edgar.democraticmessage.Models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {
    //Superclass activity
    //Master Toast declaration
    Toast masterToast;

    Vibrator vibrator;

    public static final List<String> nameIds = new ArrayList<>();
    public static final List<User> name = new ArrayList<>();

    //Method for obtaining the current user ID
    @NonNull
    String getUid() {
        try{
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        catch (NullPointerException e){
            return "No user found";
        }

    }
    //Method for obtaining the current users Username using their email
    @NonNull
    String getUName(){
        try{
            String temp = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            //Trim off the email from the username and return the result
            return  temp.split("@")[0];
        }
        catch (NullPointerException e){
            return "No Email found";
        }
    }
    //Method to start vibration on device
    public void clickVibrate(){
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(250,VibrationEffect.DEFAULT_AMPLITUDE));
        }else{
            //deprecated in API 26
            vibrator.vibrate(250);
        }
    }

    public void populateUsers(){
        DatabaseReference data = FirebaseDatabase.getInstance().getReference().child("users");
        data.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                User user = dataSnapshot.getValue(User.class);
                assert user!= null;
                nameIds.add(dataSnapshot.getKey());
                name.add(user);
                Log.d("Base","Size is " + nameIds.size());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
