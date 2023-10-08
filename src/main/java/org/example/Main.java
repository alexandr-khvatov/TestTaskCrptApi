package org.example;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        var a = new CrptApiImp(10, 10, TimeUnit.SECONDS);
        var doc = new CrptApiImp.Document(new CrptApiImp.Description("DescriptionName"),
                "111",
                "cr",
                CrptApiImp.DocType.LP_INTRODUCE_GOODS,
                true,
                "own",
                "part",
                "prod",
                LocalDate.now(),
                "type",
                List.of(new CrptApiImp.Product("cert",
                        LocalDate.now(),
                        "",
                        "",
                        "",
                        LocalDate.now(),
                        "",
                        "",
                        "")), LocalDate.now(), "");

        for (int i = 0; i < 100; i++) {
            new Thread(() -> a.createDocument(doc, "").thenAccept(System.out::println)).start();
        }
    }
}

