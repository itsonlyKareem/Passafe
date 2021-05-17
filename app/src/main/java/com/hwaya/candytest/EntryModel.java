package com.hwaya.candytest;

public class EntryModel {
    String username;
    String password;
    String category;
    String name;
    String website;
    String notes;

    public EntryModel() {
    }

    public EntryModel(String username, String password, String category, String name, String webiste, String notes) {
        this.name = name;
        this.website = webiste;
        this.notes = notes;
        this.username = username;
        this.password = password;
        this.category = category;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
