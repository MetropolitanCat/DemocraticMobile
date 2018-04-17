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
import android.support.annotation.NonNull;
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
            //Connect service to the activity
            dataService = (UserData.DataBinder) service;
            //Set up local data inside the service
            dataService.setRoomKey(roomData);
            mDatabase.child("rooms").child(roomData).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    RoomType room = dataSnapshot.getValue(RoomType.class);
                    assert room != null;
                    dataService.setBudgetType(room.budgettype);
                    dataService.setRoomType(room.conferencetype);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            Log.d("Service","" + dataService.getBudgetType());
            Log.d("Service","" + dataService.getRoomType());
            Log.d("Service","Service start");
            //Update the data inside the service
            getUserInfo(true);
            DatabaseReference notifInfo = mDatabase.child("participants").child(roomData).child(getUid());
            //Display request when one is available
            notifInfo.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Participant part = dataSnapshot.getValue(Participant.class);

                    assert part != null;
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
        //Show request on the activity
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));
        ((TextView) layout.findViewById(R.id.title)).setText(R.string.requestTitle);
        ((TextView) layout.findViewById(R.id.message)).setText(R.string.requestMess);

        final PopupWindow pw = new PopupWindow(layout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);

        pw.showAtLocation(layout,  Gravity.END , 0, 0);

        final DatabaseReference reqView = mDatabase.child("participants").child(roomData).child(getUid());
        //Go to request activity if the user taps on the request popup
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
        //Start request activity
        Intent intent = new Intent(this,Request.class);
        startActivity(intent);
    }

    private void getUserInfo(final boolean set){
        //Update data inside the service or database
        //True for service update
        //False for database update
        String userId = getUid();
        final DatabaseReference roomInfo = mDatabase.child("participants").child(roomData).child(userId);
        roomInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                if(!delete){
                    if(set){
                        assert part != null;
                        dataService.setBudget(part.budget);
                    }
                    else {
                        roomInfo.child("budget").setValue(dataService.getBudget());
                    }
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
                    assert part != null;
                    dataService.setTimeUsed(part.timeUsed);
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
        //Set adapter for the view to display messages
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
        // Clean up message listener
        mAdapter.cleanupListener();
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView mName;
        private final TextView mText;
        private final TextView mCost;

        private MessageViewHolder(final View itemView) {
            super(itemView);

            mName = itemView.findViewById(R.id.messageName);
            mText = itemView.findViewById(R.id.messageText);
            mCost = itemView.findViewById(R.id.messageCost);
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

        private final Context mContext;
        private final DatabaseReference mDatabaseReference;
        private final ChildEventListener mChildEventListener;

        private final List<String> mMessageIds = new ArrayList<>();
        private final List<Message> mMessages = new ArrayList<>();

        private MessageAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new message has been added, add it to the displayed list
                    Message mMessage = dataSnapshot.getValue(Message.class);
                    mMessageIds.add(dataSnapshot.getKey());
                    mMessages.add(mMessage);

                    Log.d("Message", "Added Message");
                    notifyItemInserted(mMessages.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so displayed the changed comment.
                    Message mMessage = dataSnapshot.getValue(Message.class);
                    String messageKey = dataSnapshot.getKey();

                    int messageIndex = mMessageIds.indexOf(messageKey);
                    if (messageIndex > -1) {
                        mMessages.set(messageIndex, mMessage);
                        notifyItemChanged(messageIndex);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so remove it.
                    String messageKey = dataSnapshot.getKey();

                    int messageIndex = mMessageIds.indexOf(messageKey);
                    if (messageIndex > -1) {
                        mMessageIds.remove(messageIndex);
                        mMessages.remove(messageIndex);
                        notifyItemRemoved(messageIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, "Failed to load messages.",
                            Toast.LENGTH_SHORT).show();
                }
            };
            ref.addChildEventListener(childEventListener);

            mChildEventListener = childEventListener;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            //Link data from the database into the holder elements
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

                        if (user == null) {
                            // User is null, error out
                            Toast.makeText(Room.this,
                                    "Error: could not fetch user.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //Check to see if the user has any budget left
                            //If the user has budget left, count the length of the message based on the talk type of the room
                            if(!dataService.getEmpty()){
                                int talkCount = 0;
                                String trimmed = message.trim();
                                switch (dataService.getBudgetType()){
                                    case "Word":
                                        talkCount = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
                                        break;
                                    case "Character":
                                        talkCount = trimmed.isEmpty() ? 0 : trimmed.replaceAll("\\s+","").length();
                                        break;
                                    default:
                                        break;
                                }
                            //Check to see if the new message puts the user over their talk budget limit
                            if(dataService.getBudget() > 0){
                                //If its not over limit, update the user service and the database
                                dataService.setBudget(budgetCheck(dataService.getBudget(), talkCount));
                                dataService.setTimeUsed(talkCount);
                                getUserInfo(false);
                            }
                            //Send message to database
                            String key = mDatabase.child(roomKey).push().getKey();
                            Message mess = new Message(user.username , message, talkCount);
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
        //Set up alert box for deleting the room
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.alertDeleteTitle);
        builder.setMessage(R.string.alertDeleteMessage);
        builder.setPositiveButton(R.string.alertYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
               mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        RoomType room = dataSnapshot.child("rooms").child(roomData).getValue(RoomType.class);
                        //Check to see if the user is the room owner
                        assert room != null;
                        if(room.roomOwner.equals(getUid())){
                            Toast.makeText(Room.this,"Owner",Toast.LENGTH_SHORT).show();
                            //Check to see if the room is empty
                            if(room.currentParticipants <= 1){
                                //If both conditions hold, delete the room and associated entries in the database and leave the room
                                delete = true;
                                dataSnapshot.child("rooms").child(roomData).getRef().removeValue();
                                dataSnapshot.child("Message").child(roomData).getRef().removeValue();
                                dataSnapshot.child("participants").child(roomData).getRef().removeValue();
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

    private void leaveRoom(){
        //Set up the alert box for leaving the room
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.alertLeaveTitle);
        builder.setMessage(R.string.alertLeaveMessage);
        builder.setPositiveButton(R.string.alertYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Set the data service to remove the users data and then remove the user from the participant list
                        RoomType tempRoom = dataSnapshot.child("rooms").child(roomData).getValue(RoomType.class);
                        delete = true;
                        DatabaseReference roomInfo = mDatabase.child("rooms");
                        assert tempRoom != null;
                        roomInfo.child(roomData).child("currentParticipants").setValue(tempRoom.currentParticipants -1);
                        dataSnapshot.child("participants").child(roomData).child(getUid()).getRef().removeValue();
                        finish();
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
            case R.id.infoLeave:
                leaveRoom();
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

