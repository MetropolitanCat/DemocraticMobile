package com.example.edgar.democraticmessage.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.edgar.democraticmessage.R;
import com.example.edgar.democraticmessage.Services.UserData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatisticsUser extends BaseActivity {

    private TextView roomID;
    private TextView userTimeUsed;
    private String userID;
    private String uBudget = "";
    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_user);

        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        TextView userName = findViewById(R.id.userStatName);
        roomID = findViewById(R.id.roomStatID);
        userTimeUsed = findViewById(R.id.userBudgetUsed);
        Button userRequest = findViewById(R.id.requestButton);

        Intent intent = getIntent();
        userName.setText(intent.getStringExtra("userName"));
        userID = intent.getStringExtra("userID");
        roomID.setText(intent.getStringExtra("RoomId"));
        uBudget = intent.getStringExtra("BudgetUsed");

    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder
                service) {
            dataService = (UserData.DataBinder) service;
            if(dataService.getRoomType().equals("Blind Man") )
                userTimeUsed.setText("???????");
            else
                userTimeUsed.setText(uBudget);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceConnection!=null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    public void request(View v){
        final DatabaseReference newParticipant = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference roomUser = newParticipant.child("participants").child("" + roomID.getText()).child(userID);

        roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("userRequest").exists()){
                    Log.d("Statistics", "Request exists");
                    Toast.makeText(getApplicationContext(), "User has a request pending!",Toast.LENGTH_SHORT).show();
                }
                else{
                    Log.d("Statistics", "Request does not exist");
                    roomUser.child("userRequest").setValue(getUid());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
