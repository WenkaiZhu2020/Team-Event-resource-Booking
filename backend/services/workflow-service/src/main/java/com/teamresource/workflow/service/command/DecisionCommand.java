package com.teamresource.workflow.service.command;

import java.util.UUID;

public interface DecisionCommand {

    UUID approvalId();

    UUID actorId();

    boolean admin();

    String note();
}
