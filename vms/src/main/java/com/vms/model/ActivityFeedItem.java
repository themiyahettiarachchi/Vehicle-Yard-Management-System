package com.vms.model;

import java.time.LocalDate;

public class ActivityFeedItem {
    private String type; // Vehicle Sold, Vehicle Added, etc.
    private String description;
    private LocalDate date;
    private String color; // CSS color for the indicator dot

    public ActivityFeedItem() {
    }

    public ActivityFeedItem(String type, String description, LocalDate date, String color) {
        this.type = type;
        this.description = description;
        this.date = date;
        this.color = color;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
