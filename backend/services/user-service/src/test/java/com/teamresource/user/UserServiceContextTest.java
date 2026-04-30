package com.teamresource.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = UserServiceApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false"
        }
)
@ActiveProfiles("test")
class UserServiceContextTest {

    @Test
    void contextLoads() {
    }
}
