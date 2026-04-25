package com.teamresource.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.resource.api.dto.AvailabilityRuleResponse;
import com.teamresource.resource.api.dto.MaintenanceSlotRequest;
import com.teamresource.resource.api.dto.MaintenanceSlotResponse;
import com.teamresource.resource.api.dto.ResourceResponse;
import com.teamresource.resource.api.dto.UpsertResourceRequest;
import com.teamresource.resource.service.ResourceService;
import java.time.LocalTime;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ResourceController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ResourceControllerTest.TestConfig.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubResourceService resourceService;

    @Test
    void listShouldReturnCatalogPayload() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        resourceService.listResponse = List.of(new ResourceResponse(
                resourceId,
                managerId,
                "Main Hall",
                "Shared hall",
                "FACILITY",
                "North Building",
                300,
                "ACTIVE",
                "ADMIN_APPROVAL",
                true,
                false,
                720,
                90,
                List.of(new AvailabilityRuleResponse(UUID.randomUUID(), 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true)),
                List.of(),
                OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                OffsetDateTime.parse("2026-01-02T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Main Hall"))
                .andExpect(jsonPath("$.data[0].requiresApproval").value(true));
    }

    @Test
    void createShouldValidateAvailabilityRules() throws Exception {
        UUID managerId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/resources")
                        .principal(() -> managerId.toString())
                        .with(authentication(new TestingAuthenticationToken(managerId.toString(), null, "ROLE_RESOURCE_MANAGER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "Broken Resource",
                                "description", "Invalid",
                                "type", "ROOM",
                                "location", "Floor 1",
                                "capacity", 10,
                                "availabilityRules", java.util.List.of(java.util.Map.of(
                                        "dayOfWeek", 9,
                                        "startTime", "09:00:00",
                                        "endTime", "17:00:00",
                                        "available", true
                                ))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void deactivateShouldReturnUpdatedResource() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        resourceService.deactivateResponse = new ResourceResponse(
                resourceId,
                managerId,
                "Main Hall",
                "Shared hall",
                "FACILITY",
                "North Building",
                300,
                "INACTIVE",
                "ADMIN_APPROVAL",
                true,
                false,
                720,
                90,
                List.of(),
                List.of(),
                OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                OffsetDateTime.parse("2026-01-03T10:00:00Z")
        );

        mockMvc.perform(post("/api/v1/resources/{resourceId}/deactivate", resourceId)
                        .principal(() -> managerId.toString())
                        .with(authentication(new TestingAuthenticationToken(managerId.toString(), null, "ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubResourceService resourceService() {
            return new StubResourceService();
        }
    }

    static class StubResourceService extends ResourceService {

        private List<ResourceResponse> listResponse;
        private ResourceResponse deactivateResponse;

        StubResourceService() {
            super(null, null, null);
        }

        @Override
        public List<ResourceResponse> listCatalog(String query, String type, String status, Boolean requiresApproval) {
            return listResponse;
        }

        @Override
        public ResourceResponse deactivate(UUID resourceId, UUID currentUserId, boolean admin) {
            return deactivateResponse;
        }

        @Override
        public ResourceResponse create(UUID managerId, boolean privileged, UpsertResourceRequest request) {
            return null;
        }

        @Override
        public MaintenanceSlotResponse addMaintenanceSlot(
                UUID resourceId,
                UUID currentUserId,
                boolean admin,
                MaintenanceSlotRequest request
        ) {
            return null;
        }
    }
}
