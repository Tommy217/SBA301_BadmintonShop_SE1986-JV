package com.fastship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// WebhookDispatcher gui webhook trong outbox
@EnableScheduling
public class FastShipApplication {
    public static void main(String[] args) {
        SpringApplication.run(FastShipApplication.class, args);
        System.out.println("\n========================================================");
        System.out.println("  FASTSHIP EXPRESS BACKEND (SPRING BOOT) SẴN SÀNG!  ");
        System.out.println("   • Port API:  http://localhost:8081/api/v1");
        System.out.println("   • Database:  MongoDB (fastship_db)");
        System.out.println("========================================================\n");
    }
}
