package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.moviehub.http.BaseHttpHandler.gson;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // Базовый URL
    private static MoviesStore store;
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        // Запуск сервера
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();
        // Создаем HTTP-клиент
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
            System.out.println("Сервер успешно остановлен");
        } else {
            System.out.println("Сервер не был создан, остановка не требуется");
        }
    }

    @BeforeEach
    void beforeEach(){
        store.clearStore();
    }


    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
        assertEquals("[]", body);
    }

    @Test
    void getMovieById_existing_returnsMovie() throws Exception {
        int id = store.addNewMovie(new Movie("Star War", 1986));
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        Movie getMovie = gson.fromJson(getResp.body(), Movie.class);

        assertEquals("Star War", getMovie.getTitle());
        assertEquals(1986, getMovie.getYear());
    }


    @Test
    void postMovie_emptyTitle_returnsError() throws Exception {
        String json = "{\"title\":\"\",\"year\":2000}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMovie_tooLongTitle_returnsError() throws Exception {
        String longTitle = "a".repeat(101);
        String json = String.format("{\"title\":\"%s\",\"year\":2000}", longTitle);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMovie_invalidYear_returnsError() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":1800}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMovie_wrongContentType_returnsError() throws Exception {
        String json = "{\"title\":\"Test\",\"year\":2020}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMovie_invalidJson_returnsError() throws Exception {
        String invalidJson = "{\"title\":\"Test\", \"year\":}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMovie_validData_returnsCreated() throws Exception {
        store.clearStore();
        String json = "{\"title\":\"Inception\",\"year\":2010}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"id\""));
    }

    @Test
    void getMovieById_notFound_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void getMovieById_notANumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void deleteExistingMovie_returnsOk() throws Exception {

        String json = "{\"title\":\"To Delete\",\"year\":2010}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
        String idStr = postResp.body().replaceAll(".*\"id\":(\\d+).*", "$1");
        int id = Integer.parseInt(idStr);

        // удаляем фильм
        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, deleteResp.statusCode(), "Удаление существующего фильма возвращает 200");

        // проверяем, что фильм удалён
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResp.statusCode(), "После удаления фильм не найден");
    }

    @Test
    void deleteMovie_notFound_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, resp.statusCode(), "Удаление несуществующего фильма - 404");
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void deleteMovie_notANumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode(), "Некорректный id (не число) - 400");
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void getMoviesByYear_validYear_returnsMovies() throws Exception {

        String json1 = "{\"title\":\"Yearly Movie 1\",\"year\":2000}";
        String json2 = "{\"title\":\"Yearly Movie 2\",\"year\":2000}";

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();
        client.send(req1, HttpResponse.BodyHandlers.ofString());

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();
        client.send(req2, HttpResponse.BodyHandlers.ofString());


        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(getReq, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode(), "Успешный запрос должен возвращать 200");
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ответ должен быть JSON-массив");
        assertTrue(body.contains("Yearly Movie 1") && body.contains("Yearly Movie 2"),
                "Массив должен содержать добавленные фильмы");
    }

    @Test
    void getMoviesByYear_noMovies_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode(), "Запрос без фильмов за указанный год возвращает 200");
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", resp.body(), "Пустой списочный ответ, если фильмов этого года нет");
    }

    @Test
    void getMoviesByYear_invalidYear_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=notANumber"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode(), "Передача некорректного параметра year возвращает 400");
        assertTrue(resp.body().contains("error"), "Ответ содержит объект с полем error");
    }

    @Test
    void getMoviesByYear_missingParameter_returnsError() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year="))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, resp.statusCode(), "Пустой параметр year возвращает 400");
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, resp.statusCode(), "Некорректный метод возвращает 405");
        assertTrue(resp.body().contains("error"));
    }
}
