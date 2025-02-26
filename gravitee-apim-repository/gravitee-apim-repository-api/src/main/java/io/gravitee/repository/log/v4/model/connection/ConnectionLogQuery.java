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
package io.gravitee.repository.log.v4.model.connection;

import io.gravitee.common.http.HttpMethod;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ConnectionLogQuery {

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private int page = 1;

    private Filter filter;

    @Data
    @Builder
    public static class Filter {

        private Long from;
        private Long to;
        private Set<String> applicationIds;
        private Set<String> planIds;
        private Set<HttpMethod> methods;
        private Set<Integer> statuses;
        private Set<String> entrypointIds;
        private Set<String> apiIds;
        private Set<String> requestIds;
        private Set<String> transactionIds;
        private String uri;
    }
}
