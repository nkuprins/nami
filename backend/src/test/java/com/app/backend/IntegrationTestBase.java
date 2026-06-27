package com.app.backend;

import com.app.backend.security.RateLimitFilter;
import com.app.backend.testutil.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("db/schema.sql"),
                    "/docker-entrypoint-initdb.d/schema.sql"
            );

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean protected S3Client s3Client;
    @MockitoBean protected S3Presigner s3Presigner;

    protected MockMvc mockMvc;

    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected AuthTestHelper authTestHelper;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUpMockMvc() {
        ReflectionTestUtils.invokeMethod(rateLimitFilter, "evictStaleBuckets");
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }
}
