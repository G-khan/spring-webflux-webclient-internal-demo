package dev.gokhana.reactive.webclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;

@SpringBootApplication
public class ReactiveWebclient {

    private final WebClient defaultWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReactiveWebclient(WebClient defaultWebClient) {
        this.defaultWebClient = defaultWebClient;
    }

    public JsonNode getTodoFromAPI() {
        return this.defaultWebClient
                .get()
                .uri("/todos/1")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.max(5))
                .timeout(Duration.ofSeconds(2),
                        Mono.just(objectMapper.createObjectNode().put("message", "fallback")))
                .block();
    }

    public JsonNode getTodoFromAPIInDepth() {
        return this.defaultWebClient
                .get()
                .uri("/todos/1")
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    System.out.println("4xx error");
                    return Mono.error(new RuntimeException("4xx"));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    System.out.println("5xx error");
                    return Mono.error(new RuntimeException("5xx"));
                })
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode postToTodoAPI() {
        return this.defaultWebClient
                .post()
                .uri("/todos")
                .body(BodyInserters.fromValue("{ \"title\": \"foo\", \"body\": \"bar\", \"userId\": \"1\"}"))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode get() {
        return this.defaultWebClient
                .get()
                .uri("/todos")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }


    public void stream() {
        this.defaultWebClient.get()
                .uri("/todos")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .subscribe(it -> System.out.println(it.toPrettyString() + " : " + System.currentTimeMillis()));
    }

    @Bean
    CommandLineRunner demo(WebClient client) {
        return args -> {
            var todo = getTodoFromAPI();
            System.out.println(todo.toPrettyString());
            var todos = get();
            System.out.println(todos);
            stream();
        };
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ReactiveWebclient.class)
                .properties(Collections.singletonMap("server.port", "8082"))
                .run(args);
    }
}
