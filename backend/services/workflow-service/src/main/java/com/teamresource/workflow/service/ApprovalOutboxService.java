package com.teamresource.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamresource.workflow.domain.OutboxStatus;
import com.teamresource.workflow.infra.persistence.ApprovalRequestEntity;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxEntity;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApprovalOutboxService {

    private final WorkflowOutboxRepository workflowOutboxRepository;
    private final ObjectMapper objectMapper;

    public ApprovalOutboxService(WorkflowOutboxRepository workflowOutboxRepository, ObjectMapper objectMapper) {
        this.workflowOutboxRepository = workflowOutboxRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String eventType, ApprovalRequestEntity entity) {
        WorkflowOutboxEntity message = new WorkflowOutboxEntity();
        message.setMessageId(UUID.randomUUID());
        message.setAggregateType("APPROVAL_REQUEST");
        message.setAggregateId(entity.getApprovalId());
        message.setEventType(eventType);
        message.setPayload(toJson(Map.of(
                "approvalId", entity.getApprovalId(),
                "targetType", entity.getTargetType().name(),
                "targetId", entity.getTargetId(),
                "status", entity.getStatus().name(),
                "approverId", entity.getApproverId(),
                "requesterId", entity.getRequesterId(),
                "updatedAt", entity.getUpdatedAt() == null ? entity.getCreatedAt() : entity.getUpdatedAt()
        )));
        message.setStatus(OutboxStatus.PENDING);
        message.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        workflowOutboxRepository.save(message);
    }

    public DomainEventMessage toDomainEvent(WorkflowOutboxEntity entity) {
        return new DomainEventMessage(
                entity.getMessageId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                toJsonNode(entity.getPayload()),
                entity.getCreatedAt()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize workflow outbox payload");
        }
    }

    private JsonNode toJsonNode(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize workflow outbox payload");
        }
    }

    public record DomainEventMessage(
            UUID messageId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            JsonNode payload,
            OffsetDateTime occurredAt
    ) {
    }
}
