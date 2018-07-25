package com.example.edgar.democraticmessage.Models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class RoomType {
    public String roomname;

    public String budgettype;
    private int budgetlimit;

    public String conferencetype;

    public int participants;
    public int currentParticipants;

    private String password;

    public String roomOwner;

    public int startingBudget;

    public int roomClass;


    public RoomType() {
    }

    public RoomType(String roomname, String budgettype,
                    int budgetlimit, int startingBudget,
                    String conferencetype, int participants,
                    String password,
                    String roomOwner, int roomClass) {
        this.roomname = roomname;
        this.budgettype = budgettype;
        this.budgetlimit = budgetlimit;
        this.startingBudget = startingBudget;
        this.conferencetype = conferencetype;
        this.participants = participants;
        this.currentParticipants = 1;
        this.password = password;
        this.roomOwner = roomOwner;
        this.roomClass = roomClass;
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
        result.put("roomOwner", roomOwner);
        result.put("roomClass", roomClass);
        return result;
    }
}
