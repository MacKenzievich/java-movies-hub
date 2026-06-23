package ru.practicum.moviehub.model;

public class Movie {
    private String title;
    private Integer year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public Integer getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }
}