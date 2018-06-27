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
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room extends BaseActivity {

    private DatabaseReference mDatabase;
    private EditText messageBody;
    private String roomKey;
    private String roomData;
    private TextView budgetVal;
    private RecyclerView mMessageRecycler;
    private Room.MessageAdapter mAdapter;
    private TextView cCurrCost;
    private Date time;

    private boolean delete = false;
    private boolean donateLock = false;
    private boolean spendLock = false;
    private boolean reqViewLock = false;

    private UserData.DataBinder dataService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        TextView cUser = findViewById(R.id.currentUserID);
        cUser.setText(getUName());

        cCurrCost = findViewById(R.id.currentCost);
        TextView roomTitle = findViewById(R.id.roomName);

        //Retrieve the intent and set up different values to be used in the room activity
        Intent intent = getIntent();
        //Database root reference
        mDatabase = FirebaseDatabase.getInstance().getReference();
        //Message text view
        messageBody = findViewById(R.id.input);
        //Listener for text change
        messageBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Work out current cost of message and update the text view
                cCurrCost.setText(Integer.toString(countCost(charSequence.toString())));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //Room key of selected room
        roomData = intent.getStringExtra("RoomKey");
        String roomName = intent.getStringExtra("RoomName");
        roomTitle.setText(roomName);
        //Database reference string to the associated messages
        roomKey = "/Message/" + roomData + "/";
        //List view display for all messages inside the room
        mMessageRecycler = findViewById(R.id.messView);
        //Set layout to scroll to the latest message
        LinearLayoutManager layoutMan = new LinearLayoutManager(this);
        layoutMan.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(layoutMan);
        //Start and bind the data service to the room
        startService(new Intent(this, UserData.class));
        bindService(new Intent(this, UserData.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
        //Initialise master Toast for the activity
        masterToast= Toast.makeText(this, "", Toast.LENGTH_SHORT);
        //Get textview to display the current budget
        budgetVal = findViewById(R.id.budgetVal);

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
            //Connect to the database and obtain the budget and room types
            mDatabase.child("rooms").child(roomData).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    RoomType room = dataSnapshot.getValue(RoomType.class);
                    assert room != null;
                    //Update the user service
                    dataService.setBudgetType(room.budgettype);
                    dataService.setRoomType(room.conferencetype);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //Update the data inside the service
            getUserInfo(true);

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    public void goRequestView(View v){
        if(!reqViewLock){
            clickVibrate();
            Intent intent = new Intent(Room.this, RequestView.class);
            intent.putExtra("RoomKey",roomData);
            reqViewLock = true;
            startActivity(intent);
        }
    }

    private void getUserInfo(final boolean set){
        //Update data inside the service or database
        //True for service update
        //False for database update
        String userId = getUid();
        final DatabaseReference roomInfo = mDatabase.child("participants").child(roomData).child(userId);
        //Add listener that updates every time a change happens inside the participant table
        roomInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                //Make sure the activity is not being shut down before starting updates
                if(!delete){
                    if(set){
                        assert part != null;
                        dataService.setBudget(part.budget);
                        budgetVal.setText(Integer.toString(dataService.getBudget()));
                        //If the user obtains more budget from a request, update the service to say that they still have budget left
                        if(dataService.getBudget() > 0){
                            dataService.setEmpty(false);
                        }
                    }
                    else {
                        //Update budget value in database
                        if(!reqViewLock){
                            roomInfo.child("budget").setValue(dataService.getBudget());
                            Log.d("Room","Database update for budget");
                            reqViewLock = true;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        //Add listener that only updates on every function call
        roomInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Participant part = dataSnapshot.getValue(Participant.class);
                if(set){
                    assert part != null;
                    dataService.setTimeUsed(part.timeUsed);
                    if(part.reqUsed != 0)
                        dataService.setReqUsed(part.reqUsed);

                    if(part.donUsed != 0)
                        dataService.setDonUsed(part.donUsed);
                }
                else {
                    if(!delete)roomInfo.child("timeUsed").setValue(dataService.getTimeUsed());
                    Log.d("Time Used", "UserInf !set");
                    Log.d("Time Use Val", "" + dataService.getTimeUsed());
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
            if(!delete) {
                reqViewLock = false;
                getUserInfo(false);
            }
            unbindService(serviceConnection);
            serviceConnection = null;
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up message listener
        mAdapter.cleanupListener();
        //Update database
        getUserInfo(false);
    }

    private void goRequest(String target, String mRef){
        Intent intent = new Intent(this,Request.class);
        intent.putExtra("Target",target);
        intent.putExtra("mRef",mRef);
        startActivity(intent);
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView mName;
        private final TextView mUID;
        private final TextView mText;
        private final TextView mCost;
        private final TextView mBud;
        private final TextView mTime;
        private final View mContain;
        private final TextView mBudTitle;
        private final TextView mprivMess;
        private final TextView mRef;

        private MessageViewHolder(final View itemView) {
            super(itemView);
            mContain = itemView.findViewById(R.id.messageCont);
            mName = itemView.findViewById(R.id.messageName);
            mUID = itemView.findViewById(R.id.messageUID);
            mText = itemView.findViewById(R.id.messageText);
            mCost = itemView.findViewById(R.id.messageCost);
            mBud = itemView.findViewById(R.id.messageBudgetVal);
            mTime = itemView.findViewById(R.id.messageTime);
            mBudTitle = itemView.findViewById(R.id.messageBudget);
            mprivMess = itemView.findViewById(R.id.messageType);
            mRef = itemView.findViewById(R.id.messageRef);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mprivMess.getText().toString().equals("0")){
                        if (!donateLock) {
                            if(mUID.getText().toString().equals(getUid())){
                                masterToast.setText("You cannot donate budget to yourself!");
                                masterToast.show();
                            }
                            else quickDon(mName.getText().toString(), mUID.getText().toString());
                        }
                    }
                    else if(mprivMess.getText().toString().equals("1")){
                        if(mName.getText().toString().equals(getUid())){
                            goRequest(mUID.getText().toString(), mRef.getText().toString());
                        }
                    }
                }
            });
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

        private final Context mContext;
        private final DatabaseReference mDatabaseReference;
        private final ChildEventListener mChildEventListener;

        private final List<String> mMessageIds = new ArrayList<>();
        private final List<Message> mMessages = new ArrayList<>();
        //Setup notification sound
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

        private MessageAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    // A new message has been added, add it to the displayed list
                    Message mMessage = dataSnapshot.getValue(Message.class);
                    switch (mMessage.privMess){
                     case 0:
                         mMessageIds.add(dataSnapshot.getKey());
                         mMessages.add(mMessage);

                         notifyItemInserted(mMessages.size() - 1);
                         //Scroll to new message inserted
                         mMessageRecycler.scrollToPosition(mMessages.size()-1);
                         //Play notification sound
                         r.play();
                         break;
                     case 1:
                         if(mMessage.username.equals(getUid()) || mMessage.uID.equals(getUid())){
                             mMessageIds.add(dataSnapshot.getKey());
                             mMessages.add(mMessage);

                             notifyItemInserted(mMessages.size() - 1);
                             //Scroll to new message inserted
                             mMessageRecycler.scrollToPosition(mMessages.size()-1);
                             //Play notification sound
                             r.play();
                         }
                         break;
                     case 2:
                         if(mMessage.username.equals(getUid()) || mMessage.uID.equals(getUid())){
                             mMessageIds.add(dataSnapshot.getKey());
                             mMessages.add(mMessage);

                             notifyItemInserted(mMessages.size() - 1);
                             //Scroll to new message inserted
                             mMessageRecycler.scrollToPosition(mMessages.size()-1);
                             //Play notification sound
                             r.play();
                         }
                         break;
                     case 3:
                         if(mMessage.username.equals(getUid()) || mMessage.uID.equals(getUid())){
                             mMessageIds.add(dataSnapshot.getKey());
                             mMessages.add(mMessage);

                             notifyItemInserted(mMessages.size() - 1);
                             //Scroll to new message inserted
                             mMessageRecycler.scrollToPosition(mMessages.size()-1);
                             //Play notification sound
                             r.play();
                         }
                         break;
                     case 4:
                         break;
                     default:
                         break;
                    }

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
            holder.mUID.setText(message.uID);
            holder.mText.setText(message.message);
            holder.mCost.setText(Integer.toString(message.cost));
            holder.mBud.setText(Integer.toString(message.sendBudget));
            holder.mTime.setText(message.timeSent);
            holder.mprivMess.setText(Integer.toString(message.privMess));
            holder.mRef.setText(mMessageIds.get(position));
            String target = "";
            String user = "";
            if(message.privMess != 0){
                //String placeholder
                target = dataService.idToName(message.username);//Target
                user = dataService.idToName(message.uID);//User
            }
            //Highlight the users messages
            switch (message.privMess){
                case 0:
                    if(message.username.equals(getUName())){
                        //Cyan for current user
                        holder.mContain.setBackgroundColor(Color.CYAN);
                    }
                    else{
                        //White for other users
                        holder.mContain.setBackgroundColor(Color.WHITE);
                    }
                    break;
                case 1:
                    //Yellow for pending request
                    holder.mContain.setBackgroundColor(Color.YELLOW);
                    if(message.uID.equals(getUid())){
                        holder.mText.setText("Request sent to " + target);
                    }

                    //Hide unneeded information on a request
                    holder.mName.setVisibility(View.GONE);
                    holder.mBud.setVisibility(View.GONE);
                    holder.mCost.setVisibility(View.GONE);
                    holder.mBudTitle.setVisibility(View.GONE);
                    break;
                case 2:
                    //Green for accepted request
                    holder.mContain.setBackgroundColor(Color.GREEN);
                    if(message.uID.equals(getUid())){
                        holder.mText.setText("Request was accepted by " + target);
                    }
                    else {
                        holder.mText.setText("Request accepted from " + user);
                    }
                    //Hide unneeded information on a request
                    holder.mName.setVisibility(View.GONE);
                    holder.mBud.setVisibility(View.GONE);
                    holder.mCost.setVisibility(View.GONE);
                    holder.mBudTitle.setVisibility(View.GONE);
                    break;
                case 3:
                    //Red for declined request
                    holder.mContain.setBackgroundColor(Color.RED);
                    //Hide unneeded information on a request
                    if(message.uID.equals(getUid())){
                        holder.mText.setText("Request was declined by " + target);
                    }
                    else holder.mText.setText("Request declined from " + user);
                    holder.mName.setVisibility(View.GONE);
                    holder.mBud.setVisibility(View.GONE);
                    holder.mCost.setVisibility(View.GONE);
                    holder.mBudTitle.setVisibility(View.GONE);
                    break;
                default:
                    //Hide all others
                    holder.mContain.setVisibility(View.GONE);
                    break;
            }
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

    private void quickDon(final String uName, final String uID){
        final DatabaseReference roomUser = mDatabase.child("participants").child(dataService.getRoomKey()).child(uID);
        roomUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("userDonate").exists()){
                    masterToast.setText("Donation in progress!");
                    masterToast.show();
                }
                else{
                    roomUser.child("userDonate").setValue("Donate");
                    masterToast.setText("Donation starting");
                    masterToast.show();
                    goQuickDonate(uName, uID);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void goQuickDonate(String uName, String uID){
        Intent intent = new Intent(this,QuickDonate.class);
        intent.putExtra("uName",uName);
        intent.putExtra("uID",uID);
        donateLock = true;
        startActivity(intent);
    }

    public void viewSpenders(View v){
        if (!spendLock){
            Intent intent = new Intent(Room.this, SpenderList.class);
            intent.putExtra("RoomKey",roomData);
            spendLock = true;
            startActivity(intent);
        }
    }

    private int countCost(String input){
        int cost = 0;
        String trimmed = input.toString().trim();
        switch (dataService.getBudgetType()){
            case "Word":
                cost = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
                break;
            case "Character":
                cost = trimmed.isEmpty() ? 0 : trimmed.replaceAll("\\s+","").length();
                break;
            default:
                break;
        }

        return cost;
    }

    public void sendMessage(@SuppressWarnings("unused") View v){

        clickVibrate();

        final String message = messageBody.getText().toString();
        // Make sure there is a message to send
        if (TextUtils.isEmpty(message)) {
            masterToast.setText("You need to input a message");
            masterToast.show();
            return;
        }

        final String userId = getUid();
        //Set listener for the message table
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        User user = dataSnapshot.getValue(User.class);

                        if (user == null) {
                            // User is null, error out
                            masterToast.setText("Could not fetch user");
                            masterToast.show();
                        } else {
                            //Check to see if the user has any budget left
                            //If the user has budget left, count the length of the message based on the talk type of the room
                            if(!dataService.getEmpty()){
                                int budgetCost = countCost(message);

                            //Get to see if the user has any budget left
                            if(dataService.getBudget() > 0){
                                //IF the new message puts the user over their budget limit, do not send the message and warn the user
                                if(budgetCheck(dataService.getBudget(), budgetCost) == -1){
                                    masterToast.setText("This message costs too much to send");
                                    masterToast.show();
                                }
                                else{
                                    //If its not over limit, update the user service and the database
                                    dataService.setBudget(budgetCheck(dataService.getBudget(), budgetCost));
                                    dataService.addTimeUsed(budgetCost);
                                    Log.d("Time Used", "Message");
                                    reqViewLock = false;
                                    getUserInfo(false);

                                    //Get current time
                                    time = new Date();

                                    //Send message to database
                                    String key = mDatabase.child(roomKey).push().getKey();
                                    Message mess = new Message(user.username , message, budgetCost, dataService.getBudget(), time.toString(),getUid(),0);
                                    Map<String, Object> sendMessage = mess.toMap();
                                    Map<String, Object> childUpdates = new HashMap<>();
                                    childUpdates.put(roomKey + key, sendMessage);
                                    mDatabase.updateChildren(childUpdates);
                                    messageBody.setText("");

                                    }
                                }
                                else{
                                //Else warn the user they have run out of budget
                                masterToast.setText("You have run out of budget!");
                                masterToast.show();
                                }
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
        //Warn the user of their new budget
        budgetVal.setText(Integer.toString(newBudget));
        masterToast.setText("New Budget is " + newBudget);
        masterToast.show();
        //Return -1 if the new budget is 0 or less
        if(newBudget < 0){
           dataService.setEmpty(true);
           return -1;
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

    private void deleteRoom(){
        //Set up alert box for deleting the room
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.alertDeleteTitle);
        builder.setMessage(R.string.alertDeleteMessage);
        builder.setPositiveButton(R.string.alertYes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                clickVibrate();
               mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        RoomType room = dataSnapshot.child("rooms").child(roomData).getValue(RoomType.class);
                        //Check to see if the user is the room owner
                        assert room != null;
                        if(room.roomOwner.equals(getUid())){
                            //Check to see if the room is empty
                            if(room.currentParticipants <= 1){
                                //If both conditions hold, delete the room and associated entries in the database and leave the room
                                delete = true;
                                dataSnapshot.child("rooms").child(roomData).getRef().removeValue();
                                dataSnapshot.child("Message").child(roomData).getRef().removeValue();
                                dataSnapshot.child("participants").child(roomData).getRef().removeValue();
                                masterToast.setText("Deleting room");
                                masterToast.show();
                                finish();


                            }
                            else{
                                //Warn the user that other users are still part of the current room
                                masterToast.setText("There are still people in the room");
                                masterToast.show();
                            }
                        }
                        else {
                            //Warn the user that they do not have permission to delete the room
                            masterToast.setText("You are not the owner of the room");
                            masterToast.show();
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
                clickVibrate();
                //If No is pressed, return toe the activity and carry on as normal
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
                clickVibrate();
                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Set the data service to remove the users data and then remove the user from the participant list
                        RoomType tempRoom = dataSnapshot.child("rooms").child(roomData).getValue(RoomType.class);
                        DatabaseReference roomInfo = mDatabase.child("rooms");
                        assert tempRoom != null;
                        roomInfo.child(roomData).child("currentParticipants").setValue(tempRoom.currentParticipants -1);
                        leave();
                        mDatabase.child("participants").child(roomData).child(getUid()).getRef().removeValue();

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
                clickVibrate();
                //If No is pressed, return toe the activity and carry on as normal
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void leave(){
        //Set the flag in order not to update the user service
        delete = true;
        //Warn the user they are leaving the room
        masterToast.setText("Leaving room");
        masterToast.show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Time Used", "OnResume");
        //Update the user service
        getUserInfo(true);
        donateLock = false;
        spendLock = false;
        reqViewLock = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        Intent intent;
        //Set actions for the user menu
        switch (i){
            case R.id.infoPart:
                intent = new Intent(Room.this, StatisticScreen.class);
                intent.putExtra("RoomKey",roomData);
                clickVibrate();
                startActivity(intent);
                return true;
            case R.id.infoLeave:
                clickVibrate();
                leaveRoom();
                return true;
            case R.id.infoDelete:
                clickVibrate();
                deleteRoom();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
