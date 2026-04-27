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
package io.gravitee.apim.core.tracing.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.tracing.model.Trace;
import io.gravitee.apim.core.tracing.query_service.TracingQueryService;

@UseCase
public class GetTraceUseCase {

    private final TracingQueryService tracingQueryService;

    public GetTraceUseCase(TracingQueryService tracingQueryService) {
        this.tracingQueryService = tracingQueryService;
    }

    public record Input(AuditInfo auditInfo, String traceId) {}

    public record Output(Trace trace) {}

    public Output execute(Input input) {
        // TODO multi-tenancy: assert trace belongs to input.auditInfo().environmentId() once backend supports it.
        var trace = tracingQueryService
            .getTrace(input.traceId())
            .orElseThrow(() -> new NotFoundDomainException("Trace [" + input.traceId() + "] not found"));
        return new Output(trace);
    }
}
