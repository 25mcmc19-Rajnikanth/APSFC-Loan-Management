package com.apsfc.loanmanagement.enums;

/**
 * States of a LoanApplication. Valid transitions are enforced exclusively by
 * the ApplicationStateMachine (never mutated ad-hoc).
 */
public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED,
    DISBURSED
}