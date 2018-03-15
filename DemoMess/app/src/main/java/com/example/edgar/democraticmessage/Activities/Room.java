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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
    private boolean delete = false;
    private MenuItem menuReq;

    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        //Retrieve the intent and set up different values to be used in the room activity
        Intent intent = getIntent();
        //Database root reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        //Message text view
        messageBody = findViewById(R.id.input);
        //Roomkey of selected room
        roomData = intent.getStringExtra("RoomKey");
        //Database reference string to the associated messages
        roomKey = "/Message/" + roomData + "/";
        //List view display for all messages inside the room
        mMessageRecycler = findViewById(R.id.messView);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        //Start and bind the data service to the room
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
            DatabaseReference notifInfo = FirebaseDatabase.getInstance().getReference().child("participants").child(roomData).child(getUid());
            notifInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Participant part = dataSnapshot.getValue(Participant.class);

                    if(part.userRequest != null){
                        Log.d("Room","There is a request");
                        showRequest();
                        menuReq.setVisible(true);
                    }
                    else{
                        menuReq.setVisible(false);
                        Log.d("Room", "No requests");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    private void showRequest(){
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));
        ((TextView) layout.findViewById(R.id.title)).setText(R.string.requestTitle);
        ((TextView) layout.findViewById(R.id.message)).setText(R.string.requestMess);

        final PopupWindow pw = new PopupWindow(layout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);

        pw.showAtLocation(layout,  Gravity.END , 0, 0);

        final DatabaseReference reqView = FirebaseDatabase.getInstance().getReference().child("participants").child(roomData).child(getUid());

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reqView.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        goRequest();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                pw.dismiss();
            }
        });
    }

    private void goRequest(){
        Intent intent = new Intent(this,Request.class);
        startActivity(intent);
    }

    private void getUserInfo(final boolean set){
        String userId = getUid();
        final DatabaseReference roomInfo = FirebaseDatabase.getInstance().getReference().child("participants").child(roomData).child(userId);
        roomInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                if(set){
                    dataService.setBudget(part.budget);
                }
                else {
                    roomInfo.child("budget").setValue(dataService.getBudget());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        roomInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                if(set){
                    dataService.setTimeUsed(part.timeUsed);
                    dataService.setRoomKey(roomData);
                }
                else {
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

            if(!delete) getUserInfo(false);
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
                if(tempRoom != null)change(tempRoom.currentParticipants -1, roomData);
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


    private class MessageViewHolder extends RecyclerView.ViewHolder {

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

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

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

                            if(!dataService.getEmpty()){
                            //Word counting function, from stack overflow
                            String trimmed = message.trim();
                            final int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
                            Toast.makeText(Room.this, "Word count is " + words, Toast.LENGTH_SHORT).show();

                            if(dataService.getBudget() > 0){
                                dataService.setBudget(budgetCheck(dataService.getBudget(), words));
                                dataService.setTimeUsed(words);
                                getUserInfo(false);
                            }


                                String key = mDatabase.child(roomKey).push().getKey();

                                Message mess = new Message(user.username , message, words);
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
        menuReq = menu.findItem(R.id.infoReq);
        menuReq.setVisible(false);
        return true;
    }

    private void deleteRoom(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.alertDeleteTitle);
        builder.setMessage(R.string.alertDeleteMessage);
        builder.setPositiveButton(R.string.alertYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final DatabaseReference dataInfo = FirebaseDatabase.getInstance().getReference();
                dataInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        RoomType room = dataSnapshot.child("rooms").child(roomData).getValue(RoomType.class);

                        if(room.roomOwner.equals(getUid())){
                            Toast.makeText(Room.this,"Owner",Toast.LENGTH_SHORT).show();

                            if(room.currentParticipants <= 1){
                                dataSnapshot.child("rooms").child(roomData).getRef().removeValue();
                                dataSnapshot.child("Message").child(roomData).getRef().removeValue();
                                dataSnapshot.child("participants").child(roomData).getRef().removeValue();
                                delete = true;
                                finish();
                            }
                            else{
                                Toast.makeText(Room.this,"There are still people in the room",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(Room.this,"You are not the owner of the room",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }
        });

        builder.setNegativeButton(R.string.alertNo, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
            case R.id.infoDelete:
                deleteRoom();
                return true;
            case R.id.infoReq:
                goRequest();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}

