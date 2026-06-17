package com.app.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest
@Testcontainers
class BackendApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("db/schema.sql"),
                    "/docker-entrypoint-initdb.d/schema.sql"
            );

    @Test
    void contextLoads() {
    }
}
