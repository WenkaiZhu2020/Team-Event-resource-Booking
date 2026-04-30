package com.teamresource.analytics.api;

import com.teamresource.analytics.api.dto.DashboardOverviewResponse;
import com.teamresource.analytics.api.dto.EventRegistrationMetricResponse;
import com.teamresource.analytics.api.dto.ResourcePopularityResponse;
import com.teamresource.analytics.api.dto.ResourceUsageMetricResponse;
import com.teamresource.analytics.service.ResourcePopularityRefreshService;
import com.teamresource.analytics.service.facade.DashboardFacade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private MockMvc mockMvc;
    private StubDashboardFacade dashboardFacade;
    private StubResourcePopularityRefreshService resourcePopularityRefreshService;

    @BeforeEach
    void setUp() {
        dashboardFacade = new StubDashboardFacade();
        resourcePopularityRefreshService = new StubResourcePopularityRefreshService();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new DashboardController(dashboardFacade),
                        new AnalyticsAdminController(resourcePopularityRefreshService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void overviewShouldReturnPayload() throws Exception {
        dashboardFacade.overviewResponse = new DashboardOverviewResponse(12, 7, 2, 1, 2, 5, 3, 840);

        mockMvc.perform(get("/api/v1/analytics/dashboard/overview")
                        .param("from", "2026-04-01T00:00:00Z")
                        .param("to", "2026-04-30T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").value(12))
                .andExpect(jsonPath("$.data.approvedBookings").value(7));
    }

    @Test
    void overviewShouldAlsoWorkOnShortPath() throws Exception {
        dashboardFacade.overviewResponse = new DashboardOverviewResponse(1, 1, 0, 0, 0, 1, 1, 60);

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBookings").value(1));
    }

    @Test
    void eventRegistrationsShouldReturnPayload() throws Exception {
        dashboardFacade.eventRegistrationResponses = List.of(new EventRegistrationMetricResponse(UUID.randomUUID(), 4, 1, 2));

        mockMvc.perform(get("/api/v1/dashboard/events/registrations")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].activeBookings").value(4))
                .andExpect(jsonPath("$.data[0].waitlistedBookings").value(1));
    }

    @Test
    void resourceUsageShouldReturnPayload() throws Exception {
        dashboardFacade.resourceUsageResponses = List.of(new ResourceUsageMetricResponse(UUID.randomUUID(), 8, 6, 1, 1, 420));

        mockMvc.perform(get("/api/v1/dashboard/resources/usage")
                        .param("limit", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalBookings").value(8))
                .andExpect(jsonPath("$.data[0].bookedMinutes").value(420));
    }

    @Test
    void popularResourcesShouldReturnPayload() throws Exception {
        dashboardFacade.popularResourcesResponses = List.of(new ResourcePopularityResponse(
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

        mockMvc.perform(get("/api/v1/analytics/dashboard/resources/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resourceName").value("Room A"))
                .andExpect(jsonPath("$.data[0].popularityScore").value(97.5));
    }

    @Test
    void popularResourcesShouldAcceptValidLimitParameter() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard/resources/popular").param("limit", "200"))
                .andExpect(status().isOk());
    }

    @Test
    void popularResourcesShouldRejectTooLargeLimit() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        var violations = validator.validate(new LimitRequest());

        var response = handler.handleConstraintViolation(new jakarta.validation.ConstraintViolationException(violations));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        org.assertj.core.api.Assertions.assertThat(response.getBody().details())
                .anyMatch(detail -> detail.contains("limit") && detail.contains("must be less than or equal to 200"));
    }

    @Test
    void refreshShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/admin/resource-popularity/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));

        org.assertj.core.api.Assertions.assertThat(resourcePopularityRefreshService.refreshed).isTrue();
    }

    static class StubDashboardFacade extends DashboardFacade {

        private DashboardOverviewResponse overviewResponse = new DashboardOverviewResponse(0, 0, 0, 0, 0, 0, 0, 0);
        private List<EventRegistrationMetricResponse> eventRegistrationResponses = List.of();
        private List<ResourceUsageMetricResponse> resourceUsageResponses = List.of();
        private List<ResourcePopularityResponse> popularResourcesResponses = List.of();

        StubDashboardFacade() {
            super(null, Runnable::run);
        }

        @Override
        public DashboardOverviewResponse overview(OffsetDateTime from, OffsetDateTime to) {
            return overviewResponse;
        }

        @Override
        public List<EventRegistrationMetricResponse> eventRegistrations(OffsetDateTime from, OffsetDateTime to, int limit) {
            return eventRegistrationResponses;
        }

        @Override
        public List<ResourceUsageMetricResponse> resourceUsage(OffsetDateTime from, OffsetDateTime to, int limit) {
            return resourceUsageResponses;
        }

        @Override
        public List<ResourcePopularityResponse> popularResources(int limit) {
            return popularResourcesResponses;
        }
    }

    static final class LimitRequest {
        @jakarta.validation.constraints.Max(200)
        private final int limit = 201;
    }

    static class StubResourcePopularityRefreshService extends ResourcePopularityRefreshService {

        private boolean refreshed;

        StubResourcePopularityRefreshService() {
            super(null, null);
        }

        @Override
        public void refresh() {
            refreshed = true;
        }
    }
}
