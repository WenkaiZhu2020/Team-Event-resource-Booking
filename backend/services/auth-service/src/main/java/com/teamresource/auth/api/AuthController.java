package com.teamresource.auth.api;

import com.teamresource.auth.api.dto.ApiResponse;
import com.teamresource.auth.api.dto.AuthResponse;
import com.teamresource.auth.api.dto.LoginRequest;
import com.teamresource.auth.api.dto.RegisterRequest;
import com.teamresource.auth.api.dto.UserResponse;
import com.teamresource.auth.service.AuthApplicationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authApplicationService.register(request.email(), request.password()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authApplicationService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Principal principal) {
        return ApiResponse.of(authApplicationService.me(principal));
    }
}
