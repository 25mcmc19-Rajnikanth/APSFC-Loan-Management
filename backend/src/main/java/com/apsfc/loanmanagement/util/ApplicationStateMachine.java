package com.apsfc.loanmanagement.util;

import com.apsfc.loanmanagement.enums.ApplicationStatus;
import com.apsfc.loanmanagement.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for legal LoanApplication status transitions.
 * Every application-status mutation must go through enforce(from, to);
 * this guarantees no code path can put an application into an illegal state.
 */
@Component
public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(ApplicationStatus.DRAFT,
                EnumSet.of(ApplicationStatus.SUBMITTED, ApplicationStatus.CANCELLED));
        ALLOWED.put(ApplicationStatus.SUBMITTED,
                EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED));
        ALLOWED.put(ApplicationStatus.UNDER_REVIEW,
                EnumSet.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED));
        ALLOWED.put(ApplicationStatus.APPROVED,
                EnumSet.of(ApplicationStatus.DISBURSED));
    }

    public void enforce(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowed = ALLOWED.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStateTransitionException(
                    "Cannot change application status from " + from + " to " + to);
        }
    }

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowed = ALLOWED.get(from);
        return allowed != null && allowed.contains(to);
    }
}