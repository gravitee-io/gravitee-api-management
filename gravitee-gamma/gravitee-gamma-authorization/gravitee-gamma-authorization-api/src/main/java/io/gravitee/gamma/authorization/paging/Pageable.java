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
package io.gravitee.gamma.authorization.paging;

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

    public static Pageable fromQuery(Integer page, Integer perPage) {
        if (page == null && perPage == null) {
            return firstPage();
        }
        return of(page == null ? 1 : page, perPage == null ? DEFAULT_PER_PAGE : perPage);
    }

    public static Pageable unbounded() {
        return new Pageable(1, MAX_PER_PAGE);
    }

    public int skip() {
        return (page - 1) * perPage;
    }
}
