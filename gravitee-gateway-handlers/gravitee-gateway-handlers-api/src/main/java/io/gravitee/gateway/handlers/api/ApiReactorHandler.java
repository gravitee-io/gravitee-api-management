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
import com.google.common.base.Throwables;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainResolver;
import io.gravitee.gateway.http.core.endpoint.EndpointLifecycleManager;
import io.gravitee.gateway.http.core.invoker.DefaultHttpInvoker;
import io.gravitee.gateway.policy.PolicyChainResolver;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.PolicyChain;
import io.gravitee.gateway.policy.impl.RequestPolicyChainProcessor;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.AbstractReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.security.core.SecurityPolicyChainResolver;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiReactorHandler extends AbstractReactorHandler implements InitializingBean {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

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
        MDC.put("api", api.getId());

        try {
            serverRequest.pause();

            // Set execution context attributes and metrics specific to this handler
            serverRequest.metrics().setApi(api.getId());

            // Create a copy of incoming HTTP request headers
            serverRequest.metrics().setClientRequestHeaders(new HttpHeaders(serverRequest.headers()));

            executionContext.setAttribute(ExecutionContext.ATTR_API, api.getId());
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);
            executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", api.properties());
            executionContext.getTemplateEngine().getTemplateContext().setVariable("endpoints", endpointLifecycleManager.targetByEndpoint());

            // Apply request policies
            RequestPolicyChainProcessor requestPolicyChain = new RequestPolicyChainProcessor(policyResolvers);
            requestPolicyChain.setResultHandler(requestPolicyChainResult -> {
                if (requestPolicyChainResult.isFailure()) {
                    sendPolicyFailure(requestPolicyChainResult.getPolicyResult(), serverResponse);
                    handler.handle(serverResponse);
                } else {
                    // Call an invoker to get a proxy connection (connection to an underlying backend, mainly HTTP)
                    Invoker upstreamInvoker = (Invoker) executionContext.getAttribute(ExecutionContext.ATTR_INVOKER);

                    long serviceInvocationStart = System.currentTimeMillis();

                    Request invokeRequest = upstreamInvoker.invoke(executionContext, serverRequest, requestPolicyChainResult.getPolicyChain(), connection -> {
                        connection.responseHandler(
                                proxyResponse -> handleProxyResponse(serverRequest, serverResponse, executionContext, proxyResponse, serviceInvocationStart, handler));

                        requestPolicyChain.setStreamErrorHandler(result -> {
                            connection.cancel();
                            // TODO: review this part of the code
                            sendPolicyFailure(result.getPolicyResult(), serverResponse);
                            handler.handle(serverResponse);
                        });
                    });

                    // Plug server request stream to request policy chain stream
                    invokeRequest
                            .bodyHandler(chunk -> {
                                MDC.put("api", api.getId());
                                invokeRequest.metrics().setRequestContentLength(invokeRequest.metrics().getRequestContentLength() + chunk.length());
                                requestPolicyChainResult.getPolicyChain().write(chunk);
                            })
                            .endHandler(aVoid -> {
                                MDC.put("api", api.getId());
                                requestPolicyChainResult.getPolicyChain().end();
                            });
                }
            });

            requestPolicyChain.execute(serverRequest, serverResponse, executionContext);
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while processing request", ex);

            serverRequest.metrics().setMessage(Throwables.getStackTraceAsString(ex));

            // Send an INTERNAL_SERVER_ERROR (500)
            serverResponse.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            serverResponse.end();

            handler.handle(serverResponse);
        } finally {
            MDC.remove("api");
        }
    }

    private void handleProxyResponse(Request serverRequest, Response serverResponse, ExecutionContext executionContext, ProxyResponse proxyResponse, long serviceInvocationStart, Handler<Response> handler) {
        if (proxyResponse == null) {
            serverResponse.status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
            serverResponse.end();
        } else {
            // Set the status
            serverResponse.status(proxyResponse.status());

            // Create a copy of proxy response headers send by the backend
            serverRequest.metrics().setProxyResponseHeaders(proxyResponse.headers());

            // Copy HTTP headers
            proxyResponse.headers().forEach((headerName, headerValues) -> serverResponse.headers().put(headerName, headerValues));

            // Calculate response policies
            PolicyChain responsePolicyChain = apiPolicyResolver.resolve(StreamType.ON_RESPONSE, serverRequest, serverResponse, executionContext);

            responsePolicyChain.setResultHandler(responsePolicyResult -> {
                if (responsePolicyResult.isFailure()) {
                    sendPolicyFailure(responsePolicyResult, serverResponse);
                    handler.handle(serverResponse);
                } else {
                    responsePolicyChain.bodyHandler(chunk -> {
                        MDC.put("api", api.getId());
                        serverRequest.metrics().setResponseContentLength(serverRequest.metrics().getResponseContentLength() + chunk.length());
                        serverResponse.write(chunk);
                    });
                    responsePolicyChain.endHandler(responseEndResult -> {
                        MDC.put("api", api.getId());
                        serverResponse.end();

                        //TODO: How to pass invocation start timestamp ?
                        serverRequest.metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                        logger.debug("Remote API invocation took {} ms [request={}]", serverRequest.metrics().getApiResponseTimeMs(), serverRequest.id());

                        // Transfer proxy response to the initial consumer
                        handler.handle(serverResponse);
                    });

                    proxyResponse.bodyHandler(responsePolicyChain::write);
                    proxyResponse.endHandler(aVoid -> responsePolicyChain.end());
                }
            });

            responsePolicyChain.setStreamErrorHandler(result -> {
                sendPolicyFailure(result, serverResponse);
                handler.handle(serverResponse);
            });

            // Execute response policy chain
            responsePolicyChain.doNext(serverRequest, serverResponse);

            // Resume response read
            proxyResponse.resume();
        }
    }

    void sendPolicyFailure(PolicyResult policyResult, Response response) {
        response.status(policyResult.httpStatusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (policyResult.message() != null) {
            try {
                Buffer payload;
                if (policyResult.contentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                    payload = Buffer.buffer(policyResult.message());
                } else {
                    String contentAsJson = mapper.writeValueAsString(new PolicyResultAsJson(policyResult));
                    payload = Buffer.buffer(contentAsJson);
                }
                response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(payload.length()));
                response.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.write(payload);
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
        PolicyChainResolver securityPolicyResolver = new SecurityPolicyChainResolver();
        PolicyChainResolver planPolicyResolver = new PlanPolicyChainResolver();

        policyResolvers = new ArrayList<PolicyChainResolver>() {
            {
                applicationContext.getAutowireCapableBeanFactory().autowireBean(securityPolicyResolver);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(planPolicyResolver);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(apiPolicyResolver);

                add(securityPolicyResolver);
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
