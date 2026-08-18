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
package io.gravitee.repository.log.v4.model.authz;

import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * @author GraviteeSource Team
 */
@Data
@Builder
public class AuthzDecisionLogQuery {

    public static final int MAX_SIZE = 1_000;

    private Set<String> apiIds;
    private Long from;
    private Long to;
    private Set<String> decisions;
    private Set<String> subjectIds;
    private Set<String> actions;
    private Set<String> resourceIds;
    private Set<String> callers;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private int page = 1;

    public void validate() {
        Objects.requireNonNull(apiIds, "apiIds");
        if (apiIds.isEmpty()) {
            throw new IllegalArgumentException("apiIds must not be empty");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1, was " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1, was " + size);
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_SIZE + ", was " + size);
        }
        if (from != null && to != null && from > to) {
            throw new IllegalArgumentException("from must be <= to, was from=" + from + " to=" + to);
        }
    }
}
