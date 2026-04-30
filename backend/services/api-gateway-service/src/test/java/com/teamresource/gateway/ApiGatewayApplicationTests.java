package com.teamresource.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.gateway.services.auth=http://localhost:8081",
        "app.gateway.services.user=http://localhost:8082",
        "app.gateway.services.event=http://localhost:8083",
        "app.gateway.services.resource=http://localhost:8084",
        "app.gateway.services.booking=http://localhost:8085",
        "app.gateway.services.notification=http://localhost:8086",
        "app.gateway.services.workflow=http://localhost:8087",
        "app.gateway.services.analytics=http://localhost:8088",
        "app.gateway.public-paths[0]=/api/v1/auth/**",
        "app.security.jwt.secret-base64=VEVBTV9SRVNPVVJDRV9NQU5BR0VNRU5UX0RFVl9TRUNSRVRfSFM1Nl8zMl9CWVRFUw=="
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
