package com.example.edgar.democraticmessage.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
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
    private String target;
    private TextView reqTalk;
    private String reqTalkAmount = "";
    private TextView reqMyTalk;
    private EditText reqGiveTalk;
    private DatabaseReference data;
    private String reqUser = "";
    private String mRef;

    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        Intent intent = getIntent();
        target = intent.getStringExtra("Target");
        mRef = intent.getStringExtra("mRef");

        reqRequestee = findViewById(R.id.reqFrom);
        reqTalk = findViewById(R.id.reqTalkFrom);
        reqMyTalk = findViewById(R.id.reqMyTalk);
        reqGiveTalk = findViewById(R.id.reqGiveAmount);
        //Bind user data service to the activity
        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
        //Obtain the participant data for the current user and the request target
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
                                        .child(target)
                                        .getValue(Participant.class);
               assert partReq != null;
               reqRequestee.setText(partReq.username);
               reqTalkAmount = "" + partReq.budget;
                if(dataService.getRoomType().equals("Blind Man")){
                    reqTalk.setText("?????");
                }
                else{
                    reqTalk.setText(reqTalkAmount);
                }
                reqUser = target;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //Initialise master Toast for the activity
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        //Connect the service to the activity and obtain user data
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

    public void reqDecline(@SuppressWarnings("unused") View v){
        //Remove request from the user
        deleteFlag(3);
        clickVibrate();
        finish();
    }

    public void reqAccept(@SuppressWarnings("unused") View v){
        //Accept the request
        clickVibrate();
        int amount =  TextUtils.isEmpty(reqGiveTalk.getText().toString()) ? 0 : Integer.parseInt(reqGiveTalk.getText().toString());
        Log.d("Req","" + amount);
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
                masterToast.setText("Finishing Request");
                masterToast.show();

                dataService.setBudget(newBudget);
                dataService.addReqUsed();

                deleteFlag(2);

                data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("reqUsed").setValue(dataService.getReqUsed());

                int reqGive = Integer.parseInt(reqTalkAmount) + amount;
                //Set the requestees new budget
                data.child("participants").child(dataService.getRoomKey()).child(reqUser).child("budget").setValue(reqGive);
                Log.d("Request", "Set target budget");

                //Set your new budget
                data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("budget").setValue(dataService.getBudget());
                Log.d("Request", "Set new budget");

                finish();
            }
        }

    }

    private void deleteFlag(int flag){
        //Alter request message based on result
        int flagVal = 1;
        switch (flag){
            case 2:
                flagVal = 2;
                break;
            case 3:
                flagVal = 3;
                break;
            case 4:
                flagVal = 4;
                break;
        }
        data.child("Message").child(dataService.getRoomKey()).child(mRef).child("privMess").setValue(flagVal);
        Log.d("Request", "Remove flag");
    }

}
