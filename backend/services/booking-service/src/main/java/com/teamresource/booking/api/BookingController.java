package com.teamresource.booking.api;

import com.teamresource.booking.api.dto.ApiResponse;
import com.teamresource.booking.api.dto.BookingDecisionRequest;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.service.BookingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> create(
            Principal principal,
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.of(bookingService.create(parsePrincipal(principal), request, idempotencyKey));
    }

    @GetMapping("/me")
    public ApiResponse<List<BookingResponse>> myBookings(
            Principal principal,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.of(bookingService.myBookings(parsePrincipal(principal), status));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> byId(
            @PathVariable UUID bookingId,
            Principal principal,
            Authentication authentication
    ) {
        return ApiResponse.of(bookingService.byId(bookingId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancel(
            @PathVariable UUID bookingId,
            Principal principal,
            Authentication authentication
    ) {
        return ApiResponse.of(bookingService.cancel(bookingId, parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @GetMapping("/approvals/pending")
    public ApiResponse<List<BookingResponse>> pendingApprovals(Principal principal, Authentication authentication) {
        return ApiResponse.of(bookingService.pendingApprovals(parsePrincipal(principal), hasRole(authentication, "ROLE_ADMIN")));
    }

    @PostMapping("/{bookingId}/approve")
    public ApiResponse<BookingResponse> approve(
            @PathVariable UUID bookingId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody(required = false) BookingDecisionRequest request
    ) {
        BookingDecisionRequest safeRequest = request == null ? new BookingDecisionRequest(null) : request;
        return ApiResponse.of(bookingService.approve(
                bookingId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                safeRequest
        ));
    }

    @PostMapping("/{bookingId}/reject")
    public ApiResponse<BookingResponse> reject(
            @PathVariable UUID bookingId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody(required = false) BookingDecisionRequest request
    ) {
        BookingDecisionRequest safeRequest = request == null ? new BookingDecisionRequest(null) : request;
        return ApiResponse.of(bookingService.reject(
                bookingId,
                parsePrincipal(principal),
                hasRole(authentication, "ROLE_ADMIN"),
                safeRequest
        ));
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
}
