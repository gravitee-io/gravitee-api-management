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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.function.Supplier;

/**
 * Runs an APIM call with the request context bound to the environment of the question. The MCP request itself
 * carries only the organization, and some services down the call read {@link GraviteeContext} rather than their
 * arguments.
 */
final class ApimContext {

    private ApimContext() {}

    static <T> T run(AuditInfo auditInfo, Supplier<T> call) {
        var previous = GraviteeContext.getExecutionContext();
        GraviteeContext.fromExecutionContext(new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()));
        try {
            return call.get();
        } finally {
            GraviteeContext.fromExecutionContext(previous);
        }
    }
}
