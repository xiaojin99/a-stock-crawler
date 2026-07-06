package com.stock.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StockCrawlerWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockCrawlerWebApplication.class, args);
    }
}
