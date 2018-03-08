package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Edgar on 15/11/2017.
 */

public class Participant {
    public String username;
    public String uID;
    public int budget;
    public int timeUsed;
    public String userRequest;

    public Participant(){}

    public Participant(String username, String uID, int budget, int timeUsed, String userRequest){
        this.username = username;
        this.uID = uID;
        this.budget = budget;
        this.timeUsed = timeUsed;
        this.userRequest = userRequest;
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
