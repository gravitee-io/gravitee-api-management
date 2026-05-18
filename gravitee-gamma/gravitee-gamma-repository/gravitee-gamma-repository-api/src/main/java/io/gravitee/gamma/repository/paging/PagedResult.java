/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.repository.paging;

import java.util.List;
import java.util.function.Function;

/**
 * Single page of a paginated result.
 *
 * <p>{@code total} is the filtered total across all pages (not the size of
 * {@code data}); UI uses it to render the pagination footer and to decide
 * when to disable the "next" button. {@code page}/{@code perPage} echo the
 * request so the consumer doesn't need to keep the original {@link Pageable}
 * around to render itself.
 */
public record PagedResult<T>(List<T> data, long total, int page, int perPage) {
    public PagedResult {
        if (data == null) throw new IllegalArgumentException("data must not be null");
        if (total < 0) throw new IllegalArgumentException("total must not be negative, got " + total);
        if (page < 1) throw new IllegalArgumentException("page must be >= 1, got " + page);
        if (perPage < 1) throw new IllegalArgumentException("perPage must be >= 1, got " + perPage);
        data = List.copyOf(data);
    }

    public static <T> PagedResult<T> empty(Pageable pageable) {
        return new PagedResult<>(List.of(), 0L, pageable.page(), pageable.perPage());
    }

    /**
     * Build a paged view over an already-loaded list (in-memory paging).
     * The repository default implementation uses this as its fallback;
     * native adapters override with {@code skip/limit + count}.
     */
    public static <T> PagedResult<T> of(List<T> all, Pageable pageable) {
        int total = all.size();
        int skip = pageable.skip();
        if (skip >= total) {
            return new PagedResult<>(List.of(), total, pageable.page(), pageable.perPage());
        }
        int end = Math.min(skip + pageable.perPage(), total);
        return new PagedResult<>(all.subList(skip, end), total, pageable.page(), pageable.perPage());
    }

    /** Map each element to a different type without losing the paging metadata. */
    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(data.stream().map(mapper).toList(), total, page, perPage);
    }
}
