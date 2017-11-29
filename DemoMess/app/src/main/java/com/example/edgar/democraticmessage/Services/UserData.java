package com.example.edgar.democraticmessage.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import com.example.edgar.democraticmessage.Activities.MainActivity;
import com.example.edgar.democraticmessage.Activities.Room;
import com.example.edgar.democraticmessage.R;

public class UserData extends Service {
    private final IBinder binder = new DataBinder();

    private int userBudget = 0;
    private int userBudgetType = 0;
    private boolean noBudget = false;
    private int timeUsed = 0;
    private Notification notif = null;
    private static int NOTIFICATION = 5;

    public UserData() {
    }

    public class DataBinder extends Binder implements IInterface{
        @Override
        public IBinder asBinder() {
            return this;
        }

        public void setBudget(int budget) {userBudget = budget;}

        public int getBudget() {return userBudget;}

        public void setBudgetType(int type) {userBudgetType = type;}

        public int getBudgetType() {return userBudgetType;}

        public void setEmpty(boolean empty){noBudget = empty;}

        public boolean getEmpty() {return noBudget;}

        public int getTimeUsed() {return timeUsed;}

        public void setTimeUsed(int used){timeUsed += used;}

        public void changeRequest(Boolean change){
            if(change){
                Log.d("Notification","True, make");
                createRequest();
                startForeground(NOTIFICATION,notif);
            }
            else{
                Log.d("Notification","False, remove");
                stopForeground(true);
            }
        }

        public void createRequest(){
            // Not working yet
            Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
            //Make sure that only one notification is active at a time and that it overwrites the old notification
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Log.d("Notification","Making notification");
            notif = new Notification.Builder(getApplicationContext())
                    //Icon image is open source and obtained from https://material.io/icons/
                    .setSmallIcon(R.drawable.ic_comment_black_24dp)
                    .setContentTitle(getApplicationContext().getString(R.string.app_name))
                    .setContentIntent(intent)
                    .setContentText("Request!!!!")
                    .build();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {return binder;}
}
