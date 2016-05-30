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
package io.gravitee.gateway.handlers.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.el.http.EvaluableRequest;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.impl.ExecutionContextImpl;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.PolicyResolver;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.policy.impl.ResponsePolicyChain;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.AbstractReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ApiReactorHandler extends AbstractReactorHandler implements InitializingBean {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiReactorHandler.class);

    @Autowired
    private Api api;

    @Autowired
    private Invoker defaultInvoker;

    @Autowired
    private ObjectMapper mapper;

    private PolicyResolver policyResolver;

    @Autowired
    private PathResolver pathResolver;

    private String contextPath;

    @Override
    public CompletableFuture<Response> handle(Request serverRequest, Response serverResponse) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        try {
            serverRequest.pause();
            
            // Set specific metrics for API
            serverRequest.metrics().setApi(api.getId());

            // Resolve the "configured" path according to the inbound request
            Path path = pathResolver.resolve(serverRequest);

            // Calculate request policies
            List<Policy> requestPolicies = getPolicyResolver().resolve(StreamType.ON_REQUEST, serverRequest, path.getRules());

            // Prepare execution context
            ExecutionContext executionContext = createExecutionContext();
            executionContext.getTemplateEngine().getTemplateContext().setVariable("request", new EvaluableRequest(serverRequest));
            executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", api.getProperties());
            executionContext.setAttribute(ExecutionContext.ATTR_RESOLVED_PATH, path.getPath());
            executionContext.setAttribute(ExecutionContext.ATTR_API, api.getId());
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, defaultInvoker);

            // Apply request policies
            RequestPolicyChain requestPolicyChain = RequestPolicyChain.create(requestPolicies, executionContext);
            requestPolicyChain.setResultHandler(requestPolicyResult -> {
                if (requestPolicyResult.isFailure()) {
                    writePolicyResult(requestPolicyResult, serverResponse);

                    future.complete(serverResponse);
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
                        List<Policy> responsePolicies = getPolicyResolver().resolve(StreamType.ON_RESPONSE, serverRequest, path.getRules());
                        ResponsePolicyChain responsePolicyChain = ResponsePolicyChain.create(responsePolicies, executionContext);

                        responsePolicyChain.setResultHandler(responsePolicyResult -> {
                            if (responsePolicyResult.isFailure()) {
                                writePolicyResult(responsePolicyResult, serverResponse);

                                future.complete(serverResponse);
                            }
                        });

                        responsePolicyChain.bodyHandler(serverResponse::write);
                        responsePolicyChain.endHandler(responseEndResult -> {
                            serverResponse.end();

                            serverRequest.metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                            LOGGER.debug("Remote API invocation took {} ms [request={}]", serverRequest.metrics().getApiResponseTimeMs(), serverRequest.id());

                            // Transfer proxy response to the initial consumer
                            future.complete(serverResponse);
                        });

                        responseStream.bodyHandler(responsePolicyChain::write);
                        responseStream.endHandler(aVoid -> responsePolicyChain.end());

                        // Execute response policy chain
                        responsePolicyChain.doNext(serverRequest, serverResponse);
                    });

                    // Resume request read
                    serverRequest.resume();
                }
            });

            requestPolicyChain.doNext(serverRequest, serverResponse);
        } catch (Throwable t) {
            LOGGER.error("An unexpected error occurs while processing request", t);

            // Send an INTERNAL_SERVER_ERROR (500)
            serverResponse.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            serverResponse.end();

            future.complete(serverResponse);
        }

        return future;
    }

    private void writePolicyResult(PolicyResult policyResult, Response response) {
        response.status(policyResult.httpStatusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (policyResult.message() != null) {
            try {
                String contentAsJson = mapper.writeValueAsString(new PolicyResultAsJson(policyResult));
                Buffer buf = Buffer.buffer(contentAsJson);
                response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(buf.length()));
                response.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.write(buf);
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
    public void afterPropertiesSet() {
        contextPath = reactable().contextPath() + '/';
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public Reactable reactable() {
        return api;
    }

    private PolicyResolver getPolicyResolver() {
        if (policyResolver == null) {
            policyResolver = applicationContext.getBean(PolicyResolver.class);
        }
        return policyResolver;
    }

    @Override
    protected void doStart() throws Exception {
        LOGGER.info("API handler is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time
        super.doStart();

        // Start resources before
        applicationContext.getBean(ResourceLifecycleManager.class).start();

        applicationContext.getBean(PolicyManager.class).start();
        applicationContext.getBean(HttpClient.class).start();
        long endTime = System.currentTimeMillis(); // Get the end Time
        LOGGER.info("API handler started in {} ms and now ready to accept requests on {}/*",
                (endTime - startTime), api.getProxy().getContextPath());
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("API handler is now stopping, closing context...");
        applicationContext.getBean(PolicyManager.class).stop();
        applicationContext.getBean(HttpClient.class).stop();
        applicationContext.getBean(ResourceLifecycleManager.class).stop();

        super.doStop();
        LOGGER.info("API handler is now stopped", api);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiReactorHandler{");
        sb.append("contextPath=").append(api.getProxy().getContextPath());
        sb.append('}');
        return sb.toString();
    }
}
