package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.Models.RoomType;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private DatabaseReference mRooms;
    private RecyclerView mCommentsRecycler;
    private RoomTypeAdapter mAdapter;
    private static String userName;
    private static String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mRooms = FirebaseDatabase.getInstance().getReference().child("rooms");

        mCommentsRecycler = findViewById(R.id.recyclerView);
        mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

        userName = getUName();
        userId = getUid();
    }

    @Override
    public void onStart(){
        super.onStart();

        mAdapter = new RoomTypeAdapter(this, mRooms);
        mCommentsRecycler.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up room listener
        mAdapter.cleanupListener();
    }

    private class RoomTypeViewHolder extends RecyclerView.ViewHolder {

        private TextView rName;
        private TextView rType;
        private TextView rKey;
        private int maxPart;
        private int currentPart;
        private int budgetShare;

        private RoomTypeViewHolder(final View itemView) {
            super(itemView);

            rName = itemView.findViewById(R.id.roomName);
            rType = itemView.findViewById(R.id.roomType);
            rKey = itemView.findViewById(R.id.roomKey);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Check if room is full
                    if(currentPart > (maxPart -1)){
                        Toast.makeText(getApplicationContext(), "Room is full!!!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference staticRooms = FirebaseDatabase.getInstance().getReference().child("rooms");

                    currentPart +=1;

                    staticRooms.child("" + rKey.getText()).child("currentParticipants").setValue(currentPart);

                    final DatabaseReference newParticipant = FirebaseDatabase.getInstance().getReference();

                    DatabaseReference roomUser = newParticipant.child("participants").child("" + rKey.getText());

                    roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.child(userId).exists()){
                                roomJoin("" + rKey.getText());
                            }
                            else{
                                Participant part = new Participant(userName,userId, budgetShare);
                                Map<String, Object> sendMessage = part.toMap();
                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put( "/participants/" + "" + rKey.getText() + "/" + userId, sendMessage);

                                newParticipant.updateChildren(childUpdates);
                                roomJoin("" + rKey.getText());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            });
        }

        private void roomJoin(String roomKey){
            Intent intent = new Intent(getApplicationContext(), Room.class);
            intent.putExtra("RoomKey",roomKey);
            startActivity(intent);
        }
    }

    private class RoomTypeAdapter extends RecyclerView.Adapter<RoomTypeViewHolder> {

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mRoomIds = new ArrayList<>();
        private List<RoomType> mRooms = new ArrayList<>();

        private RoomTypeAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new room has been added, add it to the displayed list
                    RoomType tRoom = dataSnapshot.getValue(RoomType.class);

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mRoomIds.add(dataSnapshot.getKey());
                    mRooms.add(tRoom);

                    Log.d("Room","Added Room");
                    notifyItemInserted(mRooms.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A comment has changed, use the key to determine if we are displaying this
                    // comment and if so displayed the changed comment.
                    RoomType newRoom = dataSnapshot.getValue(RoomType.class);
                    String commentKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int commentIndex = mRoomIds.indexOf(commentKey);
                    if (commentIndex > -1) {
                        // Replace with the new data
                        mRooms.set(commentIndex, newRoom);

                        // Update the RecyclerView
                        notifyItemChanged(commentIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A room has changed, use the key to determine if we are displaying this
                    // room and if so remove it.
                    String roomKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int roomIndex = mRoomIds.indexOf(roomKey);
                    if (roomIndex > -1) {
                        // Remove data from the list
                        mRoomIds.remove(roomIndex);
                        mRooms.remove(roomIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(roomIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    // A room has changed position, use the key to determine if we are
                    // displaying this room and if so move it.
                    RoomType roomMoved = dataSnapshot.getValue(RoomType.class);
                    String roomKey = dataSnapshot.getKey();

                    // ...
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "Failed to load rooms.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public RoomTypeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.room_thumb, parent, false);
            return new RoomTypeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RoomTypeViewHolder holder, int position) {
            RoomType room = mRooms.get(position);
            holder.rName.setText(room.roomname);
            holder.rType.setText(room.conferencetype);
            holder.rKey.setText(mRoomIds.get(position));
            holder.maxPart = room.participants;
            holder.currentPart = room.currentParticipants;
            holder.budgetShare = room.startingBudget;
        }

        @Override
        public int getItemCount() {
            return mRooms.size();

        }

        private void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, UserSignInUp.class));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void createRoom(View v){
        Intent intent = new Intent(this, RoomCreate.class);

        startActivity(intent);
    }
}
