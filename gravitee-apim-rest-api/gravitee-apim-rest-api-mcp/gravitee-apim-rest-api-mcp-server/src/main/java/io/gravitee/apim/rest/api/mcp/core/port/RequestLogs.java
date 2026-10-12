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
package io.gravitee.apim.rest.api.mcp.core.port;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import java.util.List;

/**
 * Logged requests of the APIs the caller can see.
 */
public interface RequestLogs {
    /** Requests answered with a 4xx or 5xx status, newest first. */
    Page failures(AuditInfo caller, TimeWindow window, Query query, int limit);

    /** A null member does not restrict. */
    record Query(String apiId, StatusGroup statusGroup, String errorKey) {}

    record Page(long total, List<FailedRequest> requests) {}
}
