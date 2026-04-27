package com.teamresource.workflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.service.ApprovalDecisionCommand;
import com.teamresource.workflow.service.ApprovalService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WorkflowController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.workflow.infra.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.workflow.infra.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WorkflowControllerTest.TestConfig.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubApprovalService approvalService;

    @Test
    void pendingShouldReturnPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        approvalService.pendingResponse = List.of(approvalResponse("PENDING"));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_USER");

        mockMvc.perform(get("/api/v1/workflows/approvals/pending").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void approveShouldDetectAdminRole() throws Exception {
        UUID approvalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        approvalService.approveResponse = approvalResponse("APPROVED");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId.toString(), null, "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/workflows/approvals/{approvalId}/approve", approvalId)
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("note", "approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        org.assertj.core.api.Assertions.assertThat(approvalService.lastApproveCommand.admin()).isTrue();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubApprovalService approvalService() {
            return new StubApprovalService();
        }
    }

    static class StubApprovalService extends ApprovalService {

        private List<ApprovalResponse> pendingResponse = List.of();
        private ApprovalResponse approveResponse;
        private ApprovalDecisionCommand lastApproveCommand;

        StubApprovalService() {
            super(null, null, null, null, null);
        }

        @Override
        public List<ApprovalResponse> pending(UUID currentUserId, boolean admin) {
            return pendingResponse;
        }

        @Override
        public ApprovalResponse approve(ApprovalDecisionCommand command) {
            this.lastApproveCommand = command;
            return approveResponse;
        }
    }

    private static ApprovalResponse approvalResponse(String status) {
        return new ApprovalResponse(
                UUID.randomUUID(),
                "BOOKING",
                UUID.randomUUID(),
                "RESOURCE_BOOKING_APPROVAL",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Booking approval",
                "Needs review",
                1,
                1,
                status,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                null,
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                List.of()
        );
    }
}
