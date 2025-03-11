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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.portal.rest.model.HttpMethod;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.model.Request;
import io.gravitee.rest.api.portal.rest.model.Response;
import io.gravitee.rest.api.portal.rest.resource.param.ResponseTimeRange;
import io.gravitee.rest.api.portal.rest.resource.param.SearchApplicationLogsParam;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class LogMapper {

    public Log convert(ApplicationRequest applicationRequest) {
        final Log logItem = new Log();
        logItem.setApi(applicationRequest.getApi());
        logItem.setHost(applicationRequest.getHost());
        logItem.setId(applicationRequest.getId());
        logItem.setMetadata(applicationRequest.getMetadata() == null ? null : new HashMap(applicationRequest.getMetadata()));
        logItem.setMethod(HttpMethod.fromValue(applicationRequest.getMethod().name()));
        logItem.setPath(applicationRequest.getPath());
        logItem.setPlan(applicationRequest.getPlan());
        if (applicationRequest.getRequest() != null) {
            logItem.setRequest(
                new Request()
                    .body(applicationRequest.getRequest().getBody())
                    .headers(applicationRequest.getRequest().getHeaders())
                    .method(HttpMethod.fromValue(applicationRequest.getRequest().getMethod().name()))
                    .uri(applicationRequest.getRequest().getUri())
            );
        }
        logItem.setRequestContentLength(applicationRequest.getRequestContentLength());
        if (applicationRequest.getResponse() != null) {
            logItem.setResponse(
                new Response()
                    .body(applicationRequest.getResponse().getBody())
                    .status(applicationRequest.getResponse().getStatus())
                    .headers(applicationRequest.getResponse().getHeaders())
            );
        }
        logItem.setResponseContentLength(applicationRequest.getResponseContentLength());
        logItem.setResponseTime(applicationRequest.getResponseTime());
        logItem.setSecurityToken(applicationRequest.getSecurityToken());
        logItem.setSecurityType(applicationRequest.getSecurityType());
        logItem.setStatus(applicationRequest.getStatus());
        logItem.setTimestamp(applicationRequest.getTimestamp());
        logItem.setTransactionId(applicationRequest.getTransactionId());
        logItem.setUser(applicationRequest.getUser());

        return logItem;
    }

    public Log convert(ApplicationRequestItem applicationRequestItem) {
        final Log logItem = new Log();
        logItem.setApi(applicationRequestItem.getApi());
        logItem.setId(applicationRequestItem.getId());
        logItem.setMethod(HttpMethod.fromValue(applicationRequestItem.getMethod().name()));
        logItem.setPath(applicationRequestItem.getPath());
        logItem.setPlan(applicationRequestItem.getPlan());
        logItem.setResponseTime(applicationRequestItem.getResponseTime());
        logItem.setStatus(applicationRequestItem.getStatus());
        logItem.setTimestamp(applicationRequestItem.getTimestamp());
        logItem.setTransactionId(applicationRequestItem.getTransactionId());
        logItem.setUser(applicationRequestItem.getUser());

        return logItem;
    }

    public List<Log> convert(List<ConnectionLog> logs) {
        if (logs == null) {
            return new ArrayList<>();
        }
        return logs.stream().map(this::convert).toList();
    }

    public Log convert(ConnectionLog connectionLog) {
        final Log logItem = new Log();
        logItem.setApi(connectionLog.getApiId());
        logItem.setId(connectionLog.getRequestId());
        logItem.setMethod(HttpMethod.fromValue(connectionLog.getMethod().name()));
        logItem.setPath(connectionLog.getUri());
        logItem.setPlan(connectionLog.getPlanId());
        logItem.setResponseTime(connectionLog.getGatewayResponseTime());
        logItem.setStatus(connectionLog.getStatus());
        logItem.setTimestamp(Instant.parse(connectionLog.getTimestamp()).toEpochMilli());
        logItem.setTransactionId(connectionLog.getTransactionId());
        logItem.setRequestContentLength(connectionLog.getRequestContentLength());
        logItem.setResponseContentLength(connectionLog.getResponseContentLength());
        return logItem;
    }

    public SearchLogsFilters convert(String applicationId, SearchApplicationLogsParam searchLogsParam) {
        return SearchLogsFilters
            .builder()
            .to(searchLogsParam.getTo())
            .from(searchLogsParam.getFrom())
            .applicationIds(Set.of(applicationId))
            .planIds(searchLogsParam.getPlanIds())
            .methods(convert(searchLogsParam.getMethods()))
            .statuses(searchLogsParam.getStatuses())
            .apiIds(searchLogsParam.getApiIds())
            .requestIds(searchLogsParam.getRequestIds())
            .transactionIds(searchLogsParam.getTransactionIds())
            .uri(searchLogsParam.getPath())
            .responseTimeRanges(convertResponseTimeRanges(searchLogsParam.getResponseTimeRanges()))
            .bodyText(searchLogsParam.getBodyText())
            .build();
    }

    private List<Range> convertResponseTimeRanges(List<ResponseTimeRange> responseTimeRanges) {
        if (responseTimeRanges == null) {
            return new ArrayList<>();
        }

        return responseTimeRanges.stream().map(r -> new Range(r.getFrom(), r.getTo())).toList();
    }

    public Set<io.gravitee.common.http.HttpMethod> convert(Set<HttpMethod> httpMethod) {
        if (httpMethod == null) {
            return new HashSet<>();
        }

        return httpMethod.stream().map(method -> io.gravitee.common.http.HttpMethod.valueOf(method.name())).collect(Collectors.toSet());
    }
}
