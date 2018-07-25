package com.example.edgar.democraticmessage.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.R;
import com.example.edgar.democraticmessage.Services.UserData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Donate extends BaseActivity {

    private String dTarget;

    private TextView dUser;
    private TextView dUBud;

    private TextView dmBudget;
    private EditText dmGiveBudget;

    private DatabaseReference data;

    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        //Obtain targets uID
        Intent intent = getIntent();
        dTarget = intent.getStringExtra("uID");

        dUser = findViewById(R.id.donUser);
        dUBud = findViewById(R.id.donBudget);

        dmBudget = findViewById(R.id.donMyBudget);
        dmGiveBudget = findViewById(R.id.donGiveAmount);

        //Bind user data service to the activity
        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        //Obtain the participant data for the current user and the target
        data = FirebaseDatabase.getInstance().getReference();
        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant partMe = dataSnapshot.child("participants")
                        .child(dataService.getRoomKey())
                        .child(getUid())
                        .getValue(Participant.class);
                assert partMe != null;
                Participant partReq = dataSnapshot.child("participants")
                        .child(dataService.getRoomKey())
                        .child(dTarget)
                        .getValue(Participant.class);
                assert partReq != null;
                //Display targets username, their current budget and the users budget
                dUser.setText(partReq.username);
                dUBud.setText(Integer.toString(partReq.budget));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //Setup master toast
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        //Connect the service to the activity and obtain user data
        @Override
        public void onServiceConnected(ComponentName name, IBinder
                service) {
            dataService = (UserData.DataBinder) service;
            String budget = "" + dataService.getBudget();
            dmBudget.setText(budget);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        clearDonate();
    }

    public void send(View v){
        clickVibrate();
        final int amount = TextUtils.isEmpty(dmGiveBudget.getText().toString()) ? 0 : Integer.parseInt(dmGiveBudget.getText().toString()) ;
        if(amount <= 0){
            //If no amount is selected, warn the user
            masterToast.setText("Enter amount of talk to give!");
            masterToast.show();
        }
        else{
            int newBudget = dataService.getBudget() - amount;
            if( newBudget < 0){
                //Warn the user if they have selected to give more talk than they have
                masterToast.setText("You do not have that much talk!");
                masterToast.show();
            }
            else{
                dataService.setBudget(newBudget);
                //Set your new budget
                data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("budget").setValue(dataService.getBudget());

                int newBud = Integer.parseInt("" + dUBud.getText()) + amount;

                //Set the targets new budget
                data.child("participants").child(dataService.getRoomKey()).child(dTarget).child("budget").setValue(newBud);

                dataService.addDonUsed();

                data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("donUsed").setValue(dataService.getDonUsed());

                clearDonate();
            }
        }
    }

    public void cancel(View v){
        clickVibrate();
        clearDonate();
    }

    private void clearDonate(){
        //Remove donate flag and leave donate screen
        data.child("participants").child(dataService.getRoomKey()).child(dTarget).child("userDonate").getRef().removeValue();
        finish();
    }
}
