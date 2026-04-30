package com.teamresource.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = AuthServiceApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false",
                "app.oauth2.google-enabled=false"
        }
)
@ActiveProfiles("test")
class AuthServiceContextTest {

    @Test
    void contextLoads() {
    }
}
