package com.ollamagemmaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.ollamagemmaapp", "com.controller"})
public class OllamaGemmaAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(OllamaGemmaAppApplication.class, args);
    }

}
