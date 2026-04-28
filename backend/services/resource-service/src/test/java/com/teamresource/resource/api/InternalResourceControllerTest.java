package com.teamresource.resource.api;

import com.teamresource.resource.api.dto.ResourceApprovalPolicyResponse;
import com.teamresource.resource.service.ResourceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InternalResourceController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.resource.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.resource.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(InternalResourceControllerTest.TestConfig.class)
class InternalResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubResourceService resourceService;

    @Test
    void approvalPolicyShouldReturnInternalPayload() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        resourceService.approvalPolicyResponse = new ResourceApprovalPolicyResponse(resourceId, managerId, "ADMIN_APPROVAL", true, 240, 30, false);

        mockMvc.perform(get("/api/v1/internal/resources/{resourceId}/approval-policy", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.data.approvalMode").value("ADMIN_APPROVAL"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubResourceService resourceService() {
            return new StubResourceService();
        }
    }

    static class StubResourceService extends ResourceService {
        private ResourceApprovalPolicyResponse approvalPolicyResponse;

        StubResourceService() {
            super(null, null, null);
        }

        @Override
        public ResourceApprovalPolicyResponse approvalPolicy(UUID resourceId) {
            return approvalPolicyResponse;
        }
    }
}
