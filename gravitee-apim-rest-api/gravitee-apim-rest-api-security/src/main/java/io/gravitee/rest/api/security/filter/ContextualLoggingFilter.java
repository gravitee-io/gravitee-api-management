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
package io.gravitee.rest.api.security.filter;

import static java.util.function.Predicate.not;

import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

@CustomLog
@RequiredArgsConstructor
public class ContextualLoggingFilter extends GenericFilterBean {

    static final String ORG_ID_KEY = "orgId";
    static final String ENV_ID_KEY = "envId";
    static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    static final String CORRELATION_ID_KEY = "correlationId";
    static final String TRACE_PARENT_KEY = "traceParent";
    static final String TRACE_PARENT_HEADER = "traceparent";
    static final String APIS_SEGMENT = "apis";
    static final String APPLICATIONS_SEGMENT = "applications";
    static final String API_ID_KEY = "apiId";
    static final String APP_ID_KEY = "appId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        MDC.clear();
        String organization = GraviteeContext.getCurrentOrganization();

        if (organization != null) {
            MDC.put(ORG_ID_KEY, organization);
        }
        String environment = GraviteeContext.getCurrentEnvironment();

        if (environment != null) {
            MDC.put(ENV_ID_KEY, environment);
        }

        if (request instanceof HttpServletRequest httpServletRequest) {
            // the de-facto standard for distributed tracing
            var correlationId = httpServletRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId != null) {
                MDC.put(CORRELATION_ID_KEY, correlationId);
            }

            // the trace context standard header (see https://www.w3.org/TR/trace-context)
            var traceParent = httpServletRequest.getHeader(TRACE_PARENT_HEADER);
            if (traceParent != null) {
                MDC.put(TRACE_PARENT_KEY, traceParent);
            }

            String[] segments = splitPath(httpServletRequest);
            getSegmentValue(APIS_SEGMENT, segments).ifPresent(value -> MDC.put(API_ID_KEY, value));
            getSegmentValue(APPLICATIONS_SEGMENT, segments).ifPresent(value -> MDC.put(APP_ID_KEY, value));
        }

        log.debug("Contextual logging is on for organization: [{}] and environment: [{}]", organization, environment);

        filterChain.doFilter(request, response);
    }

    private static String[] splitPath(final HttpServletRequest httpServletRequest) {
        String pathInfo = httpServletRequest.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) {
            return new String[0];
        }
        return pathInfo.split("/");
    }

    private static Optional<String> getSegmentValue(final String segmentName, final String[] segments) {
        for (int i = 0; i < segments.length - 1; i++) {
            if (segmentName.equals(segments[i])) {
                return Optional.of(segments[i + 1]).filter(not(String::isEmpty));
            }
        }
        return Optional.empty();
    }
}
