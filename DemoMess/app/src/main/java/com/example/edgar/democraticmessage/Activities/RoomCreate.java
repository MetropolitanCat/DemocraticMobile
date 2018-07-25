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
    private EditText roomClassSpec;
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
        roomClassSpec = findViewById(R.id.roomClassSpec);
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
        //Create master Toast for the activity
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    private int budgetCalc(int budget, int participants){
        //Make sure the budget and participants set are not 0
        if(budget == 0){
            return -1;
        }
        else if(participants == 0 ){
            return -2;
        }
        //Make sure that the budget can be equally divided among the participants
        else if(budget % participants != 0 ){
            return -3;
        }
        else{
            return  budget / participants;
        }
    }

    public void setupRoom(@SuppressWarnings("unused") View v) {
        //Obtain input from all the inputs
        final String name = roomName.getText().toString();
        final String budget = roomBudget.getText().toString();
        final String classSpec = TextUtils.isEmpty(roomClassSpec.getText().toString()) ? "0" : roomClassSpec.getText().toString();
        final String password = roomPassword.getText().toString();
        final String participants = roomParticipants.getText().toString();
        final String budgetType = spinnerBudgetType.getSelectedItem().toString();
        final String conferenceType = spinnerRoomType.getSelectedItem().toString();
        //Send the obtained budget and participant numbers to the budget calc function
        final int startingBudget = budgetCalc(
                                            //If either budget or participants are empty, set them 0, otherwise parse the value input
                                            TextUtils.isEmpty(budget) ? 0 : Integer.parseInt(budget),
                                            TextUtils.isEmpty(participants) ? 0 :Integer.parseInt(participants));

        if (TextUtils.isEmpty(name)) {
            //Warn the user if a name for the room is not set
            masterToast.setText("You need to input a room name");
            masterToast.show();
        }
        else if(startingBudget == -1){
            //Warn the user if the budget is not set or is 0
            masterToast.setText("You need to input a budget");
            masterToast.show();
        }
        else if(startingBudget == -2){
            //Warn the user if the participants are not set or are 0
            masterToast.setText("You need to input the amount of participants");
            masterToast.show();
        }
        else if(startingBudget == -3){
            //Warn the user that the budget cant be equally divided between the participants
            masterToast.setText("Stating budget not a whole number");
            masterToast.show();
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
                            masterToast.setText("Could not fetch user");
                            masterToast.show();
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
                                            Integer.parseInt(participants), password, Integer.parseInt(classSpec)), startingBudget
                                            );
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
                            String password, int classSpec){
        //Create the room inside the database
        String key = mDatabase.child("rooms").push().getKey();
        RoomType room = new RoomType(name, type, budget, startBudget, conference, participants, password, getUid(), classSpec);
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