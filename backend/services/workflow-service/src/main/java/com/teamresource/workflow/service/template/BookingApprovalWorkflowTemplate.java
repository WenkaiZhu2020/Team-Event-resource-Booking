package com.teamresource.workflow.service.template;

import com.teamresource.workflow.api.dto.CreateApprovalRequest;
import org.springframework.stereotype.Component;

@Component
public class BookingApprovalWorkflowTemplate extends ApprovalWorkflowTemplate {

    @Override
    protected String resolveApprovalType(CreateApprovalRequest request) {
        return request.approvalType() == null || request.approvalType().isBlank()
                ? "RESOURCE_BOOKING_APPROVAL"
                : request.approvalType().trim();
    }

    @Override
    protected String resolveTitle(CreateApprovalRequest request) {
        return request.title() == null || request.title().isBlank()
                ? "Booking approval request"
                : request.title().trim();
    }
}
