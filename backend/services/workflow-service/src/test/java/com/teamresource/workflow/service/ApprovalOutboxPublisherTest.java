package com.teamresource.workflow.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.teamresource.workflow.domain.OutboxStatus;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxEntity;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalOutboxPublisherTest {

    @Mock
    private WorkflowOutboxRepository workflowOutboxRepository;

    private RecordingRabbitTemplate rabbitTemplate;
    private ApprovalOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = new RecordingRabbitTemplate();
        publisher = new ApprovalOutboxPublisher(
                workflowOutboxRepository,
                new ApprovalOutboxService(workflowOutboxRepository, JsonMapper.builder().findAndAddModules().build()),
                rabbitTemplate
        );
    }

    @Test
    void publishPendingMessagesShouldSendAndMarkPublished() {
        WorkflowOutboxEntity entity = new WorkflowOutboxEntity();
        entity.setMessageId(UUID.randomUUID());
        entity.setAggregateType("APPROVAL_REQUEST");
        entity.setAggregateId(UUID.randomUUID());
        entity.setEventType("workflow.approval.approved");
        entity.setPayload("{\"status\":\"APPROVED\"}");
        entity.setStatus(OutboxStatus.PENDING);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        when(workflowOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(entity));
        when(workflowOutboxRepository.save(org.mockito.ArgumentMatchers.any(WorkflowOutboxEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        publisher.publishPendingMessages();

        org.assertj.core.api.Assertions.assertThat(rabbitTemplate.lastExchange).isEqualTo("team-resource.events");
        org.assertj.core.api.Assertions.assertThat(rabbitTemplate.lastRoutingKey).isEqualTo("workflow.approval.approved");
        org.assertj.core.api.Assertions.assertThat(rabbitTemplate.lastPayload).isNotNull();
        org.assertj.core.api.Assertions.assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    private static class RecordingRabbitTemplate extends RabbitTemplate {
        private String lastExchange;
        private String lastRoutingKey;
        private Object lastPayload;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object) {
            this.lastExchange = exchange;
            this.lastRoutingKey = routingKey;
            this.lastPayload = object;
        }
    }
}
