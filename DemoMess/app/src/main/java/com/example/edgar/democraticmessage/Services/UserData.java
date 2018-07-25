package com.example.edgar.democraticmessage.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import com.example.edgar.democraticmessage.Activities.BaseActivity;

public class UserData extends Service {
    private final IBinder binder = new DataBinder();

    private int userBudget = 0;
    private String userBudgetType = "";
    private boolean noBudget = false;
    private int timeUsed = 0;
    private int reqUsed = 0;
    private int donUsed = 0;
    private String roomKey = "";
    private String roomType = "";
    private int roomClass = 0;

    public UserData() {
    }

    public class DataBinder extends Binder implements IInterface{
        @Override
        public IBinder asBinder() {
            return this;
        }
        //Setter for conference type
        public void setRoomType(String type){roomType = type;}
        //Getter for conference type
        public String getRoomType(){return roomType;}
        //Setter for the talk budget value
        public void setBudget(int budget) {userBudget = budget;}
        //Getter for the talk budget value
        public int getBudget() {return userBudget;}
        //Setter for talk budget type
        public void setBudgetType(String type) {userBudgetType = type;}
        //Getter for talk budget type
        public String getBudgetType() {return userBudgetType;}
        //Setter for the no budget flag
        public void setEmpty(boolean empty){noBudget = empty;}
        //Getter for the no budget flag
        public boolean getEmpty() {return noBudget;}
        //Getter for the time used
        public int getTimeUsed() {return timeUsed;}
        //Adder for the time used
        public void addTimeUsed(int used){timeUsed += used;}
        //Setter for the time used
        public void setTimeUsed(int used){timeUsed = used;}
        //Getter for requests used
        public int getReqUsed(){return reqUsed;}
        //Adder for incrementing requests used
        public void addReqUsed(){reqUsed ++;}
        //Setter for requests used
        public void setReqUsed(int used){reqUsed = used;}
        //Getter fpr donates used
        public int getDonUsed(){return donUsed;}
        //Adder for donates used
        public void addDonUsed(){donUsed ++;}
        //Setter for donates used
        public void setDonUsed(int used){donUsed = used;}
        //Setter for room id
        public void setRoomKey(String key){roomKey = key;}
        //Getter for the room id
        public String getRoomKey() {return roomKey;}
        //Setter for room class
        public void setClassType(int type){roomClass = type;}
        //Getter for room class
        public int getClassType(){return roomClass;}
        //Exchange ID's for Username
        public String idToName(String id){
            String temp = "";
            for(int i = 0; i < BaseActivity.nameIds.size(); i++){
                if(BaseActivity.nameIds.get(i).equals(id)){
                    temp = BaseActivity.name.get(i).username;
                    break;
                }
            }
            return temp;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {return binder;}
}
