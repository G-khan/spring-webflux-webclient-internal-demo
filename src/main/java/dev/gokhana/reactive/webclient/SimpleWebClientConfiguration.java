package dev.gokhana.reactive.webclient;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.logging.Logger;

@Component
public class SimpleWebClientConfiguration {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    Logger logger = Logger.getLogger(SimpleWebClientConfiguration.class.getName());

    @Bean
    public WebClient webClientFromScratch() {

        return WebClient.builder()
                .baseUrl(BASE_URL)
                //.filter(basicAuthentication("user", "password"))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            logger.info("Request:" + clientRequest.method() +" : "+ clientRequest.url());
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> logger.info( name +"="+ value)));
            return next.exchange(clientRequest);
        };
    }
}