package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoviesStore {
    private Map<Integer, Movie> movies = new HashMap<>();
    private static Integer idCounter = 0;

    public int addNewMovie(Movie movie) {
        idCounter++;
        movies.put(idCounter, movie);
        return idCounter;
    }

    public Movie findMovie(Integer id) {
            return movies.get(id);
    }

    public Movie deleteMovie(Integer id) {
        return movies.remove(id);
    }


public List<Movie> filterMovies(Integer year) {
    List<Movie> filteredMovies = movies.values().stream()
            .filter(movie -> movie.getYear().equals(year))
            .collect(Collectors.toList());
    return filteredMovies;
}

public List<Movie> getMovies() {
    return new ArrayList<>(movies.values());
}

public void clearStore() {
    movies = new HashMap<>();
}
}