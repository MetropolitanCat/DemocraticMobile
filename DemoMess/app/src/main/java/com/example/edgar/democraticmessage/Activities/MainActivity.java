package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.support.annotation.NonNull;
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
    private RecyclerView mRoomView;
    private RoomTypeAdapter mRoomAdapter;
    private static String userName;
    private static String userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get database reference for the rooms table in the database
        mRooms = FirebaseDatabase.getInstance().getReference().child("rooms");
        //Setup view to display all current rooms
        mRoomView = findViewById(R.id.recyclerView);
        mRoomView.setLayoutManager(new LinearLayoutManager(this));

        userName = getUName();
        userId = getUid();
        //Initialize the master Toast for the activity
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    @Override
    public void onStart(){
        super.onStart();
        //Create and attach adapter to the room view
        mRoomAdapter = new RoomTypeAdapter(this, mRooms);
        mRoomView.setAdapter(mRoomAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up room listener
        mRoomAdapter.cleanupListener();
    }

    private class RoomTypeViewHolder extends RecyclerView.ViewHolder {

        private final TextView rName;
        private final TextView rType;
        private final TextView rKey;
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
                    final DatabaseReference newParticipant = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference roomUser = newParticipant.child("participants").child("" + rKey.getText());

                    roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //Check if user is in the participant list
                            if(dataSnapshot.child(userId).exists()){
                                //If they are, let the user join the room
                                roomJoin("" + rKey.getText(), "" + rName.getText());
                            }
                            else {
                                //If user is not in the participant list, check to see if the participant list is full
                                if (currentPart > (maxPart - 1)) {
                                    //IF the list is full, do not allow the user to join
                                    masterToast.setText("Room is full!!!");
                                    masterToast.show();
                                    return;
                                } else {
                                    //If the list is not full, add the user to the participant list and let them join the room
                                    currentPart += 1;
                                    mRooms.child("" + rKey.getText()).child("currentParticipants").setValue(currentPart);

                                    //Update participant list in the database
                                    Participant part = new Participant(userName, userId, budgetShare);
                                    Map<String, Object> sendMessage = part.toMap();
                                    Map<String, Object> childUpdates = new HashMap<>();
                                    childUpdates.put("/participants/" + "" + rKey.getText() + "/" + userId, sendMessage);
                                    newParticipant.updateChildren(childUpdates);
                                    masterToast.setText("Joining room");
                                    masterToast.show();
                                }
                                roomJoin("" + rKey.getText(), "" + rName.getText());
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {}
                    });
                }
            });
        }

        private void roomJoin(String roomKey, String roomName){
            //Join the selected room
            Intent intent = new Intent(getApplicationContext(), Room.class);
            //Send the roomkey to the Room activity
            intent.putExtra("RoomKey",roomKey);
            intent.putExtra("RoomName",roomName);
            clickVibrate();
            startActivity(intent);
        }
    }

    private class RoomTypeAdapter extends RecyclerView.Adapter<RoomTypeViewHolder> {

        private final Context mContext;
        private final DatabaseReference mDatabaseReference;
        private final ChildEventListener mChildEventListener;

        private final List<String> mRoomIds = new ArrayList<>();
        private final List<RoomType> mRooms = new ArrayList<>();

        private RoomTypeAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            //Create child listener in order to add rooms to the view
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new room has been added, add it to the displayed list
                    RoomType tRoom = dataSnapshot.getValue(RoomType.class);
                    //Add the new room to the view
                    mRoomIds.add(dataSnapshot.getKey());
                    mRooms.add(tRoom);
                    Log.d("Room","Added Room");
                    notifyItemInserted(mRooms.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    RoomType newRoom = dataSnapshot.getValue(RoomType.class);
                    String roomID = dataSnapshot.getKey();
                    //Make changes to the correct room in the view
                    int roomIndex = mRoomIds.indexOf(roomID);
                    if (roomIndex > -1) {
                        mRooms.set(roomIndex, newRoom);

                        notifyItemChanged(roomIndex);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Remove any rooms that have been deleted but are still being displayed
                    String roomKey = dataSnapshot.getKey();
                    int roomIndex = mRoomIds.indexOf(roomKey);
                    if (roomIndex > -1) {
                        // Remove data from the list
                        mRoomIds.remove(roomIndex);
                        mRooms.remove(roomIndex);

                        notifyItemRemoved(roomIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            ref.addChildEventListener(childEventListener);

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @NonNull
        @Override
        public RoomTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.room_thumb, parent, false);
            return new RoomTypeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RoomTypeViewHolder holder, int position) {
            //Bind the data obtained from the database to the holder
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
        //Create menu inside the activity
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Allow the user to logout
        int i = item.getItemId();
        if (i == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            clickVibrate();
            startActivity(new Intent(this, UserSignInUp.class));
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void createRoom(@SuppressWarnings("unused") View v){
        //Go to the RoomCreate activity
        Intent intent = new Intent(this, RoomCreate.class);
        clickVibrate();
        startActivity(intent);
    }
}
