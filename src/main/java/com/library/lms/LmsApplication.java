package com.library.lms;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class LmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(DataSource.class)
    public CommandLineRunner verifyDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("=================================================");
                System.out.println(">>> SUCCESS: Connected to MySQL Database: " + connection.getCatalog());
                System.out.println(">>> Database Product: " + connection.getMetaData().getDatabaseProductName() + " " + connection.getMetaData().getDatabaseProductVersion());
                System.out.println("=================================================");
            } catch (Exception e) {
                System.err.println(">>> ERROR: Failed to connect to MySQL database!");
                e.printStackTrace();
            }
        };
    }
}
