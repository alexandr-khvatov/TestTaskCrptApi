package org.example;

import java.util.concurrent.CompletableFuture;

public interface CrptApi {
    CompletableFuture<String> createDocument(CrptApiImp.Document doc, String signature);
}
