package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CrptApiImp implements CrptApi {
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final String ACCESS_TOKEN = "Bearer XXXXXXXXX";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final long timeout;
    private final long capacity;
    private long availableTokens;
    private long lastRefill;

    public CrptApiImp(long permits, long timeout, TimeUnit unit) {
        this.timeout = unit.toNanos(timeout);
        this.capacity = permits;
        this.availableTokens = permits;
        this.lastRefill = System.nanoTime();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private void refill() {
        long now = System.nanoTime();
        long timeElapsed = now - lastRefill;

        if (timeElapsed > timeout) {
            availableTokens = capacity;
            lastRefill = now;
        }
        if (availableTokens == 0) {
            try {
                long sleepTime = timeout - timeElapsed;
                TimeUnit.NANOSECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            availableTokens = capacity;
            lastRefill = System.nanoTime();
        }
    }

    synchronized private void tryConsume() {
        refill();
        availableTokens -= 1;
    }

    @Override
    public CompletableFuture<String> createDocument(Document doc, String signature) {
        checkValidation();
        tryConsume();
        var jsonDoc = serializeDocument(doc);
        System.out.println(Thread.currentThread());
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonDoc))
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("Authorization", ACCESS_TOKEN)
                .build();
        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    private String serializeDocument(Document doc) {
        try {
            String jsonString = mapper.writeValueAsString(doc);
            return jsonString;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization exception", e);
        }
    }

    private void checkValidation() {
        // Check Validation
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Document(Description description,
                    String docId,
                    String docStatus,
                    DocType docType,
                    @JsonProperty("importRequest")
                    boolean importRequest,
                    String ownerInn,
                    String participantInn,
                    String producerInn,
                    LocalDate productionDate,
                    String productionType,
                    List<Product> products,
                    LocalDate regDate,
                    String regNumber) {
    }

    enum DocType {
        LP_INTRODUCE_GOODS
    }

    record Description(String participantInn) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Product(
            String certificateDocument,
            LocalDate certificateDocumentDate,
            String certificateDocumentNumber,
            String ownerInn,
            String producerInn,
            LocalDate productionDate,
            String tnvedCode,
            String uitCode,
            String uituCode) {
    }
}
