package com.example.edgar.democraticmessage.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public class UserData extends Service {
    private final IBinder binder = new DataBinder();

    private int userBudget = 0;
    private String userBudgetType = "";
    private boolean noBudget = false;
    private int timeUsed = 0;
    private String roomKey = "";
    private String roomType = "";

    public UserData() {
    }


    public class DataBinder extends Binder implements IInterface{
        @Override
        public IBinder asBinder() {
            return this;
        }

        public void setRoomType(String type){roomType = type;}

        public String getRoomType(){return roomType;}

        public void setBudget(int budget) {userBudget = budget;}

        public int getBudget() {return userBudget;}

        public void setBudgetType(String type) {userBudgetType = type;}

        public String getBudgetType() {return userBudgetType;}

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
