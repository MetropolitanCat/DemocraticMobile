package com.example.edgar.democraticmessage.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StatisticsUser extends BaseActivity {

    private TextView userName;
    private TextView roomID;
    private TextView userTimeUsed;
    private String userID;
    private Button userRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_user);

        userName = findViewById(R.id.userStatName);
        roomID = findViewById(R.id.roomStatID);
        userTimeUsed = findViewById(R.id.userBudgetUsed);
        userRequest = findViewById(R.id.requestButton);

        Intent intent = getIntent();
        userName.setText(intent.getStringExtra("userName"));
        userID = intent.getStringExtra("userID");
        roomID.setText(intent.getStringExtra("RoomId"));
        userTimeUsed.setText(intent.getStringExtra("BudgetUsed"));
    }

    public void request(View v){
        final DatabaseReference newParticipant = FirebaseDatabase.getInstance().getReference();
        final DatabaseReference roomUser = newParticipant.child("participants").child("" + roomID.getText()).child(userID);

        roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("userRequest").exists()){
                    Log.d("Statistics", "Request exists");
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
