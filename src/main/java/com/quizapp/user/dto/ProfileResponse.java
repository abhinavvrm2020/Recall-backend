package com.quizapp.user.dto;

public record ProfileResponse(
        Long id, String name, String email, int currentStreak, int longestStreak) {}
