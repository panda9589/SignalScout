package com.scout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SignalScout - Investment Research Radar
 * 
 * A personal investment research assistant that ingests documents,
 * extracts investment events using AI, and generates portfolio recommendations
 * using deterministic scoring rules.
 */
@SpringBootApplication
@EnableScheduling
public class SignalScoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalScoutApplication.class, args);
    }
}
