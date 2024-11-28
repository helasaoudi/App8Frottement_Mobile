package com.example.frottementsactionsapp;

public class ActionItem {
    private String title;
    private int frottements;
    private boolean isEnabled; // Nouveau champ

    public ActionItem(String title, int frottements, boolean isEnabled) {
        this.title = title;
        this.frottements = frottements;
        this.isEnabled = isEnabled;
    }
    public ActionItem(String title, int frottements) {
        this.title = title;
        this.frottements = frottements;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFrottements() {
        return frottements;
    }

    public void setFrottements(int frottements) {
        this.frottements = frottements;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
