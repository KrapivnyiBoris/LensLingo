package com.example.lenslingo.data.model;

import com.example.lenslingo.data.local.LanguageCount;

import java.util.List;

public class StatsData {
    public int learned;
    public int total;
    public int streak;
    public int daysSinceLast = -1;
    public String[] dayLabels;
    public int[] dayCounts;
    public List<LanguageCount> languages;

    public int languageCount() {
        return languages == null ? 0 : languages.size();
    }

    public boolean isEmpty() {
        return total == 0 && daysSinceLast == -1;
    }
}