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
package io.gravitee.gamma.authorization.service;

/**
 * Server-side paging request.
 *
 * <p>1-based {@code page} (per the REST convention exposed to the UI),
 * {@code perPage} bounded to {@link #MAX_PER_PAGE} so a hostile or buggy
 * client cannot ask the backend for an unbounded result set. Use
 * {@link #firstPage()} for the default UI listing and
 * {@link #unbounded()} when a caller genuinely needs the entire result —
 * SCIM reconcile is one such case and is allowed to fetch the whole env
 * because the alternative is treating page 2+ entities as orphans.
 */
public record Pageable(int page, int perPage) {
    public static final int DEFAULT_PER_PAGE = 25;
    public static final int MAX_PER_PAGE = 1000;

    public Pageable {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1, got " + page);
        }
        if (perPage < 1) {
            throw new IllegalArgumentException("perPage must be >= 1, got " + perPage);
        }
        if (perPage > MAX_PER_PAGE) {
            throw new IllegalArgumentException("perPage must be <= " + MAX_PER_PAGE + ", got " + perPage);
        }
    }

    public static Pageable firstPage() {
        return new Pageable(1, DEFAULT_PER_PAGE);
    }

    public static Pageable of(int page, int perPage) {
        return new Pageable(page, perPage);
    }

    /**
     * A pageable wide enough to fit any single env's authz data set
     * ({@link #MAX_PER_PAGE} = 1000 records). Used by callers that need to
     * iterate the whole result (SCIM reconcile, schema invalidation) but
     * still want to flow through the paginated port for consistency.
     */
    public static Pageable unbounded() {
        return new Pageable(1, MAX_PER_PAGE);
    }

    /** 0-based skip offset for repository implementations. */
    public int skip() {
        return (page - 1) * perPage;
    }
}
