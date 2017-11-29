package com.example.edgar.democraticmessage.Activities;

import com.example.edgar.democraticmessage.Models.Message;
import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.Models.RoomType;
import com.example.edgar.democraticmessage.Models.User;
import com.example.edgar.democraticmessage.R;
import com.example.edgar.democraticmessage.Services.UserData;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room extends BaseActivity {

    private DatabaseReference mDatabase;
    private EditText messageBody;
    private String roomKey;
    private String roomData;
    private RecyclerView mMessageRecycler;
    private Room.MessageAdapter mAdapter;


    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent intent = getIntent();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        messageBody = findViewById(R.id.input);
        roomData = intent.getStringExtra("RoomKey");
        roomKey = "/Message/" + intent.getStringExtra("RoomKey") + "/";

        mMessageRecycler = findViewById(R.id.messView);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));

        //Start and bind the mp3 service to the activity
        startService(new Intent(this, UserData.class));
        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder
                service) {
            dataService = (UserData.DataBinder) service;
            Log.d("Service","Service start");
            getUserInfo(true);

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };


    private void getUserInfo(final boolean set){
        String userId = getUid();
        final DatabaseReference roomInfo = FirebaseDatabase.getInstance().getReference().child("participants").child(roomData).child(userId);
        roomInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                //Log.d("Budget", ""+part.budget);
                //Log.d("Used Budget", ""+part.timeUsed);
                if(set){
                    dataService.setBudget(part.budget);
                    dataService.setTimeUsed(part.timeUsed);
                }
                else {
                    roomInfo.child("budget").setValue(dataService.getBudget());
                    roomInfo.child("timeUsed").setValue(dataService.getTimeUsed());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();

        mAdapter = new MessageAdapter(this, mDatabase.getRef().child(roomKey));
        mMessageRecycler.setAdapter(mAdapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unbind the service when the activity is destroyed
        if(serviceConnection!=null) {
            getUserInfo(false);
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //Update current participants in the room
        DatabaseReference roomInfo = FirebaseDatabase.getInstance().getReference().child("rooms").child(roomData);
        roomInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RoomType tempRoom = dataSnapshot.getValue(RoomType.class);
                change(tempRoom.currentParticipants -1, roomData);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        // Clean up message listener
        mAdapter.cleanupListener();
    }

    private void change(int val, String key){
        DatabaseReference roomInfo = FirebaseDatabase.getInstance().getReference().child("rooms");
        roomInfo.child(key).child("currentParticipants").setValue(val);
    }

    private static class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private TextView mText;
        private TextView mCost;

        private MessageViewHolder(final View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.messageName);
            mText = itemView.findViewById(R.id.messageText);
            mCost = itemView.findViewById(R.id.messageCost);
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

        private Context mContext;
        private DatabaseReference mDatabaseReference;
        private ChildEventListener mChildEventListener;

        private List<String> mMessageIds = new ArrayList<>();
        private List<Message> mMessages = new ArrayList<>();

        private MessageAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new message has been added, add it to the displayed list
                    //DataSnapshot snap = dataSnapshot.child(dataSnapshot.getKey());

                    Message mMessage = dataSnapshot.getValue(Message.class);
                    //Log.d("Message user", mMessage.username);
                    //Log.d("Message text", mMessage.message);

                    // [START_EXCLUDE]
                    // Update RecyclerView
                    mMessageIds.add(dataSnapshot.getKey());
                    mMessages.add(mMessage);

                    Log.d("Message", "Added Message");
                    //Log.d("Message text")
                    notifyItemInserted(mMessages.size() - 1);
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so displayed the changed comment.
                    Message mMessage = dataSnapshot.getValue(Message.class);
                    String messageKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int messageIndex = mMessageIds.indexOf(messageKey);
                    if (messageIndex > -1) {
                        // Replace with the new data
                        mMessages.set(messageIndex, mMessage);

                        // Update the RecyclerView
                        notifyItemChanged(messageIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so remove it.
                    String messageKey = dataSnapshot.getKey();

                    // [START_EXCLUDE]
                    int messageIndex = mMessageIds.indexOf(messageKey);
                    if (messageIndex > -1) {
                        // Remove data from the list
                        mMessageIds.remove(messageIndex);
                        mMessages.remove(messageIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(messageIndex);
                    }
                    // [END_EXCLUDE]
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    // A message has changed position, use the key to determine if we are
                    // displaying this message and if so move it.
                    RoomType messageMoved = dataSnapshot.getValue(RoomType.class);
                    String messageKey = dataSnapshot.getKey();

                    // ...
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "Failed to load messages.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);
            // [END child_event_listener_recycler]

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            Message message = mMessages.get(position);
            holder.mName.setText(message.username);
            holder.mText.setText(message.message);
            holder.mCost.setText(Integer.toString(message.cost));
        }

        @Override
        public int getItemCount() {
            return mMessages.size();

        }

        private void cleanupListener() {
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
            }
        }
    }

    public void sendMessage(View v){

        final String message = messageBody.getText().toString();

        // Title is required
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "You need to input a message", Toast.LENGTH_SHORT).show();
            return;
        }


        final String userId = getUid();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        // [START_EXCLUDE]
                        if (user == null) {
                            // User is null, error out
                            Toast.makeText(Room.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Word counting function, from stack overflow
                            String trimmed = message.trim();
                            final int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
                            Toast.makeText(Room.this, "Word count is " + words, Toast.LENGTH_SHORT).show();

                            if(dataService.getBudget() > 0){
                                dataService.setBudget(budgetCheck(dataService.getBudget(), words));
                                dataService.setTimeUsed(words);
                                getUserInfo(false);
                            }

                            if(!dataService.getEmpty()){
                                String key = mDatabase.child(roomKey).push().getKey();

                                Message mess = new Message(user.username , message, 0);
                                Map<String, Object> sendMessage = mess.toMap();
                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put(roomKey + key, sendMessage);
                                mDatabase.updateChildren(childUpdates);
                                messageBody.setText("");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private int budgetCheck(int budget, int wordCost) {
        int newBudget = budget - wordCost;
        if(newBudget <= 0){
           dataService.setEmpty(true);
           return budget;
        }
        else{
            dataService.setEmpty(false);
            return newBudget;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.room_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();

        switch (i){
            case R.id.infoBudget:
                Toast.makeText(Room.this, "Current Budget is " + dataService.getBudget(), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.infoPart:
                Intent intent = new Intent(Room.this, StatisticScreen.class);
                intent.putExtra("RoomKey",roomData);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}

