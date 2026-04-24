package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;
import com.teamresource.resource.domain.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class ResourcePolicyDefaultsFactory {

    public ResourcePolicyDefaults create(ResourceType type) {
        return switch (type) {
            case ROOM -> new ResourcePolicyDefaults(ApprovalMode.MANAGER_APPROVAL, true, 240, 60);
            case EQUIPMENT -> new ResourcePolicyDefaults(ApprovalMode.MANAGER_APPROVAL, true, 480, 30);
            case FACILITY -> new ResourcePolicyDefaults(ApprovalMode.ADMIN_APPROVAL, false, 720, 90);
        };
    }
}
