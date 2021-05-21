package com.example.lize.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Ambito {
    public static final String BASE_AMBITO_NAME = "Personal";
    public static final int BASE_AMBITO_COLOR = 1;

    private String name;
    private int color;
    private String selfID;
    private String userID;

    private ArrayList<Note> notes;
    private final Map<String, Folder> folders;

    public Ambito(String name, int color) {
        this.name = name;
        this.color = color;
        this.notes = new ArrayList<>();
        this.folders = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getSelfID() {
        return selfID;
    }

    public void setSelfID(String selfID) {
        this.selfID = selfID;
        for (Note note : this.notes){
            note.setAmbitoID(selfID);
        }
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public ArrayList<Note> getNotes() { return this.notes; }

    public ArrayList<Folder> getFolders() { return new ArrayList<>(folders.values()); }

    public Folder getFolder(String folderName) {
        return folders.get(folderName);
    }

    public void addFolder(String folderName){
        if (!folders.containsKey(folderName))
            folders.put(folderName, new Folder(folderName));
    }

    public void addNote(Note note){
        this.notes.add(note);
        note.setAmbitoID(selfID);

        String folderName = note.getFolderTAG();
        if (folderName != null){
            addFolder(folderName);
            getFolder(folderName).addNote(note);
        }
    }

}
