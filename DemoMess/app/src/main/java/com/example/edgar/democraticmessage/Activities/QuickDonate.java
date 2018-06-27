package com.example.edgar.democraticmessage.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

public class QuickDonate extends BaseActivity{

    private String qUName;
    private String qUID;
    private Toast masterToast;
    private DatabaseReference data;
    private TextView qBud;
    private UserData.DataBinder dataService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_donate);

        Button back = findViewById(R.id.qBack);
        Button q1 = findViewById(R.id.q1);
        Button q5 = findViewById(R.id.q5);
        Button q10 = findViewById(R.id.q10);
        Button q20 = findViewById(R.id.q20);
        Button qCustom = findViewById(R.id.qCustom);
        qBud = findViewById(R.id.qBud);

        masterToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanup();
            }
        });

        q1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qDonate(1);
            }
        });

        q5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qDonate(5);
            }
        });

        q10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qDonate(10);
            }
        });

        q20.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qDonate(20);
            }
        });

        qCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goDonate();
                cleanup();
            }
        });

        //Bind user data service to the activity
        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        data = FirebaseDatabase.getInstance().getReference();

        Intent intent = getIntent();
        qUName = intent.getStringExtra("uName");
        qUID = intent.getStringExtra("uID");

        TextView quickTarget;
        quickTarget = findViewById(R.id.qDonTarget);
        quickTarget.setText(qUName);

        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        getWindow().setAttributes(wlp);

        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant partTarget = dataSnapshot.child("participants")
                        .child(dataService.getRoomKey())
                        .child(qUID)
                        .getValue(Participant.class);
                assert partTarget != null;
                qBud.setText(Integer.toString(partTarget.budget));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        //Connect the service to the activity and obtain user data
        @Override
        public void onServiceConnected(ComponentName name, IBinder
                service) {
            dataService = (UserData.DataBinder) service;

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    private void qDonate(int amount){
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

            int newBud = Integer.parseInt(qBud.getText().toString()) + amount;

            //Set the targets new budget
            data.child("participants").child(dataService.getRoomKey()).child(qUID).child("budget").setValue(newBud);

            dataService.addDonUsed();

            data.child("participants").child(dataService.getRoomKey()).child(getUid()).child("donUsed").setValue(dataService.getDonUsed());

        }
        cleanup();
    }

    private void goDonate(){
        Intent intent = new Intent(this, Donate.class);
        intent.putExtra("uID", qUID);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cleanup();
    }

    private void cleanup(){
        data.child("participants").child(dataService.getRoomKey()).child(qUID).child("userDonate").getRef().removeValue();
        finish();
    }
}
