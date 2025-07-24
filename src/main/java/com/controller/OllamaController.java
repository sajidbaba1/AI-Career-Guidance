package com.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;
import java.time.Duration;

@CrossOrigin(origins = "http://localhost:34")
@RestController
@RequestMapping("/ask")
public class OllamaController {

    @PostMapping
    public ResponseEntity<String> askModel(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest().body("Prompt is required");
        }

        try {
            // Prepare request body for Ollama
            Map<String, Object> ollamaBody = Map.of(
                    "model", "gemma:7b",
                    "prompt", prompt,
                    "stream", false
            );

            // Send POST request to Ollama API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            new ObjectMapper().writeValueAsString(ollamaBody)
                    ))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Extract only the "response" field from JSON
            String fullJson = response.body();
            String reply = new ObjectMapper().readTree(fullJson).get("response").asText();

            return ResponseEntity.ok(reply);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamModel(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        return Flux.<String>create(sink -> {
            try {
                Map<String, Object> req = Map.of(
                        "model", "gemma:7b",
                        "prompt", prompt,
                        "stream", true
                );

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:11434/api/generate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(req)))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                        .thenAccept(resp -> resp.body().forEach(line -> {
                            try {
                                String token = new ObjectMapper().readTree(line).get("response").asText();
                                sink.next(token);
                            } catch (Exception ignore) {}
                        }))
                        .whenComplete((v, ex) -> sink.complete());
            } catch (Exception e) {
                sink.error(e);
            }
        }).delayElements(Duration.ofMillis(5));
    }
}
