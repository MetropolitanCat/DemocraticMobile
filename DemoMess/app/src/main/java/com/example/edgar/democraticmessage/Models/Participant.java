package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Participant {
    public String username;
    public String uID;
    public int budget;
    public int timeUsed;
    public String userRequest;

    public Participant(){}

    public Participant(String username, String uID, int budget){
        this.username = username;
        this.uID = uID;
        this.budget = budget;
        this.timeUsed = 0;
        this.userRequest = null;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("uID", uID);
        result.put("budget", budget);
        result.put("timeUsed", timeUsed);
        result.put("userRequest", userRequest);

        return result;
    }
}
