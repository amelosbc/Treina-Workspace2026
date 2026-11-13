package com.amelosbc.taskmanager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the Spring application context loads successfully
 * and connects to the file-based H2 database.
 */
@SpringBootTest
class TaskmanagerApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("Database connects using file-based H2 configuration and creates taskdb file")
    void databaseConnectsToFileBasedH2() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertThat(url).contains("jdbc:h2:file:./data/taskdb");
        }

        File dbFile = new File("./data/taskdb.mv.db");
        assertThat(dbFile).exists();
    }
}
