package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.edgar.democraticmessage.Models.Message;
import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StatisticScreen extends BaseActivity {
    private DatabaseReference users;
    private RecyclerView userRecycler;
    private UserListAdapter userAdapter;
    private String roomKey;

    private final List<String> requestIds = new ArrayList<>();
    private final List<Message> request = new ArrayList<>();

    private DatabaseReference mainData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic_screen);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("RoomKey");

        mainData = FirebaseDatabase.getInstance().getReference();
        users = mainData.child("participants").child(roomKey);

        userRecycler = findViewById(R.id.statUser);
        userRecycler.setLayoutManager(new LinearLayoutManager(this));
        //Initialise master Toast for the activity
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);

        DatabaseReference reqMessList = mainData.child("Message").child(roomKey);
        reqMessList.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                Log.d("StatScreen","" + message.privMess);
                assert message != null;
                if(message.privMess == 1){
                    if(message.uID.equals(getUid())){
                        requestIds.add(dataSnapshot.getKey());
                        request.add(message);
                    }
                }

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


    @Override
    public void onStart(){
        super.onStart();
        //Create and attach adapter to the statistic view
        userAdapter = new UserListAdapter(this, users);
        userRecycler.setAdapter(userAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        userAdapter.cleanupListener();
    }

    private class UserListHolder extends RecyclerView.ViewHolder {

        private final TextView uName;
        private final TextView uBudget;
        private final TextView userID;
        private final Button butReq, butDon;

        private String uID;

        private UserListHolder(final View itemView) {
            super(itemView);

            uName = itemView.findViewById(R.id.userName);
            uBudget = itemView.findViewById(R.id.userBudgetVal);
            userID = itemView.findViewById(R.id.userID);
            butDon = itemView.findViewById(R.id.buttDon);
            butReq = itemView.findViewById(R.id.buttReq);

            butReq.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickVibrate();
                    if(uID.equals(getUid())){
                        masterToast.setText("You cannot send a request to yourself!");
                        masterToast.show();
                    }
                    else request(uID);
                }
            });

            butDon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickVibrate();
                    if(uID.equals(getUid())){
                        masterToast.setText("You cannot donate budget to yourself!");
                        masterToast.show();
                    }
                    else donate(uID);
                }
            });
        }
    }

    private class UserListAdapter extends RecyclerView.Adapter<UserListHolder> {

        private final Context mContext;
        private final DatabaseReference mDatabaseReference;
        private final ChildEventListener mChildEventListener;

        private final List<String> mUserIDs = new ArrayList<>();
        private final List<Participant> mUsers = new ArrayList<>();

        private UserListAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new participant has been added, add it to the displayed list
                    Participant part = dataSnapshot.getValue(Participant.class);
                    mUserIDs.add(dataSnapshot.getKey());
                    mUsers.add(part);

                    notifyItemInserted(mUsers.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Participant newPart = dataSnapshot.getValue(Participant.class);
                    //If a user changes, update their entry
                    String commentKey = dataSnapshot.getKey();
                    int userIndex = mUserIDs.indexOf(commentKey);
                    if (userIndex > -1) {
                        mUsers.set(userIndex, newPart);
                        notifyItemChanged(userIndex);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    String userKey = dataSnapshot.getKey();
                    //If a user is removed from the participant list, stop displaying them
                    int userIndex = mUserIDs.indexOf(userKey);
                    if (userIndex > -1) {
                        mUserIDs.remove(userIndex);
                        mUsers.remove(userIndex);
                        notifyItemRemoved(userIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            ref.addChildEventListener(childEventListener);
            //Store reference for deletion
            mChildEventListener = childEventListener;
        }

        @NonNull
        @Override
        public UserListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.user_thumb, parent, false);
            return new UserListHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserListHolder holder, int position) {
            //Link the participant data to the holder variables
            Participant part = mUsers.get(position);
            holder.uName.setText(part.username);
            holder.uBudget.setText(Integer.toString(part.budget));
            holder.userID.setText(part.uID);
            holder.uID = part.uID;
            if(part.username.equals(getUName())){
                holder.butDon.setVisibility(View.GONE);
                holder.butReq.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mUsers.size();

        }

        private void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }

    private void request(final String uid){
        String target = uid;
        Date time = new Date();

        Log.d("StatScreen","Request size " + request.size());

        //Update previous request to ignored (flag 4)
        if(request.size() > 0) {
            for(int i = 0; i < request.size(); i++){
                if(request.get(i).username.equals(uid)){
                    mainData.child("Message").child(roomKey).child(requestIds.get(i)).child("privMess").setValue(4);
                }
            }
        }
        //Insert new request
        String key = mainData.child("/Message/" + roomKey + "/").push().getKey();
        String reqMess = "Request from " + getUName();
        Message mess = new Message(target, reqMess, 0, 0, time.toString(), getUid(), 1);
        Map<String, Object> sendMessage = mess.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/Message/" + roomKey + "/" + key, sendMessage);
        mainData.updateChildren(childUpdates);
    }

    private void donate(String uid){
        final DatabaseReference roomUser = mainData.child("participants").child(roomKey).child(uid);
        final String tUID = uid;
        roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("userDonate").exists()){
                    masterToast.setText("Donation in progress!");
                    masterToast.show();
                }
                else{
                    roomUser.child("userDonate").setValue("Donate");
                    masterToast.setText("Request sent");
                    masterToast.show();
                    goToDonate(tUID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void goToDonate(String uid){
        Intent intent = new Intent(this, Donate.class);
        intent.putExtra("uID", uid);
        startActivity(intent);
    }
}