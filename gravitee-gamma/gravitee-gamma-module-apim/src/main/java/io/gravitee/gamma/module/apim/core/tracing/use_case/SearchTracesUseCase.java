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
package io.gravitee.gamma.module.apim.core.tracing.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.node.api.opentelemetry.query.TracingQueryService;
import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSearchCriteria;
import java.util.List;

@UseCase
public class SearchTracesUseCase {

    private final TracingQueryService tracingQueryService;

    public SearchTracesUseCase(TracingQueryService tracingQueryService) {
        this.tracingQueryService = tracingQueryService;
    }

    public record Input(TraceSearchCriteria criteria) {}

    public record Output(List<Trace> traces) {}

    public Output execute(Input input) {
        // TODO multi-tenancy: add environmentId to Input and scope via TraceQL once backend supports it.
        return new Output(tracingQueryService.searchTraces(input.criteria()).blockingGet());
    }
}
