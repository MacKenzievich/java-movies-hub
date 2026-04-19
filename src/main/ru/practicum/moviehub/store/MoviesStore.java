package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoviesStore {
    private static Map<Integer, Movie> movies = new HashMap<>();

    public static boolean addNewMovie(Movie movie) {
        movies.put(movie.getId(), movie);
        return true;
    }

    public static Movie foundMovie(Integer id) {
        if (movies.containsKey(id)) {
            return movies.get(id);
        }
        return null;
    }

    public static boolean deleteMovie(Integer id) {
        if (movies.containsKey(id)) {
            movies.remove(id);
            return true;
        }
        return false;
    }

    public static List<Movie> filtrationMovies(Integer year) {
        List<Movie> filteredMovies = movies.values().stream()
                .filter(movie -> movie.getYear().equals(year))
                .collect(Collectors.toList());
        return filteredMovies;
    }

    public static List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public static void clearStore(){
        movies = new HashMap<>();
    }
}