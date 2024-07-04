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
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.log.Log;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogAdapter extends Log {

    private final io.gravitee.reporter.api.v4.log.Log log;

    public LogAdapter(final io.gravitee.reporter.api.v4.log.Log log) {
        super(log.getTimestamp());
        this.log = log;
    }

    @Override
    public String getApi() {
        return log.getApiId();
    }

    @Override
    public void setApi(final String api) {
        log.setApiId(api);
    }

    @Override
    public String getRequestId() {
        return log.getRequestId();
    }

    @Override
    public void setRequestId(final String requestId) {
        log.setRequestId(requestId);
    }

    @Override
    public Request getClientRequest() {
        return log.getEntrypointRequest();
    }

    @Override
    public void setClientRequest(final Request clientRequest) {
        log.setEntrypointRequest(clientRequest);
    }

    @Override
    public Request getProxyRequest() {
        return log.getEndpointRequest();
    }

    @Override
    public void setProxyRequest(final Request proxyRequest) {
        log.setEndpointRequest(proxyRequest);
    }

    @Override
    public Response getClientResponse() {
        return log.getEntrypointResponse();
    }

    @Override
    public void setClientResponse(final Response clientResponse) {
        log.setEntrypointResponse(clientResponse);
    }

    @Override
    public Response getProxyResponse() {
        return log.getEndpointResponse();
    }

    @Override
    public void setProxyResponse(final Response proxyResponse) {
        log.setEndpointResponse(proxyResponse);
    }

    @Override
    public String getApiName() {
        return log.getApiName();
    }

    @Override
    public void setApiName(final String apiName) {
        log.setApiName(apiName);
    }
}
