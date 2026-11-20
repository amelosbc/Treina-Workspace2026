package com.amelosbc.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Task Manager Spring Boot application.
 *
 * <p>This application provides a RESTful API for managing tasks,
 * built with clean architecture principles.</p>
 */
@SpringBootApplication
public class TaskmanagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskmanagerApplication.class, args);
    }

}
