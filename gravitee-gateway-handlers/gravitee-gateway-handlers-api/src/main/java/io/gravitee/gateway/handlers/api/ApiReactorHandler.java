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
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.expression.TemplateContext;
import io.gravitee.gateway.api.expression.TemplateVariableProvider;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecyleManager;
import io.gravitee.gateway.core.logging.processor.LoggableRequestProcessorProvider;
import io.gravitee.gateway.core.processor.*;
import io.gravitee.gateway.core.proxy.DirectProxyConnection;
import io.gravitee.gateway.handlers.api.context.ExecutionContextFactory;
import io.gravitee.gateway.handlers.api.cors.CorsHandler;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.metrics.PathMappingMetricsHandler;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainResolver;
import io.gravitee.gateway.handlers.api.processor.ApiLoggableRequestProcessor;
import io.gravitee.gateway.policy.PolicyChainResolver;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.AbstractReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.security.core.SecurityPolicyChainResolver;
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
public class ApiReactorHandler extends AbstractReactorHandler implements TemplateVariableProvider,  InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(ApiReactorHandler.class);

    @Autowired
    protected Api api;

    /**
     * Invoker is the connector to access the remote backend / endpoint.
     * If not override by a policy, default invoker is {@link io.gravitee.gateway.core.invoker.DefaultInvoker}.
     */
    @Autowired
    private Invoker invoker;

    @Autowired
    private ObjectMapper mapper;

    private String contextPath;

    private List<ProcessorProvider> requestProcessors;

    private PolicyChainResolver apiPolicyResolver;

    @Autowired
    private ExecutionContextFactory executionContextFactory;

    @Override
    protected void doHandle(Request serverRequest, Response serverResponse, Handler<Response> handler) {
        // Set the path without the context-path
        serverRequest.metrics().setPath(serverRequest.pathInfo());

        if (api.getPathMappings() != null && !api.getPathMappings().isEmpty()) {
            handler = new PathMappingMetricsHandler(handler, api.getPathMappings(), serverRequest);
        }

        // Prepare request execution context
        ExecutionContext executionContext = executionContextFactory.create(serverRequest, serverResponse);
        executionContext.setAttribute(ExecutionContext.ATTR_CONTEXT_PATH, serverRequest.contextPath());

        try {
            // Set execution context attributes and metrics specific to this handler
            serverRequest.metrics().setApi(api.getId());

            executionContext.setAttribute(ExecutionContext.ATTR_API, api.getId());
            executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);

            Cors cors = api.getProxy().getCors();
            if (cors != null && cors.isEnabled()) {
                Handler<Response> finalHandler = handler;

                new CorsHandler(cors)
                        .responseHandler(response -> handleClientRequest(serverRequest, serverResponse, executionContext, finalHandler))
                        .handle(serverRequest, serverResponse, handler);
            } else {
                handleClientRequest(serverRequest, serverResponse, executionContext, handler);
            }
        } catch (Exception ex) {
            logger.error("An unexpected error occurs while processing request", ex);

            serverRequest.metrics().setMessage(Throwables.getStackTraceAsString(ex));

            // Send an INTERNAL_SERVER_ERROR (500)
            serverResponse.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            serverResponse.end();

            handler.handle(serverResponse);
        }
    }

    private void handleClientRequest(final Request serverRequest, final Response serverResponse, final ExecutionContext executionContext, final Handler<Response> handler) {
        serverRequest.pause();

        // Process incoming request
        ProcessorContext context = ProcessorContext.from(serverRequest, serverResponse, executionContext);
        StreamableProcessor<Buffer> requestProcessor = new ProviderRequestProcessorChain(requestProcessors);
        requestProcessor
                .handler(__ -> handleProxyInvocation(context, requestProcessor, handler))
                .errorHandler(failure -> {
                        handleProcessorFailure(failure, context.getResponse());
                        handler.handle(context.getResponse());
                })
                .process(context);
    }

    private void handleProxyInvocation(final ProcessorContext processorContext, final StreamableProcessor<Buffer> processor, final Handler<Response> handler) {
        // Call an invoker to get a proxy connection (connection to an underlying backend, mainly HTTP)
        Invoker upstreamInvoker = (Invoker) processorContext.getContext().getAttribute(ExecutionContext.ATTR_INVOKER);

        final long serviceInvocationStart = System.currentTimeMillis();
        Request invokeRequest = upstreamInvoker.invoke(processorContext.getContext(), processorContext.getRequest(), processor, connection -> {
            connection.responseHandler(proxyResponse -> handleProxyResponse(processorContext, proxyResponse, serviceInvocationStart, handler));

            processor.streamErrorHandler(error -> {
                connection.cancel();
                handleProcessorFailure(error, processorContext.getResponse());
                handler.handle(processorContext.getResponse());
            });
        });

        processorContext.setRequest(invokeRequest);

        // Plug server request stream to request processor stream
        invokeRequest
                .bodyHandler(processor::write)
                .endHandler(aVoid -> processor.end());
    }

    private void handleProxyResponse(final ProcessorContext processorContext, final ProxyResponse proxyResponse, final long serviceInvocationStart, final Handler<Response> handler) {
        if (proxyResponse == null || proxyResponse instanceof DirectProxyConnection.DirectResponse) {
            processorContext.getResponse().status((proxyResponse == null) ? HttpStatusCode.SERVICE_UNAVAILABLE_503 : proxyResponse.status());
            processorContext.getResponse().end();
            handler.handle(processorContext.getResponse());
        } else {
            handleClientResponse(processorContext, proxyResponse, serviceInvocationStart, handler);
        }
    }

    private void handleClientResponse(final ProcessorContext processorContext, final ProxyResponse proxyResponse, final long serviceInvocationStart, final Handler<Response> handler) {
        // Set the status
        processorContext.getResponse().status(proxyResponse.status());

        // Copy HTTP headers
        proxyResponse.headers().forEach((headerName, headerValues) -> processorContext.getResponse().headers().put(headerName, headerValues));

        // Calculate response policies
        StreamableProcessor<PolicyResult> responsePolicyChain = apiPolicyResolver
                .resolve(StreamType.ON_RESPONSE, processorContext.getRequest(), processorContext.getResponse(), processorContext.getContext());

        responsePolicyChain
                .handler(result -> {
                    responsePolicyChain
                            .bodyHandler(chunk -> processorContext.getResponse().write(chunk))
                            .endHandler(__ -> {
                                processorContext.getResponse().end();
                                handler.handle(processorContext.getResponse());
                            });

                    proxyResponse.bodyHandler(buffer -> {
                        responsePolicyChain.write(buffer);

                        if (processorContext.getResponse().writeQueueFull()) {
                            proxyResponse.pause();
                            processorContext.getResponse().drainHandler(aVoid -> proxyResponse.resume());
                        }
                    });
                    proxyResponse.endHandler(__ -> {
                        responsePolicyChain.end();
                        processorContext.getRequest().metrics().setApiResponseTimeMs(System.currentTimeMillis() - serviceInvocationStart);
                    });
                })
                .errorHandler(failure -> {
                    handleProcessorFailure(failure, processorContext.getResponse());
                    handler.handle(processorContext.getResponse());
                })
                .streamErrorHandler(result -> {
                    handleProcessorFailure(result, processorContext.getResponse());
                    handler.handle(processorContext.getResponse());
                });

        // Execute response policy chain
        responsePolicyChain.process(processorContext);

        // Resume response read
        proxyResponse.resume();
    }

    private void handleProcessorFailure(ProcessorFailure failure, Response response) {
        response.status(failure.statusCode());

        response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (failure.message() != null) {
            try {
                Buffer payload;
                if (failure.contentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                    payload = Buffer.buffer(failure.message());
                } else {
                    String contentAsJson = mapper.writeValueAsString(new ProcessorFailureAsJson(failure));
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

    @Override
    public void provide(TemplateContext templateContext) {
        templateContext.setVariable("properties", api.properties());
    }

    private class ProcessorFailureAsJson {

        @JsonProperty
        private final String message;

        @JsonProperty("http_status_code")
        private final int httpStatusCode;

        private ProcessorFailureAsJson(ProcessorFailure processorFailure) {
            this.message = processorFailure.message();
            this.httpStatusCode = processorFailure.statusCode();
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

        requestProcessors = new ArrayList<>();

        apiPolicyResolver = new ApiPolicyChainResolver();
        PolicyChainResolver securityPolicyResolver = new SecurityPolicyChainResolver();
        PolicyChainResolver planPolicyResolver = new PlanPolicyChainResolver();

        applicationContext.getAutowireCapableBeanFactory().autowireBean(securityPolicyResolver);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(planPolicyResolver);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(apiPolicyResolver);

        requestProcessors.add(securityPolicyResolver);

        if (api.getProxy().getLogging() != null && api.getProxy().getLogging().getMode() != LoggingMode.NONE) {
            LoggableRequestProcessorProvider requestProcessorProvider = new LoggableRequestProcessorProvider(
                    new ApiLoggableRequestProcessor(api.getProxy().getLogging()));

            requestProcessors.add(requestProcessorProvider);
        }

        requestProcessors.add(planPolicyResolver);
        requestProcessors.add(apiPolicyResolver);
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
        applicationContext.getBean(GroupLifecyleManager.class).start();

        long endTime = System.currentTimeMillis(); // Get the end Time
        logger.info("API handler started in {} ms and now ready to accept requests on {}/*",
                (endTime - startTime), api.getProxy().getContextPath());
    }

    @Override
    protected void doStop() throws Exception {
        logger.info("API handler is now stopping, closing context...");

        applicationContext.getBean(PolicyManager.class).stop();
        applicationContext.getBean(ResourceLifecycleManager.class).stop();
        applicationContext.getBean(GroupLifecyleManager.class).stop();

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
