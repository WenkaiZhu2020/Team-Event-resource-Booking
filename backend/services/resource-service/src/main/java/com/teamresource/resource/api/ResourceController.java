package com.teamresource.resource.api;

import com.teamresource.resource.api.dto.ApiResponse;
import com.teamresource.resource.api.dto.MaintenanceSlotRequest;
import com.teamresource.resource.api.dto.MaintenanceSlotResponse;
import com.teamresource.resource.api.dto.ResourceResponse;
import com.teamresource.resource.api.dto.UpsertResourceRequest;
import com.teamresource.resource.service.ResourceService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResourceResponse> create(
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody UpsertResourceRequest request
    ) {
        return ApiResponse.of(resourceService.create(
                parsePrincipal(principal),
                hasAnyRole(authentication, "ROLE_RESOURCE_MANAGER", "ROLE_ADMIN"),
                request
        ));
    }

    @PutMapping("/{resourceId}")
    public ApiResponse<ResourceResponse> update(
            @PathVariable UUID resourceId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody UpsertResourceRequest request
    ) {
        return ApiResponse.of(resourceService.update(
                resourceId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                request
        ));
    }

    @GetMapping
    public ApiResponse<List<ResourceResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean requiresApproval
    ) {
        return ApiResponse.of(resourceService.listCatalog(query, type, status, requiresApproval));
    }

    @GetMapping("/me")
    public ApiResponse<List<ResourceResponse>> myResources(Principal principal) {
        return ApiResponse.of(resourceService.myResources(parsePrincipal(principal)));
    }

    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceResponse> byId(@PathVariable UUID resourceId) {
        return ApiResponse.of(resourceService.byId(resourceId));
    }

    @PostMapping("/{resourceId}/activate")
    public ApiResponse<ResourceResponse> activate(@PathVariable UUID resourceId, Principal principal, Authentication authentication) {
        return ApiResponse.of(resourceService.activate(resourceId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{resourceId}/deactivate")
    public ApiResponse<ResourceResponse> deactivate(@PathVariable UUID resourceId, Principal principal, Authentication authentication) {
        return ApiResponse.of(resourceService.deactivate(resourceId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{resourceId}/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MaintenanceSlotResponse> addMaintenanceSlot(
            @PathVariable UUID resourceId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody MaintenanceSlotRequest request
    ) {
        return ApiResponse.of(resourceService.addMaintenanceSlot(
                resourceId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                request
        ));
    }

    @DeleteMapping("/{resourceId}/maintenance/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMaintenanceSlot(
            @PathVariable UUID resourceId,
            @PathVariable UUID slotId,
            Principal principal,
            Authentication authentication
    ) {
        resourceService.removeMaintenanceSlot(resourceId, slotId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN"));
    }

    private UUID parsePrincipal(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }
        return false;
    }
}
