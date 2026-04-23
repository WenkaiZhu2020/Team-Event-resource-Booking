package com.teamresource.auth.service;

import java.util.Set;
import java.util.UUID;

public interface UserProvisioningClient {
    void provisionUser(UUID userId, String email, String displayName, String timezone, Set<String> roles);
}
