package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Edgar on 14/11/2017.
 */

public class RoomType {
    public String roomname;

    public int budgettype;
    public int budgetlimit;

    public String conferencetype;

    public int participants;
    public int currentParticipants;

    public String password;

    public int startingBudget;


    public RoomType() {
    }

    public RoomType(String roomname, int budgettype, int budgetlimit, int startingBudget, String conferencetype, int participants, int currentParticipants, String password) {
        this.roomname = roomname;
        this.budgettype = budgettype;
        this.budgetlimit = budgetlimit;
        this.startingBudget = startingBudget;
        this.conferencetype = conferencetype;
        this.participants = participants;
        this.currentParticipants = currentParticipants;
        this.password = password;

    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("roomname",roomname);
        result.put("budgettype", budgettype);
        result.put("budgetlimit", budgetlimit);
        result.put("startingBudget", startingBudget);
        result.put("conferencetype", conferencetype);
        result.put("participants", participants);
        result.put("currentParticipants", currentParticipants);
        result.put("password", password);

        return result;
    }
}
