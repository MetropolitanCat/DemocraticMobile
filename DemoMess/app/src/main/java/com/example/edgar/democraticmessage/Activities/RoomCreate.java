package com.example.edgar.democraticmessage.Activities;

import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.Models.RoomType;
import com.example.edgar.democraticmessage.Models.User;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class RoomCreate extends BaseActivity {

    private DatabaseReference mDatabase;
    private EditText roomName;
    private EditText roomBudget;
    private EditText roomPassword;
    private EditText roomParticipants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_create);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        roomName = findViewById(R.id.messageName);
        roomBudget = findViewById(R.id.roomBudget);
        roomPassword = findViewById(R.id.roomPass);
        roomParticipants = findViewById(R.id.roomPart);
    }

    public void setupRoom(View v) {

        final String name = roomName.getText().toString();
        final String budget = roomBudget.getText().toString();
        final String password = roomPassword.getText().toString();
        final String participants = roomParticipants.getText().toString();
        final int budgetType = 2;
        final String conferenceType = "Basic";
        final int startingBudget = Integer.parseInt(budget) / Integer.parseInt(participants);

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "You need to input a room name",Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(budget)) {
            Toast.makeText(this, "You need to input a budget",Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(participants)) {
            Toast.makeText(this, "You need to input the amount of participants",Toast.LENGTH_SHORT).show();
            return;
        }

        final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        if (user == null) {
                            // User is null, error out
                        } else {
                            //Create the master user who has the ability to delete the room
                            //The room key is obtained from the create room function
                            createMasterUser(
                                    createRoom(name,
                                               budgetType,
                                               Integer.parseInt(budget),
                                               startingBudget,
                                               conferenceType,
                                               Integer.parseInt(participants), password), startingBudget);
                            //Leave activity
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private String createRoom(String name, int type,
                            int budget, int startBudget,
                            String conference, int participants,
                            String password){
        String key = mDatabase.child("rooms").push().getKey();
        RoomType room = new RoomType(name, type, budget, startBudget, conference, participants, password, getUid());
        Map<String, Object> newRoom = room.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
         childUpdates.put("/rooms/" + key, newRoom);

        mDatabase.updateChildren(childUpdates);
        return  key;
    }

    private void createMasterUser(final String roomKey, final int startBudget){

        final DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference();

        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

            Participant part = new Participant("Master: " + getUName(),getUid(), startBudget);
            Map<String, Object> sendMessage = part.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put( "/participants/" + "" + roomKey + "/" + getUid(), sendMessage);

            dataRef.updateChildren(childUpdates);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}