package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.service.facade.DashboardFacade;
import java.math.BigDecimal;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DashboardController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.teamresource.analytics.infra.security.JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(DashboardControllerTest.TestConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubDashboardFacade dashboardFacade;

    @Test
    void overviewShouldReturnPayload() throws Exception {
        dashboardFacade.overviewResponse = new DashboardOverviewResponse(12, 7, 2, 1, 2, 5, 3, 840);

        mockMvc.perform(get("/api/v1/analytics/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").value(12))
                .andExpect(jsonPath("$.data.approvedBookings").value(7));
    }

    @Test
    void popularResourcesShouldValidateLimit() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard/resources/popular").param("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        StubDashboardFacade dashboardFacade() {
            return new StubDashboardFacade();
        }
    }

    static class StubDashboardFacade extends DashboardFacade {

        private DashboardOverviewResponse overviewResponse = new DashboardOverviewResponse(0, 0, 0, 0, 0, 0, 0, 0);

        StubDashboardFacade() {
            super(null, Runnable::run);
        }

        @Override
        public DashboardOverviewResponse overview() {
            return overviewResponse;
        }

        @Override
        public List<ResourcePopularityResponse> topResources(int limit) {
            return List.of(new ResourcePopularityResponse(
                    UUID.randomUUID(),
                    "Room A",
                    "ROOM",
                    5,
                    4,
                    1,
                    0,
                    0,
                    240,
                    BigDecimal.valueOf(97.5),
                    OffsetDateTime.parse("2026-06-01T10:00:00Z")
            ));
        }
    }
}
