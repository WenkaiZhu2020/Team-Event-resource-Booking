package com.teamresource.resource.service;

import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.ResourceType;
import com.teamresource.resource.service.policy.ResourcePolicyDefaultsFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcePolicyDefaultsFactoryTest {

    private final ResourcePolicyDefaultsFactory factory = new ResourcePolicyDefaultsFactory();

    @Test
    void shouldProvideFacilityDefaults() {
        var defaults = factory.create(ResourceType.FACILITY);

        assertThat(defaults.approvalMode()).isEqualTo(ApprovalMode.ADMIN_APPROVAL);
        assertThat(defaults.allowWaitlist()).isFalse();
        assertThat(defaults.maxBookingDurationMinutes()).isEqualTo(720);
    }
}
