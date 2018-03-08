package com.example.edgar.democraticmessage.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.util.Log;

import com.example.edgar.democraticmessage.Activities.MainActivity;
import com.example.edgar.democraticmessage.Activities.Room;
import com.example.edgar.democraticmessage.R;

import static android.app.Notification.CATEGORY_MESSAGE;

public class UserData extends Service {
    private final IBinder binder = new DataBinder();

    private int userBudget = 0;
    private int userBudgetType = 0;
    private boolean noBudget = false;
    private int timeUsed = 0;
    private String roomKey = "";

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

        public void setRoomKey(String key){roomKey = key;}

        public String getRoomKey() {return roomKey;}

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {return binder;}
}
