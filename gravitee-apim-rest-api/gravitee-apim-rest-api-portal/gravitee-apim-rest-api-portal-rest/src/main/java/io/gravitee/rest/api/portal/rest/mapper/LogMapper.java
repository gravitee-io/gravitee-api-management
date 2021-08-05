/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.portal.rest.model.HttpMethod;
import io.gravitee.rest.api.portal.rest.model.Log;
import io.gravitee.rest.api.portal.rest.model.Request;
import io.gravitee.rest.api.portal.rest.model.Response;
import java.util.HashMap;
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
}
