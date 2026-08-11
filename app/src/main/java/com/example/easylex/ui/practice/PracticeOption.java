package com.example.easylex.ui.practice;

public class PracticeOption {
    private String title, description, type, badgeText;
    private int iconRes, color;

    public PracticeOption(String title, String description, int iconRes, int color, String type, String badgeText) {
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.color = color;
        this.type = type;
        this.badgeText = badgeText;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIconRes() { return iconRes; }
    public int getColor() { return color; }
    public String getType() { return type; }
    public String getBadgeText() { return badgeText; }
}