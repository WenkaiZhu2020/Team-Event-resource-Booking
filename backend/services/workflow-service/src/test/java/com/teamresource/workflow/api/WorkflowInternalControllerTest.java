package com.teamresource.workflow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.workflow.api.dto.ApprovalResponse;
import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import com.teamresource.workflow.domain.ApprovalTargetType;
import com.teamresource.workflow.service.ApprovalWorkflowFacade;
import com.teamresource.workflow.service.command.CreateApprovalCommand;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WorkflowInternalController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.common.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.common.security.InternalApiKeyFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WorkflowInternalControllerTest.TestConfig.class)
class WorkflowInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubApprovalWorkflowFacade approvalWorkflowFacade;

    @Test
    void createShouldReturnCreatedApproval() throws Exception {
        approvalWorkflowFacade.createResponse = approvalResponse();

        mockMvc.perform(post("/api/v1/internal/workflows/approvals")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateApprovalRequest(
                                ApprovalTargetType.BOOKING,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                null,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Booking approval",
                                "RESOURCE_BOOKING_APPROVAL",
                                "Needs review",
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createShouldValidateTitle() throws Exception {
        mockMvc.perform(post("/api/v1/internal/workflows/approvals")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "targetType", "BOOKING",
                                "targetId", UUID.randomUUID(),
                                "requesterId", UUID.randomUUID(),
                                "targetOwnerId", UUID.randomUUID(),
                                "resourceId", UUID.randomUUID(),
                                "title", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubApprovalWorkflowFacade approvalWorkflowFacade() {
            return new StubApprovalWorkflowFacade();
        }
    }

    static class StubApprovalWorkflowFacade extends ApprovalWorkflowFacade {

        private ApprovalResponse createResponse;
        private CreateApprovalCommand lastCreateCommand;

        StubApprovalWorkflowFacade() {
            super(null, null, null, null);
        }

        @Override
        public ApprovalResponse create(CreateApprovalCommand command) {
            this.lastCreateCommand = command;
            return createResponse;
        }
    }

    private static ApprovalResponse approvalResponse() {
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
                "ASSIGNED_USER",
                "PENDING",
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                null,
                null,
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                OffsetDateTime.parse("2026-05-01T10:00:00Z"),
                List.of(),
                List.of()
        );
    }
}
