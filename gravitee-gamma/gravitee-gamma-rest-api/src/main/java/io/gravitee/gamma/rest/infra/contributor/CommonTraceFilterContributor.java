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
package io.gravitee.gamma.rest.infra.contributor;

import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import java.util.List;

/**
 * Built-in cross-module trace filters that ship with the gamma host application. Discovered by
 * {@link io.gravitee.gamma.rest.infra.adapter.SpiTraceFilterRegistry} via
 * {@link java.util.ServiceLoader} — see
 * {@code META-INF/services/io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor}.
 *
 * <p><b>Slim cut scope.</b> Only the filters + operators the slim search endpoint's
 * {@link io.gravitee.gamma.rest.core.tracing.use_case.SearchTraceFilterTranslator} can actually
 * translate to an ES query end-to-end appear here. Discovery and search MUST stay in sync — a
 * filter exposed here that the translator doesn't handle would return 400 from search, which is
 * misleading. The trim is deliberate.
 *
 * <h2>What's intentionally not yet exposed</h2>
 * <ul>
 *   <li><b>{@code STATUS}</b> — backed by the top-level OTel {@code status.code} field, not a span
 *       attribute. The ES adapter reads it and promotes it into {@code attributes['otel.status_code']}
 *       on the in-memory domain model as a convenience, but it's NOT stored at that path in ES, so
 *       routing a filter through {@code attributeFilters} would query a non-existent field and
 *       return zero matches. Comes back with the follow-up PR that lets the search criteria carry
 *       top-level-field clauses.</li>
 *   <li><b>{@code HAS_ERROR}</b> — trace-wide computed property; needs an aggregation, not a
 *       term query. Follow-up PR.</li>
 *   <li><b>{@code DURATION_NANOS}</b> — top-level OTel {@code duration} field with range operators
 *       ({@code gte} / {@code lte}); needs range-query rendering. Follow-up PR.</li>
 *   <li><b>{@code OPERATION_NAME}</b> — top-level OTel {@code name} field; not an attribute.
 *       Needs the search criteria to grow a top-level-field slot. Follow-up PR.</li>
 *   <li><b>{@code SPAN_KIND}</b> — top-level OTel {@code kind} field; same blocker as
 *       {@code OPERATION_NAME}.</li>
 *   <li>{@code IN} / {@code GTE} / {@code LTE} / {@code CONTAINS} operators on the surviving three
 *       filters — translator handles {@code eq} only today. {@code IN} needs the repository SPI's
 *       {@link io.gravitee.repository.tracing.model.TraceSearchCriteria} to take
 *       {@code Map<String, List<String>>} instead of {@code Map<String, String>}; range / contains
 *       need new clause types.</li>
 *   <li><b>{@code SERVICE_NAME}</b> — with the gateway as the sole OTel emitter today every span
 *       carries {@code serviceName="gio-apim-gateway"}, no discriminative signal as a filter chip.
 *       Add when multi-source ingestion lands.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
public class CommonTraceFilterContributor implements TraceFilterContributor {

    @Override
    public String moduleId() {
        // null = cross-module: included in every response regardless of the `module` query param.
        return null;
    }

    @Override
    public List<TraceFilterSpec> getFilters() {
        return COMMON_FILTERS;
    }

    private static final List<TraceFilterSpec> COMMON_FILTERS = List.of(
        new TraceFilterSpec(
            "HTTP_METHOD",
            "HTTP method",
            FilterType.ENUM,
            List.of(FilterOperator.EQ),
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"),
            null
        ),
        new TraceFilterSpec(
            "HTTP_STATUS_CODE",
            "HTTP status code",
            FilterType.NUMBER,
            List.of(FilterOperator.EQ),
            null,
            new TraceFilterSpec.Range(100, 599)
        ),
        new TraceFilterSpec("HTTP_ROUTE", "HTTP route", FilterType.STRING, List.of(FilterOperator.EQ), null, null)
    );
}
