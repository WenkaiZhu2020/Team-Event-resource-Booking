package com.teamresource.resource.service.policy;

import com.teamresource.resource.domain.ApprovalMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicyStrategyFactory {

    private final Map<ApprovalMode, ApprovalPolicyStrategy> strategies;

    public ApprovalPolicyStrategyFactory(List<ApprovalPolicyStrategy> strategies) {
        this.strategies = new EnumMap<>(ApprovalMode.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.supports(), strategy));
    }

    public ApprovalPolicyStrategy get(ApprovalMode approvalMode) {
        return strategies.get(approvalMode);
    }
}
