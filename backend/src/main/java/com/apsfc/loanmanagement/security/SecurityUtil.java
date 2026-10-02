package com.apsfc.loanmanagement.security;

import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper to read the authenticated user from the SecurityContext.
 * Controllers and services use this instead of touching Spring internals.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "You must be authenticated to perform this operation");
        }
        return principal.getUser();
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }
}