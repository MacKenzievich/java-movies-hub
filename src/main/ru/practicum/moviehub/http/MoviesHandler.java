package ru.practicum.moviehub.http;

import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.api.ErrorResponse;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ((method.equals("POST")) || (method.equals("PUT") && path.matches("^/movies/"))) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("application/json")) {
                sendJson(exchange, 415, gson.toJson(new ErrorResponse("Неподдерживаемый тип контента", null)));
                return;
            }
        }
        if (method.equals("GET") && "/movies".equals(path)) {
            handleGetAllOrFiltered(exchange, query);
        } else if (method.equals("POST") && "/movies".equals(path)) {
            handleAddMovie(exchange);
        } else if (method.equals("GET") && path.startsWith("/movies/")) {
            handleGetMovieById(exchange);
        } else if (method.equals("DELETE") && path.startsWith("/movies/")) {
            handleDeleteMovie(exchange);
        } else {
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Маршрут не найден", null)));
        }
    }

    private void handleGetAllOrFiltered(HttpExchange exchange, String query) throws IOException {
        if (query != null && query.startsWith("year=")) {
            try {
                int year = Integer.parseInt(query.substring(5));
                List<Movie> result = MoviesStore.filtrationMovies(year);
                String json = gson.toJson(result);
                sendJson(exchange, 200, json);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'", null)));
            }
        } else {
            List<Movie> movies = MoviesStore.getMovies();
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

        // Валидация
        java.util.List<String> details = new java.util.ArrayList<>();
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            details.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            details.add("название не должно превышать 100 символов");
        }

        int currentYear = java.time.Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            details.add("год должен быть между 1888 и " + (currentYear + 1));
        }

        if (!details.isEmpty()) {
            sendJson(exchange, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            return;
        }

        Movie newMovie = new Movie(movie.getTitle(), movie.getYear());
        MoviesStore.addNewMovie(newMovie);
        sendJson(exchange, 201, gson.toJson(newMovie));
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

        Movie movie = MoviesStore.foundMovie(id);
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

        boolean deleted = MoviesStore.deleteMovie(id);
        if (deleted) {
            sendNoContent(exchange);
        } else {
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
        }
    }
}