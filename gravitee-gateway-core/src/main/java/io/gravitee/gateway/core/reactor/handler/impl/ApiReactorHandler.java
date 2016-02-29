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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.*;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.StringBodyPart;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.expression.spel.WrappedRequestVariable;
import io.gravitee.gateway.core.policy.PathResolver;
import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.gateway.core.policy.StreamType;
import io.gravitee.gateway.core.policy.impl.AbstractPolicyChain;
import io.gravitee.gateway.core.policy.impl.ExecutionContextImpl;
import io.gravitee.gateway.core.reactor.handler.ContextReactorHandler;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiReactorHandler extends ContextReactorHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiReactorHandler.class);

    @Autowired
    private Api api;

    @Autowired
    private Invoker remoteInvoker;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PathResolver pathResolver;

    @Override
    public void handle(Request serverRequest, Response serverResponse, Handler<Response> handler) {
        // Set specific metrics for API handler
        serverRequest.metrics().setApi(api.getId());

        // Resolve the "configured" path according to the inbound request
        Path path = pathResolver.resolve(serverRequest);

        // Calculate request policies
        List<Policy> requestPolicies = getPolicyResolver().resolve(StreamType.REQUEST, serverRequest, path.getRules());

        // Prepare execution context
        ExecutionContext executionContext = createExecutionContext();
        executionContext.getTemplateEngine().getTemplateContext().setVariable("request", new WrappedRequestVariable(serverRequest));
        executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", api.getProperties());
        executionContext.setAttribute(ExecutionContext.ATTR_RESOLVED_PATH, path.getPath());
        executionContext.setAttribute(ExecutionContext.ATTR_API, api.getId());
        executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, remoteInvoker);

        // Apply request policies
        AbstractPolicyChain requestPolicyChain = getRequestPolicyChainBuilder().newPolicyChain(requestPolicies, executionContext);
        requestPolicyChain.setResultHandler(requestPolicyResult -> {
            if (requestPolicyResult.isFailure()) {
                writePolicyResult(requestPolicyResult, serverResponse);

                handler.handle(serverResponse);
            } else {
                // Use the upstream invoker (call the remote API using HTTP client)
                Invoker invoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);

                long serviceInvocationStart = System.currentTimeMillis();
                invoker.invoke(executionContext, serverRequest, responseStream -> {

                    // Set the status
                    serverResponse.status(responseStream.status());

                    // Copy HTTP headers
                    responseStream.headers().forEach((headerName, headerValues) -> serverResponse.headers().put(headerName, headerValues));

                    // Calculate response policies
                    List<Policy> responsePolicies = getPolicyResolver().resolve(StreamType.RESPONSE, serverRequest, path.getRules());
                    AbstractPolicyChain responsePolicyChain = getResponsePolicyChainBuilder().newPolicyChain(responsePolicies, executionContext);

                    responsePolicyChain.setResultHandler(responsePolicyResult -> {
                        if (responsePolicyResult.isFailure()) {
                            writePolicyResult(responsePolicyResult, serverResponse);

                            handler.handle(serverResponse);
                        } else {
                            responseStream.bodyHandler(serverResponse::write);

                            responseStream.endHandler(result -> {
                                serverResponse.end();

                                serverRequest.metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                                LOGGER.debug("Remote API invocation took {} ms [request={}]", serverRequest.metrics().getApiResponseTimeMs(), serverRequest.id());

                                // Transfer proxy response to the initial consumer
                                handler.handle(serverResponse);
                            });
                        }
                    });

                    responsePolicyChain.doNext(serverRequest, serverResponse);
                });
            }
        });

        requestPolicyChain.doNext(serverRequest, serverResponse);
    }

    private void writePolicyResult(PolicyResult policyResult, Response response) {
        response.status(policyResult.httpStatusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (policyResult.message() != null) {
            try {
                String contentAsJson = mapper.writeValueAsString(new PolicyResultAsJson(policyResult));
                StringBodyPart responseBody = new StringBodyPart(contentAsJson);
                response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(responseBody.length()));
                response.headers().set(HttpHeaders.CONTENT_TYPE, "application/json");
                response.write(responseBody);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        response.end();
    }

    private class PolicyResultAsJson {

        @JsonProperty
        private final String message;

        @JsonProperty("http_status_code")
        private final int httpStatusCode;

        private PolicyResultAsJson(PolicyResult policyResult) {
            this.message = policyResult.message();
            this.httpStatusCode = policyResult.httpStatusCode();
        }

        private String getMessage() {
            return message;
        }

        private int httpStatusCode() {
            return httpStatusCode;
        }
    }

    private ExecutionContext createExecutionContext() {
        return new ExecutionContextImpl(applicationContext);
    }

    @Override
    public String getContextPath() {
        return api.getProxy().getContextPath() + '/';
    }

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
