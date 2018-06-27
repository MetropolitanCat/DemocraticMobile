package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Participant {
    public String username;
    public String uID;
    public int budget;
    public int timeUsed;
    public int reqUsed;
    public int donUsed;
    public String userRequest;
    public String userDonate;


    public Participant(){}

    public Participant(String username, String uID, int budget){
        this.username = username;
        this.uID = uID;
        this.budget = budget;
        this.timeUsed = 0;
        this.reqUsed = 0;
        this.donUsed = 0;
        this.userRequest = null;
        this.userDonate = null;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("uID", uID);
        result.put("budget", budget);
        result.put("timeUsed", timeUsed);
        result.put("reqUsed", reqUsed);
        result.put("donUsed", donUsed);
        result.put("userRequest", userRequest);
        result.put("userDonate", userDonate);

        return result;
    }
}
