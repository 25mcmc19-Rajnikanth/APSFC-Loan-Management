package com.apsfc.loanmanagement.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Builds Pageable safely: bounds page/size, and only allows sort fields from
 * a whitelist (unknown fields fall back to the default).
 */
public final class PageableUtil {

    private PageableUtil() {
    }

    public static Pageable of(int page, int size, String sort, String dir, String defaultSort, Set<String> allowed) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String field = allowed.contains(sort) ? sort : defaultSort;
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}