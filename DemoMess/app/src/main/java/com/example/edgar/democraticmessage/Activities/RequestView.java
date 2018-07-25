package com.example.edgar.democraticmessage.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.edgar.democraticmessage.Models.Message;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class RequestView extends BaseActivity {

    private String roomKey;
    private DatabaseReference data;
    private DatabaseReference messages;
    private RequestAdapter requestAdapter;
    private RecyclerView requestRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_view);
        Intent intent = getIntent();
        roomKey = intent.getStringExtra("RoomKey");

        data = FirebaseDatabase.getInstance().getReference();
        messages = data.child("Message").child(roomKey);

        requestRecycler = findViewById(R.id.requestView);
        requestRecycler.setLayoutManager(new LinearLayoutManager(this));

        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        getWindow().setAttributes(wlp);
    }
    @Override
    public void onStart(){
        super.onStart();
        //Set adapter for the view to display messages
        requestAdapter = new RequestAdapter(this, messages);
        requestRecycler.setAdapter(requestAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up message listener
        requestAdapter.cleanupListener();
    }

    private class RequestViewHolder extends RecyclerView.ViewHolder {

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

        private RequestViewHolder(final View itemView) {
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
                    goRequest(mUID.getText().toString(), mRef.getText().toString());
                }
            });
        }
    }

    private class RequestAdapter extends RecyclerView.Adapter<RequestViewHolder> {

        private final Context requestContext;
        private final DatabaseReference requestDatabaseReference;
        private final ChildEventListener requestChildEventListener;

        private final List<String> requestPartIds = new ArrayList<>();
        private final List<Message> requestPart = new ArrayList<>();


        private RequestAdapter(final Context context, DatabaseReference ref) {
            requestContext = context;
            requestDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Message message = dataSnapshot.getValue(Message.class);
                    assert message != null;
                    if(message.privMess == 1){
                        if(message.username.equals(getUid())){
                            requestPartIds.add(dataSnapshot.getKey());
                            requestPart.add(message);

                            notifyItemInserted(requestPart.size() - 1);
                        }

                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so displayed the changed comment.
                    Message message = dataSnapshot.getValue(Message.class);
                    String requestKey = dataSnapshot.getKey();

                    int requestIndex = requestPartIds.indexOf(requestKey);
                    if (requestIndex > -1) {
                        requestPart.set(requestIndex, message);
                        notifyItemChanged(requestIndex);
                    }

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so remove it.
                    String requestKey = dataSnapshot.getKey();

                    int requestIndex = requestPartIds.indexOf(requestKey);
                    if (requestIndex > -1) {
                        requestPartIds.remove(requestIndex);
                        requestPart.remove(requestIndex);
                        notifyItemRemoved(requestIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            ref.addChildEventListener(childEventListener);

            requestChildEventListener = childEventListener;
        }

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(requestContext);
            View view = inflater.inflate(R.layout.message, parent, false);
            return new RequestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
            //Link data from the database into the holder elements
            Message message = requestPart.get(position);
            holder.mName.setText(message.username);
            holder.mUID.setText(message.uID);
            holder.mText.setText(message.message);
            holder.mCost.setText(Integer.toString(message.cost));
            holder.mBud.setText(Integer.toString(message.sendBudget));
            holder.mTime.setText(message.timeSent);
            holder.mprivMess.setText(Integer.toString(message.privMess));
            holder.mRef.setText(requestPartIds.get(position));

            holder.mName.setVisibility(View.GONE);
            holder.mBud.setVisibility(View.GONE);
            holder.mCost.setVisibility(View.GONE);
            holder.mBudTitle.setVisibility(View.GONE);

        }

        @Override
        public int getItemCount() {
            return requestPart.size();
        }

        private void cleanupListener() {
            if (requestChildEventListener != null) {
                requestDatabaseReference.removeEventListener(requestChildEventListener);
            }
        }
    }

    private void goRequest(String target, String mRef){
        Intent intent = new Intent(this,Request.class);
        intent.putExtra("Target",target);
        intent.putExtra("mRef",mRef);
        startActivity(intent);
    }

    public void back(View v){
        finish();
    }

}
