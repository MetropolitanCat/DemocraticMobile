package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Edgar on 14/11/2017.
 */

public class Message {
    public String username;
    public String message;
    public int cost;

    public Message() {
    }

    public Message(String username, String message, int cost) {
        this.username = username;
        this.message = message;
        this.cost = cost;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("message", message);
        result.put("cost",cost);

        return result;
    }
}
