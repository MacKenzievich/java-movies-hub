package ru.practicum.moviehub.http;

import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.api.ErrorResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final List<String> SUPPORTED_METHODS = Arrays.asList("GET", "POST", "DELETE");

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!SUPPORTED_METHODS.contains(method)) {
            sendJson(exchange, 405, gson.toJson(new ErrorResponse("Method Not Allowed", null)));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ((method.equals("POST")) || (method.equals("PUT") && path.matches(MoviesServer.MOVIES_PATH))) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("application/json")) {
                sendJson(exchange, 415, gson.toJson(new ErrorResponse("Неподдерживаемый тип контента", null)));
                return;
            }
        }
        if (method.equals("GET") && MoviesServer.MOVIES_PATH.equals(path)) {
            handleGetAllOrFiltered(exchange, query);
        } else if (method.equals("POST") && MoviesServer.MOVIES_PATH.equals(path)) {
            handleAddMovie(exchange);
        } else if (method.equals("GET") && path.startsWith(MoviesServer.MOVIES_PATH)) {
            handleGetMovieById(exchange);
        } else if (method.equals("DELETE") && path.startsWith(MoviesServer.MOVIES_PATH)) {
            handleDeleteMovie(exchange);
        } else {
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Маршрут не найден", null)));
        }
    }

    private void handleGetAllOrFiltered(HttpExchange exchange, String query) throws IOException {
        if (query != null && query.startsWith("year=")) {
            try {
                int year = Integer.parseInt(query.substring(5));
                List<Movie> result = store.filterMovies(year);
                String json = gson.toJson(result);
                sendJson(exchange, 200, json);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'", null)));
            }
        } else {
            List<Movie> movies = store.getMovies();
            String json = gson.toJson(movies);
            sendJson(exchange, 200, json);
        }
    }

    private void handleAddMovie(HttpExchange exchange) throws IOException {

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie movie;

        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (Exception e) {
            sendJson(exchange, 422, gson.toJson(new ErrorResponse("Ошибка валидации", null)));
            return;
        }

        List<String> validationErrors = validateMovie(movie);
        if (!validationErrors.isEmpty()) {
            sendJson(exchange, 422, gson.toJson(new ErrorResponse("Ошибка валидации", validationErrors)));
            return;
        }

        int id = store.addNewMovie(movie);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("title", movie.getTitle());
        response.put("year", movie.getYear());

        sendJson(exchange, 201, gson.toJson(response));
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        int currentYear = java.time.Year.now().getValue();

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно превышать 100 символов");
        }

        if (movie.getYear() < MIN_YEAR || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между " + MIN_YEAR + " и " + (currentYear + 1));
        }
        return errors;
    }

    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String idStr = parts[parts.length - 1];

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный ID", null)));
            return;
        }

        Movie movie = store.findMovie(id);
        if (movie == null) {
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
            return;
        }
        sendJson(exchange, 200, gson.toJson(movie));
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String idStr = parts[parts.length - 1];

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный ID", null)));
            return;
        }

        if (store.deleteMovie(id) != null) {
            sendNoContent(exchange);
        } else {
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
        }
    }
}