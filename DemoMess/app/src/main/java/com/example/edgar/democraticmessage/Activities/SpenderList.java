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

import com.example.edgar.democraticmessage.Models.Participant;
import com.example.edgar.democraticmessage.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SpenderList extends BaseActivity {

    private DatabaseReference users;
    private SpenderAdapter spenAdapter;
    private RecyclerView spenderRecycler;
    private String roomKey;

    private DatabaseReference data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spender_list);

        Intent intent = getIntent();
        roomKey = intent.getStringExtra("RoomKey");

        data = FirebaseDatabase.getInstance().getReference();
        users = data.child("participants").child(roomKey);

        spenderRecycler = findViewById(R.id.spendView);
        spenderRecycler.setLayoutManager(new LinearLayoutManager(this));

        WindowManager.LayoutParams wlp = getWindow().getAttributes();
        wlp.dimAmount = 0;
        wlp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        getWindow().setAttributes(wlp);

    }

    @Override
    public void onStart(){
        super.onStart();
        //Set adapter for the view to display messages
        spenAdapter = new SpenderAdapter(this, users);
        spenderRecycler.setAdapter(spenAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up message listener
        spenAdapter.cleanupListener();
    }

    private class SpenderViewHolder extends RecyclerView.ViewHolder {

        private final TextView spenName;
        private final TextView spenSpent;

        private SpenderViewHolder(final View itemView) {
            super(itemView);
            spenName = itemView.findViewById(R.id.spenUName);
            spenSpent = itemView.findViewById(R.id.spenSpent);
        }
    }

    private class SpenderAdapter extends RecyclerView.Adapter<SpenderViewHolder> {

        private final Context spenContext;
        private final DatabaseReference spenDatabaseReference;
        private final ChildEventListener spenChildEventListener;

        private final List<String> spenPartIds = new ArrayList<>();
        private final List<Participant> spenPart = new ArrayList<>();

        private SpenderAdapter(final Context context, DatabaseReference ref) {
            spenContext = context;
            spenDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Participant part = dataSnapshot.getValue(Participant.class);

                    spenPartIds.add(dataSnapshot.getKey());
                    spenPart.add(part);

                    notifyItemInserted(spenPart.size() - 1);
                    assert part != null;
                    Collections.sort(spenPart, new Comparator<Participant>() {
                        @Override
                        public int compare(Participant lhs, Participant rhs){
                            return lhs.timeUsed > rhs.timeUsed ? -1 : (lhs.timeUsed < rhs.timeUsed) ? 1 : 0;
                        }
                    });
                    notifyItemChanged(spenPart.indexOf(part));
                    notifyItemChanged(spenPartIds.indexOf(dataSnapshot.getKey()));



                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so displayed the changed comment.
                    Participant part = dataSnapshot.getValue(Participant.class);
                    String partKey = dataSnapshot.getKey();

                    int partIndex = spenPartIds.indexOf(partKey);
                    if (partIndex > -1) {
                        spenPart.set(partIndex, part);

                        assert part != null;
                        Collections.sort(spenPart, new Comparator<Participant>() {
                            @Override
                            public int compare(Participant lhs, Participant rhs){
                                return lhs.timeUsed > rhs.timeUsed ? -1 : (lhs.timeUsed < rhs.timeUsed) ? 1 : 0;
                            }
                        });

                        notifyItemChanged(partIndex);
                    }

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    // A message has changed, use the key to determine if we are displaying this
                    // message and if so remove it.
                    String partKey = dataSnapshot.getKey();

                    int partIndex = spenPartIds.indexOf(partKey);
                    if (partIndex > -1) {
                        spenPartIds.remove(partIndex);
                        spenPart.remove(partIndex);
                        notifyItemRemoved(partIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Participant movedPart = dataSnapshot.getValue(Participant.class);
                    String partKey = dataSnapshot.getKey();


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            ref.addChildEventListener(childEventListener);

            spenChildEventListener = childEventListener;
        }

        @NonNull
        @Override
        public SpenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(spenContext);
            View view = inflater.inflate(R.layout.spender, parent, false);
            return new SpenderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SpenderViewHolder holder, int position) {
            //Link data from the database into the holder elements
            Participant part = spenPart.get(position);
            holder.spenName.setText(part.username);
            holder.spenSpent.setText(Integer.toString(part.timeUsed));

        }

        @Override
        public int getItemCount() {
            return spenPart.size();

        }

        private void cleanupListener() {
            if (spenChildEventListener != null) {
                spenDatabaseReference.removeEventListener(spenChildEventListener);
            }
        }
    }

    public void back(View v){
        finish();
    }

}
