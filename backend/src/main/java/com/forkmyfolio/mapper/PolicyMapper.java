package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.PolicyDto;
import com.forkmyfolio.model.Policy;
import org.springframework.stereotype.Component;

/**
 * Mapper for Policy related objects.
 */
@Component
public class PolicyMapper {

    public PolicyDto toDto(Policy policy) {
        if (policy == null) {
            return null;
        }
        return new PolicyDto(policy.getVersion(), policy.getTitle(), policy.getContent());
    }
}
