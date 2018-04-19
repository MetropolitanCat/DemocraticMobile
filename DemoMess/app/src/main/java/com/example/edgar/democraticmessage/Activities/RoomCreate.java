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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class RoomCreate extends BaseActivity {

    private DatabaseReference mDatabase;
    private EditText roomName;
    private EditText roomBudget;
    private EditText roomPassword;
    private EditText roomParticipants;
    private Spinner spinnerRoomType;
    private Spinner spinnerBudgetType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_create);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        roomName = findViewById(R.id.messageName);
        roomBudget = findViewById(R.id.roomBudget);
        roomPassword = findViewById(R.id.roomPass);
        roomParticipants = findViewById(R.id.roomPart);

        spinnerRoomType = findViewById(R.id.spinnerRT);
        ArrayAdapter<CharSequence> adapterRT = ArrayAdapter.createFromResource(this,
                R.array.roomType, android.R.layout.simple_spinner_item);
        adapterRT.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoomType.setAdapter(adapterRT);

        spinnerBudgetType = findViewById(R.id.spinnerBT);
        ArrayAdapter<CharSequence> adapterBT = ArrayAdapter.createFromResource(this,
                R.array.budgetType, android.R.layout.simple_spinner_item);
        adapterBT.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBudgetType.setAdapter(adapterBT);
    }

    public void setupRoom(@SuppressWarnings("unused") View v) {

        final String name = roomName.getText().toString();
        final String budget = roomBudget.getText().toString();
        final String password = roomPassword.getText().toString();
        final String participants = roomParticipants.getText().toString();
        final String budgetType = spinnerBudgetType.getSelectedItem().toString();
        final String conferenceType = spinnerRoomType.getSelectedItem().toString();
        final int startingBudget = TextUtils.isEmpty(budget) ? 0 : Integer.parseInt(budget) / Integer.parseInt(participants);

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getApplicationContext(), "You need to input a room name",Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(budget)) {
            Toast.makeText(getApplicationContext(), "You need to input a budget",Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(participants)) {
            Toast.makeText(getApplicationContext(), "You need to input the amount of participants",Toast.LENGTH_SHORT).show();
        }
        else{
            final String userId = getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        if (user == null) {
                            Toast.makeText(getBaseContext(), "An Error has occured", Toast.LENGTH_LONG).show();
                            finish();
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

    }

    private String createRoom(String name, String type,
                            int budget, int startBudget,
                            String conference, int participants,
                            String password){
        //Create the room inside the database
        String key = mDatabase.child("rooms").push().getKey();
        RoomType room = new RoomType(name, type, budget, startBudget, conference, participants, password, getUid());
        Map<String, Object> newRoom = room.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
         childUpdates.put("/rooms/" + key, newRoom);

        mDatabase.updateChildren(childUpdates);
        return  key;
    }

    private void createMasterUser(final String roomKey, final int startBudget){

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
            //Create the master user, who has the power to delete the room
            Participant part = new Participant("Master: " + getUName(),getUid(), startBudget);
            Map<String, Object> sendMessage = part.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put( "/participants/" + "" + roomKey + "/" + getUid(), sendMessage);

                mDatabase.updateChildren(childUpdates);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}