package com.apsfc.loanmanagement.dto.request;

import com.apsfc.loanmanagement.enums.Recommendation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * LOAN_OFFICER advisory recommendation assigned during review.
 */
public record RecommendRequest(
        @NotNull(message = "Recommendation is required (APPROVE or REJECT)")
        Recommendation recommendation,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}