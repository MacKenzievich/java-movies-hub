package ru.practicum.moviehub.model;

public class Movie {
    private static Integer idCounter = 1;

    private Integer id;
    private String title;
    private Integer year;

    public Movie(String title, int year) {
        this.id = idCounter++;
        this.title = title;
        this.year = year;
    }

    public Integer getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }
}