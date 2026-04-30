package com.teamresource.user.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record SyncRolesRequest(
        @NotEmpty Set<String> roles,
        String assignedBy
) {
}
