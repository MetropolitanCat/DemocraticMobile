package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Message {
    public String username;
    public String uID;
    public String message;
    public int cost;
    public int sendBudget;
    public String timeSent;
    public int privMess;

    public Message() {
    }

    public Message(String username, String message, int cost, int sendBudget, String timeSent, String uID, int privMess) {
        this.username = username;
        this.message = message;
        this.cost = cost;
        this.sendBudget = sendBudget;
        this.timeSent = timeSent;
        this.uID = uID;
        this.privMess = privMess;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("message", message);
        result.put("cost",cost);
        result.put("sendBudget", sendBudget);
        result.put("timeSent",timeSent);
        result.put("uID",uID);
        result.put("privMess",privMess);

        return result;
    }
}
