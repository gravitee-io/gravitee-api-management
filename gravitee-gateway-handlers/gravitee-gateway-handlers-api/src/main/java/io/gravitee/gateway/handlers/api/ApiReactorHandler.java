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
import io.gravitee.gateway.api.*;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.policy.*;
import io.gravitee.gateway.http.core.endpoint.EndpointLifecycleManager;
import io.gravitee.gateway.http.core.invoker.DefaultHttpInvoker;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.AbstractReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiReactorHandler extends AbstractReactorHandler implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(ApiReactorHandler.class);

    @Autowired
    private Api api;

    /**
     * Invoker is the connector to access the remote backend / endpoint.
     * If not override by a policy, default invoker is {@link DefaultHttpInvoker}.
     */
    @Autowired
    private Invoker invoker;

    @Autowired
    private ObjectMapper mapper;

    private String contextPath;

    private List<PolicyChainResolver> policyResolvers;

    private PolicyChainResolver apiPolicyResolver;

    @Autowired
    private EndpointLifecycleManager endpointLifecycleManager;

    @Override
    protected void doHandle(Request serverRequest, Response serverResponse, Handler<Response> handler,
                            ExecutionContext executionContext) {
        try {
            serverRequest.pause();

            // Set execution context attributes and metrics specific to this handler
            serverRequest.metrics().setApi(api.getId());

            executionContext.setAttribute(ExecutionContext.ATTR_API, api.getId());
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);
            executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", api.getProperties());
            executionContext.getTemplateEngine().getTemplateContext().setVariable("endpoints", endpointLifecycleManager.targetByEndpoint());

            // Apply request policies
            RequestPolicyChainProcessor requestPolicyChain = new RequestPolicyChainProcessor(policyResolvers);
            requestPolicyChain.setResultHandler(requestPolicyChainResult -> {
                if (requestPolicyChainResult.isFailure()) {
                    sendPolicyFailure(requestPolicyChainResult.getPolicyResult(), serverResponse);
                    handler.handle(serverResponse);
                } else {
                    // Use the upstream invoker (call the remote API using HTTP client)
                    Invoker upstreamInvoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);

                    long serviceInvocationStart = System.currentTimeMillis();
                    ClientRequest clientRequest = upstreamInvoker.invoke(executionContext, serverRequest, responseStream -> {

                        // Set the status
                        serverResponse.status(responseStream.status());

                        // Copy HTTP headers
                        responseStream.headers().forEach((headerName, headerValues) -> serverResponse.headers().put(headerName, headerValues));

                        // Calculate response policies
                        PolicyChain responsePolicyChain = apiPolicyResolver.resolve(StreamType.ON_RESPONSE, serverRequest, serverResponse, executionContext);
                        responsePolicyChain.setResultHandler(responsePolicyResult -> {
                            if (responsePolicyResult.isFailure()) {
                                sendPolicyFailure(responsePolicyResult, serverResponse);
                                handler.handle(serverResponse);
                            } else {
                                responsePolicyChain.bodyHandler(chunk -> {
                                    serverRequest.metrics().setResponseContentLength(serverRequest.metrics().getResponseContentLength() + chunk.length());
                                    serverResponse.write(chunk);
                                });
                                responsePolicyChain.endHandler(responseEndResult -> {
                                    serverResponse.end();

                                    serverRequest.metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                                    logger.debug("Remote API invocation took {} ms [request={}]", serverRequest.metrics().getApiResponseTimeMs(), serverRequest.id());

                                    // Transfer proxy response to the initial consumer
                                    handler.handle(serverResponse);
                                });

                                responseStream.bodyHandler(responsePolicyChain::write);
                                responseStream.endHandler(aVoid -> responsePolicyChain.end());
                            }
                        });

                        // Execute response policy chain
                        responsePolicyChain.doNext(serverRequest, serverResponse);
                    });

                    // In case of underlying service unavailable, we can have a null client request
                    if (clientRequest != null) {
                        // Plug request policy chain stream to backend request stream
                        requestPolicyChainResult.getPolicyChain()
                                .bodyHandler(clientRequest::write)
                                .endHandler(aVoid -> clientRequest.end());
                    }

                    // Plug server request stream to request policy chain stream
                    serverRequest
                            .bodyHandler(chunk -> {
                                serverRequest.metrics().setRequestContentLength(serverRequest.metrics().getRequestContentLength() + chunk.length());
                                requestPolicyChainResult.getPolicyChain().write(chunk);
                            })
                            .endHandler(aVoid -> requestPolicyChainResult.getPolicyChain().end());

                    // Resume request read
                    serverRequest.resume();
                }
            });

            requestPolicyChain.execute(serverRequest, serverResponse, executionContext);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while processing request", ex);

            // Send an INTERNAL_SERVER_ERROR (500)
            serverResponse.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            serverResponse.end();

            handler.handle(serverResponse);
        }
    }

    private void sendPolicyFailure(PolicyResult policyResult, Response response) {
        response.status(policyResult.httpStatusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (policyResult.message() != null) {
            try {
                String contentAsJson = mapper.writeValueAsString(new PolicyResultAsJson(policyResult));
                Buffer buf = Buffer.buffer(contentAsJson);
                response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(buf.length()));
                response.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.write(buf);
            } catch (JsonProcessingException jpe) {
                logger.error("Unable to transform a policy result into a json payload", jpe);
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

    @Override
    public void afterPropertiesSet() {
        contextPath = reactable().contextPath() + '/';

        apiPolicyResolver = new ApiPolicyChainResolver();
        PolicyChainResolver apiKeyPolicyResolver = new ApiKeyPolicyChainResolver();
        PolicyChainResolver planPolicyResolver = new PlanPolicyChainResolver();

        policyResolvers = new ArrayList<PolicyChainResolver>() {
            {
                applicationContext.getAutowireCapableBeanFactory().autowireBean(apiKeyPolicyResolver);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(planPolicyResolver);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(apiPolicyResolver);

                add(apiKeyPolicyResolver);
                add(planPolicyResolver);
                add(apiPolicyResolver);
            }
        };
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public Reactable reactable() {
        return api;
    }

    @Override
    protected void doStart() throws Exception {
        logger.info("API handler is now starting, preparing API context...");
        long startTime = System.currentTimeMillis(); // Get the start Time
        super.doStart();

        // Start resources before
        applicationContext.getBean(ResourceLifecycleManager.class).start();
        applicationContext.getBean(PolicyManager.class).start();
        endpointLifecycleManager.start();

        long endTime = System.currentTimeMillis(); // Get the end Time
        logger.info("API handler started in {} ms and now ready to accept requests on {}/*",
                (endTime - startTime), api.getProxy().getContextPath());
    }

    @Override
    protected void doStop() throws Exception {
        logger.info("API handler is now stopping, closing context...");

        applicationContext.getBean(PolicyManager.class).stop();
        applicationContext.getBean(ResourceLifecycleManager.class).stop();
        endpointLifecycleManager.stop();

        super.doStop();
        logger.info("API handler is now stopped", api);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiReactorHandler{");
        sb.append("contextPath=").append(api.getProxy().getContextPath());
        sb.append('}');
        return sb.toString();
    }
}
