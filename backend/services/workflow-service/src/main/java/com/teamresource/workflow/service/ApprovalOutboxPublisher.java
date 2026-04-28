package com.teamresource.workflow.service;

import com.teamresource.workflow.domain.OutboxStatus;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxEntity;
import com.teamresource.workflow.infra.persistence.WorkflowOutboxRepository;
import com.teamresource.workflow.config.WorkflowServiceConfiguration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ApprovalOutboxPublisher {

    private final WorkflowOutboxRepository workflowOutboxRepository;
    private final ApprovalOutboxService approvalOutboxService;
    private final RabbitTemplate rabbitTemplate;

    public ApprovalOutboxPublisher(
            WorkflowOutboxRepository workflowOutboxRepository,
            ApprovalOutboxService approvalOutboxService,
            RabbitTemplate rabbitTemplate
    ) {
        this.workflowOutboxRepository = workflowOutboxRepository;
        this.approvalOutboxService = approvalOutboxService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    public void publishPendingMessages() {
        List<WorkflowOutboxEntity> messages = workflowOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (WorkflowOutboxEntity message : messages) {
            try {
                rabbitTemplate.convertAndSend(
                        WorkflowServiceConfiguration.WORKFLOW_EVENTS_EXCHANGE,
                        message.getEventType(),
                        approvalOutboxService.toDomainEvent(message)
                );
                message.setStatus(OutboxStatus.PUBLISHED);
                message.setPublishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            } catch (Exception ex) {
                message.setStatus(OutboxStatus.FAILED);
            }
            workflowOutboxRepository.save(message);
        }
    }
}
