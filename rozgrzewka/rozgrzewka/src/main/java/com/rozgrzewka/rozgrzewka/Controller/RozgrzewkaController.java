package com.rozgrzewka.rozgrzewka.Controller;

import com.rozgrzewka.rozgrzewka.ResourceRepresentation.Rozgrzewka;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class RozgrzewkaController {
    private static String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public Rozgrzewka greeting(@RequestParam(defaultValue = "World") String name){
        return new Rozgrzewka(counter.incrementAndGet(), template.formatted(name));
    }

}
