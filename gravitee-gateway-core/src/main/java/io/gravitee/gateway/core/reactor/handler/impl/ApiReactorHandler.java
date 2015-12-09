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
package io.gravitee.gateway.core.reactor.handler.impl;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.gateway.api.http.client.AsyncResponseHandler;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.endpoint.EndpointResolver;
import io.gravitee.gateway.core.http.StringBodyPart;
import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.gateway.core.policy.impl.AbstractPolicyChain;
import io.gravitee.gateway.core.reactor.handler.ContextReactorHandler;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiReactorHandler extends ContextReactorHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiReactorHandler.class);

    @Autowired
    private Api api;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private EndpointResolver endpointResolver;

    @Override
    public void handle(Request serverRequest, Response serverResponse, Handler<Response> handler) {
        // Do we need API name or API id ? (this information is transferred to target API)
        serverRequest.headers().set(GraviteeHttpHeader.X_GRAVITEE_API_NAME, api.getId());

        // Set specific metrics for API handler
        serverResponse.metrics().setApi(api.getId());

        // Calculate policies
        List<Policy> policies = getPolicyResolver().resolve(serverRequest);

        // Resolve endpoint target
        URI endpoint = endpointResolver.resolve(serverRequest);
        serverResponse.metrics().setEndpoint(endpoint.toString());

        // Apply serverRequest policies
        AbstractPolicyChain requestPolicyChain = getRequestPolicyChainBuilder().newPolicyChain(policies);
        requestPolicyChain.setResultHandler(requestPolicyResult -> {
            if (requestPolicyResult.isFailure()) {
                writePolicyResult(requestPolicyResult, serverResponse);

                handler.handle(serverResponse);
            } else {
                // Call remote endpoint
                long serviceInvocationStart = System.currentTimeMillis();
                httpClient.invoke(serverRequest, endpoint, new AsyncResponseHandler() {
                    @Override
                    public void onStatusReceived(int status) {
                        serverResponse.status(status);
                    }

                    @Override
                    public void onHeadersReceived(HttpHeaders headers) {
                        LOGGER.debug("{} proxying serverResponse headers to downstream", serverRequest.id());

                        headers.forEach(
                                (headerName, headerValues) -> serverResponse.headers().put(headerName, headerValues));

                        String transferEncoding = serverResponse.headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
                        if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                            serverResponse.chunked(true);
                        }
                    }

                    @Override
                    public void onBodyPartReceived(BodyPart bodyPart) {
                        LOGGER.debug("{} proxying content to downstream: {} bytes", serverRequest.id(), bodyPart.length());
                        serverResponse.write(bodyPart);
                    }

                    @Override
                    public void onComplete() {
                        serverResponse.end();

                        serverResponse.metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                        LOGGER.debug("Remote API invocation took {} ms [request={}]", serverResponse.metrics().getApiResponseTimeMs(), serverRequest.id());

                        // Transfer proxy response to the initial consumer
                        handler.handle(serverResponse);
                    }
                });
            }
        });

        requestPolicyChain.doNext(serverRequest, serverResponse);
    }

    private void writePolicyResult(PolicyResult policyResult, Response response) {
        response.status(policyResult.httpStatusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (policyResult.message() != null) {
            StringBodyPart responseBody = new StringBodyPart(policyResult.message());
            response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(responseBody.length()));
            response.write(responseBody);
        }

        response.end();
    }

    @Override
    public String getContextPath() {
        return api.getProxy().getContextPath();
    }

    /*
    @Override
    public String getVirtualHost() {
        return api.getProxy().getTarget().getAuthority();
    }
    */

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        httpClient.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        httpClient.stop();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiReactorHandler{");
        sb.append("contextPath=").append(api.getProxy().getContextPath());
        sb.append('}');
        return sb.toString();
    }

    public Api getApi() {
        return api;
    }
}
