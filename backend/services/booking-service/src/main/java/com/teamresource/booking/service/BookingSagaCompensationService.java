package com.teamresource.booking.service;

import com.teamresource.booking.infra.persistence.BookingEntity;
import com.teamresource.booking.infra.persistence.BookingRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSagaCompensationService {

    private final BookingRepository bookingRepository;
    private final BookingTransitionService bookingTransitionService;

    public BookingSagaCompensationService(
            BookingRepository bookingRepository,
            BookingTransitionService bookingTransitionService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingTransitionService = bookingTransitionService;
    }

    @Transactional
    public void compensateResourceAllocationFailure(UUID bookingId, String reason) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return;
        }
        bookingTransitionService.compensateResourceAllocationFailure(booking, reason);
    }

    @Transactional
    public void compensateWorkflowApprovalRejected(UUID bookingId, String reason) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return;
        }
        bookingTransitionService.compensateWorkflowApprovalRejected(booking, reason);
    }
}
