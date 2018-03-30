package com.example.edgar.democraticmessage.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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

public class Request extends BaseActivity {

    private TextView reqRequestee;
    private TextView reqTalk;
    private String reqTalkAmount = "";
    private TextView reqMyTalk;
    private EditText reqGiveTalk;
    private DatabaseReference data;

    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        reqRequestee = findViewById(R.id.reqRequestee);
        reqTalk = findViewById(R.id.reqTalk);
        reqMyTalk = findViewById(R.id.reqMyTalk);
        reqGiveTalk = findViewById(R.id.reqGiveAmount);

        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        data = FirebaseDatabase.getInstance().getReference();
        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
               Participant partMe = dataSnapshot.child("participants")
                                    .child(dataService.getRoomKey())
                                    .child(getUid())
                                    .getValue(Participant.class);
               Participant partReq = dataSnapshot.child("participants")
                                        .child(dataService.getRoomKey())
                                        .child(partMe.userRequest)
                                        .getValue(Participant.class);
               reqRequestee.setText(partReq.username);
               reqTalkAmount = "" + partReq.budget;
                if(dataService.getRoomType().equals("Blind Man")){
                    reqTalk.setText("?????");
                }
                else{
                    reqTalk.setText(reqTalkAmount);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder
                service) {
            dataService = (UserData.DataBinder) service;
            String talk = "" + dataService.getBudget();
            reqMyTalk.setText(talk);
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

    public void reqDecline(View v){
        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("userRequest").getRef().removeValue();
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void reqAccept(View v){
        final int amount = reqGiveTalk.getText() != null ? Integer.parseInt(reqGiveTalk.getText().toString()) : 0;
        Log.d("Req","" + amount);
        if(amount <= 0){
            Toast.makeText(getApplicationContext(),"Enter amount of talk to give!",Toast.LENGTH_SHORT).show();
        }
        else{
            int newBudget = dataService.getBudget() - amount;
            if( newBudget < 0){
                Toast.makeText(getApplicationContext(),"You do not have that much talk!",Toast.LENGTH_SHORT).show();
            }
            else{
                dataService.setBudget(newBudget);

                data.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Participant partMe = dataSnapshot.child("participants")
                                .child(dataService.getRoomKey())
                                .child(getUid())
                                .getValue(Participant.class);

                        //Set your new budget
                        data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("budget").setValue(dataService.getBudget());
                        //Remove the request
                        data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("userRequest").getRef().removeValue();
                        //Set the requestees new budget
                        int reqGive = Integer.parseInt(reqTalkAmount) + amount;
                        data.child("participants").child(dataService.getRoomKey()).child(partMe.userRequest).child("budget").setValue(reqGive);
                        finish();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });


            }
        }
    }
}
