package com.teamresource.booking.api.controller;

import com.teamresource.booking.api.dto.ApiResponse;
import com.teamresource.booking.api.dto.BookingResponse;
import com.teamresource.booking.api.dto.CancelBookingRequest;
import com.teamresource.booking.api.dto.CreateBookingRequest;
import com.teamresource.booking.api.dto.PageResponse;
import com.teamresource.booking.api.dto.RejectBookingRequest;
import com.teamresource.booking.api.dto.WaitlistEntryResponse;
import com.teamresource.booking.application.command.ApproveBookingCommand;
import com.teamresource.booking.application.command.CancelBookingCommand;
import com.teamresource.booking.application.command.CreateBookingCommand;
import com.teamresource.booking.application.command.RejectBookingCommand;
import com.teamresource.booking.application.facade.BookingFacade;
import com.teamresource.booking.common.error.ApiException;
import com.teamresource.booking.domain.model.BookingSearchCriteria;
import com.teamresource.booking.domain.model.BookingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/bookings")
@Profile("stage2-layered-inactive")
public class BookingController {

    private final BookingFacade bookingFacade;

    public BookingController(BookingFacade bookingFacade) {
        this.bookingFacade = bookingFacade;
    }

    @PostMapping
    public ApiResponse<BookingResponse> create(
            Principal principal,
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        UUID userId = parsePrincipalUserId(principal);
        BookingResponse response = bookingFacade.create(new CreateBookingCommand(
                idempotencyKey,
                userId,
                request.eventId(),
                request.resourceId(),
                request.startAt(),
                request.endAt()
        ));
        return ApiResponse.of(response);
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancel(
            @PathVariable UUID bookingId,
            Principal principal,
            Authentication authentication,
            @Valid @RequestBody CancelBookingRequest request
    ) {
        UUID userId = parsePrincipalUserId(principal);
        boolean admin = hasRole(authentication, "ROLE_ADMIN");

        BookingResponse response = bookingFacade.cancel(new CancelBookingCommand(
                bookingId,
                userId,
                admin,
                request.reason()
        ));
        return ApiResponse.of(response);
    }

    @PostMapping("/{bookingId}/approve")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<BookingResponse> approve(@PathVariable UUID bookingId, Principal principal) {
        BookingResponse response = bookingFacade.approve(new ApproveBookingCommand(bookingId, parsePrincipalUserId(principal)));
        return ApiResponse.of(response);
    }

    @PostMapping("/{bookingId}/reject")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<BookingResponse> reject(
            @PathVariable UUID bookingId,
            Principal principal,
            @Valid @RequestBody RejectBookingRequest request
    ) {
        BookingResponse response = bookingFacade.reject(new RejectBookingCommand(bookingId, parsePrincipalUserId(principal), request.reason()));
        return ApiResponse.of(response);
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> byId(@PathVariable UUID bookingId, Principal principal, Authentication authentication) {
        BookingResponse response = bookingFacade.byId(bookingId);
        UUID currentUser = parsePrincipalUserId(principal);
        boolean admin = hasRole(authentication, "ROLE_ADMIN");
        boolean manager = hasRole(authentication, "ROLE_RESOURCE_MANAGER");
        if (!admin && !manager && !response.userId().equals(currentUser)) {
            throw new ApiException("BOOKING_FORBIDDEN", HttpStatus.FORBIDDEN, "Cannot access booking of another user");
        }
        return ApiResponse.of(response);
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<BookingResponse>> myBookings(
            Principal principal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        UUID userId = parsePrincipalUserId(principal);
        var result = bookingFacade.list(
                new BookingSearchCriteria(userId, null, status, from, to),
                PageRequest.of(page, size)
        );
        return ApiResponse.of(PageResponse.fromPage(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<PageResponse<BookingResponse>> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        var result = bookingFacade.list(
                new BookingSearchCriteria(userId, resourceId, status, from, to),
                PageRequest.of(page, size)
        );
        return ApiResponse.of(PageResponse.fromPage(result));
    }

    @GetMapping("/waitlist")
    @PreAuthorize("hasAnyRole('RESOURCE_MANAGER','ADMIN')")
    public ApiResponse<java.util.List<WaitlistEntryResponse>> waitlist(@RequestParam UUID resourceId) {
        return ApiResponse.of(bookingFacade.listWaitlist(resourceId));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    private UUID parsePrincipalUserId(Principal principal) {
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ex) {
            throw new ApiException("AUTH_PRINCIPAL_INVALID", HttpStatus.UNAUTHORIZED, "Invalid principal user identifier");
        }
    }
}
