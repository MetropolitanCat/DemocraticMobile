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
import android.widget.TextView;
import android.widget.Toast;

import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import java.util.List;


public class StatisticScreen extends BaseActivity {
    private DatabaseReference users;
    private RecyclerView userRecycler;
    private UserListAdapter userAdapter;
    private String roomKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic_screen);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("RoomKey");

        DatabaseReference mainData = FirebaseDatabase.getInstance().getReference();
        users = mainData.child("participants").child(roomKey);

        userRecycler = findViewById(R.id.statUser);
        userRecycler.setLayoutManager(new LinearLayoutManager(this));
    }


    @Override
    public void onStart(){
        super.onStart();
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
        private String userName;
        private String uID;
        private String uBudgetUsed;

        private UserListHolder(final View itemView) {
            super(itemView);

            uName = itemView.findViewById(R.id.userName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(),StatisticsUser.class);
                    intent.putExtra("userName", userName);
                    intent.putExtra("userID",uID);
                    intent.putExtra("RoomId", roomKey);
                    intent.putExtra("BudgetUsed",uBudgetUsed);
                    startActivity(intent);
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

                    Log.d("Room","Added Room");
                    notifyItemInserted(mUsers.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Participant newPart = dataSnapshot.getValue(Participant.class);
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
                    Toast.makeText(mContext, "Failed to load users.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);

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
            holder.userName = part.username;
            holder.uID = part.uID;
            holder.uBudgetUsed = Integer.toString(part.timeUsed);
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
}
