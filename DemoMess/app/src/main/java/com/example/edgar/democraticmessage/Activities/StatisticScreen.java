package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
    private DatabaseReference mainData;
    private RecyclerView userRecycler;
    private UserListAdapter userAdapter;
    private String roomKey;


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
        // Clean up room listener
        userAdapter.cleanupListener();
    }

    private class UserListHolder extends RecyclerView.ViewHolder {

        private TextView uName;
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

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mUserIDs = new ArrayList<>();
        private List<Participant> mUsers = new ArrayList<>();

        private UserListAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new room has been added, add it to the displayed list
                    Participant part = dataSnapshot.getValue(Participant.class);

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mUserIDs.add(dataSnapshot.getKey());
                    mUsers.add(part);

                    Log.d("Room","Added Room");
                    notifyItemInserted(mUsers.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    Participant newPart = dataSnapshot.getValue(Participant.class);
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int userIndex = mUserIDs.indexOf(commentKey);
                    if (userIndex > -1) {
                        // Replace with the new data
                        mUsers.set(userIndex, newPart);

                        // Update the RecyclerView
                        notifyItemChanged(userIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A room has changed, use the key to determine if we are displaying this
                    // room and if so remove it.
                    String userKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int userIndex = mUserIDs.indexOf(userKey);
                    if (userIndex > -1) {
                        // Remove data from the list
                        mUserIDs.remove(userIndex);
                        mUsers.remove(userIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(userIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    // A room has changed position, use the key to determine if we are
                    // displaying this room and if so move it.
                    Participant userMoved = dataSnapshot.getValue(Participant.class);
                    String userKey = dataSnapshot.getKey();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "Failed to load users.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public UserListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.user_thumb, parent, false);
            return new UserListHolder(view);
        }

        @Override
        public void onBindViewHolder(UserListHolder holder, int position) {
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
