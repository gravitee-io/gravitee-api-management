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
package io.gravitee.gateway.reactive.debug.reactor.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.definition.model.debug.DebugMetrics;
import io.gravitee.definition.model.debug.PreprocessorStep;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.debug.definition.DebugApiV4;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.debug.policy.steps.PolicyStep;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugCompletionProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCompletionProcessor.class);
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DebugCompletionProcessor(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getId() {
        return "processor-debug-completion";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.defer(() -> {
            final DebugExecutionContext debugContext = (DebugExecutionContext) ctx;
            ReactableDebugApi<?> debugApi = getDebugApi(debugContext);

            return Maybe.fromCallable(() -> eventRepository.findById(debugApi.getEventId())).flatMapCompletable(eventOptional -> {
                if (eventOptional.isPresent()) {
                    final Event event = eventOptional.get();
                    return computeDebugApiEventPayload(debugContext, debugApi)
                        .doOnSuccess(definitionDebugApi -> {
                            updateEvent(event.updatePayload(objectMapper.writeValueAsString(definitionDebugApi)), ApiDebugStatus.SUCCESS);
                        })
                        .ignoreElement()
                        .onErrorResumeNext(throwable -> {
                            LOGGER.error("Error occurs while saving debug event", throwable);
                            failEvent(event);
                            return Completable.complete();
                        });
                }
                return Completable.complete();
            });
        }).subscribeOn(Schedulers.io());
    }

    private Single<io.gravitee.definition.model.debug.DebugApiProxy> computeDebugApiEventPayload(
        DebugExecutionContext debugContext,
        ReactableDebugApi<?> debugApi
    ) {
        return Single.defer(() -> {
            var definitionDebugApi = convert(debugApi);
            PreprocessorStep preprocessorStep = createPreprocessorStep(debugContext);
            definitionDebugApi.setPreprocessorStep(preprocessorStep);
            definitionDebugApi.setDebugSteps(convert(debugContext.getDebugSteps()));
            HttpResponse invokerResponse = createResponse(
                debugContext.getInvokerResponse().getHeaders(),
                debugContext.getInvokerResponse().getStatus(),
                debugContext.getInvokerResponse().getBuffer()
            );
            definitionDebugApi.setBackendResponse(invokerResponse);
            definitionDebugApi.setMetrics(createMetrics(debugContext.metrics()));
            return debugContext
                .response()
                .bodyOrEmpty()
                .map(buffer -> {
                    HttpResponse response = createResponse(debugContext.response().headers(), debugContext.response().status(), buffer);
                    definitionDebugApi.setResponse(response);
                    return definitionDebugApi;
                });
        });
    }

    private ReactableDebugApi<?> getDebugApi(HttpExecutionContextInternal ctx) {
        ReactableDebugApi<?> debugApi;
        try {
            debugApi = (ReactableDebugApi<?>) ctx.getComponent(Api.class);
        } catch (NoSuchBeanDefinitionException e) {
            debugApi = (ReactableDebugApi<?>) ctx.getComponent(io.gravitee.gateway.reactive.handlers.api.v4.Api.class);
        }

        return debugApi;
    }

    private DebugMetrics createMetrics(Metrics metrics) {
        final DebugMetrics debugMetrics = new DebugMetrics();
        if (metrics != null) {
            debugMetrics.setApiResponseTimeMs(metrics.getEndpointResponseTimeMs());
            debugMetrics.setProxyLatencyMs(metrics.getGatewayLatencyMs());
            debugMetrics.setProxyResponseTimeMs(metrics.getGatewayResponseTimeMs());
        }
        return debugMetrics;
    }

    private PreprocessorStep createPreprocessorStep(DebugExecutionContext debugContext) {
        final PreprocessorStep preprocessorStep = new PreprocessorStep();
        preprocessorStep.setAttributes(debugContext.getInitialAttributes());
        preprocessorStep.setHeaders(debugContext.getInitialHeaders().toListValuesMap());
        return preprocessorStep;
    }

    private HttpResponse createResponse(HttpHeaders httpHeaders, int statusCode, Buffer bodyBuffer) {
        HttpResponse response = new HttpResponse();
        if (httpHeaders != null) {
            response.setHeaders(httpHeaders.toListValuesMap());
        }
        response.statusCode(statusCode);
        if (bodyBuffer != null) {
            response.setBody(bodyBuffer.toString());
        }
        return response;
    }

    private void failEvent(@NonNull Event debugEvent) {
        try {
            updateEvent(debugEvent, ApiDebugStatus.ERROR);
        } catch (TechnicalException e) {
            LOGGER.error("Error when updating event {} with ERROR status", debugEvent.getId());
        }
    }

    private void updateEvent(@NonNull Event debugEvent, ApiDebugStatus apiDebugStatus) throws TechnicalException {
        eventRepository.update(debugEvent.updateProperties(Event.EventProperties.API_DEBUG_STATUS.getValue(), apiDebugStatus.name()));
    }

    private io.gravitee.definition.model.debug.DebugApiProxy convert(ReactableDebugApi<?> content) {
        if (content instanceof DebugApiV2 debugApiV2) {
            io.gravitee.definition.model.debug.DebugApiV2 debugAPI = new io.gravitee.definition.model.debug.DebugApiV2();
            debugAPI.setName(debugApiV2.getName());
            debugAPI.setId(debugApiV2.getId());
            debugAPI.setDefinitionVersion(debugApiV2.getDefinitionVersion());
            debugAPI.setResponse(debugApiV2.getResponse());
            debugAPI.setRequest(debugApiV2.getRequest());
            debugAPI.setFlowMode(debugApiV2.getDefinition().getFlowMode());
            debugAPI.setFlows(debugApiV2.getDefinition().getFlows());
            debugAPI.setPathMappings(debugApiV2.getDefinition().getPathMappings());
            debugAPI.setPlans(debugApiV2.getDefinition().getPlans());
            debugAPI.setPaths(debugApiV2.getDefinition().getPaths());
            debugAPI.setServices(debugApiV2.getDefinition().getServices());
            debugAPI.setProxy(debugApiV2.getDefinition().getProxy());
            debugAPI.setProperties(debugApiV2.getDefinition().getProperties());
            debugAPI.setResources(debugApiV2.getDefinition().getResources());
            debugAPI.setServices(debugApiV2.getDefinition().getServices());
            debugAPI.setResponseTemplates(debugApiV2.getDefinition().getResponseTemplates());
            debugAPI.setExecutionMode(debugApiV2.getDefinition().getExecutionMode());
            return debugAPI;
        } else if (content instanceof DebugApiV4 debugApiV4) {
            return new io.gravitee.definition.model.debug.DebugApiV4(
                debugApiV4.getDefinition(),
                debugApiV4.getRequest(),
                debugApiV4.getResponse()
            );
        }

        throw new IllegalArgumentException("Unsupported debug API: " + content.getClass().getSimpleName());
    }

    private List<io.gravitee.definition.model.debug.DebugStep> convert(List<PolicyStep<?>> policySteps) {
        return policySteps.stream().map(this::convert).collect(Collectors.toList());
    }

    private io.gravitee.definition.model.debug.DebugStep convert(PolicyStep<?> ds) {
        final io.gravitee.definition.model.debug.DebugStep debugStep = new io.gravitee.definition.model.debug.DebugStep();
        debugStep.setPolicyInstanceId(ds.getId());
        debugStep.setPolicyId(ds.getPolicyId());
        debugStep.setDuration(ds.elapsedTime().toNanos());
        debugStep.setStatus(ds.getStatus());
        debugStep.setCondition(ds.getCondition());
        debugStep.setError(ds.getError());
        debugStep.setStage(ds.getFlowPhase());
        if (ds.getExecutionPhase() == ExecutionPhase.REQUEST) {
            debugStep.setScope(PolicyScope.ON_REQUEST);
        } else if (ds.getExecutionPhase() == ExecutionPhase.RESPONSE) {
            debugStep.setScope(PolicyScope.ON_RESPONSE);
        }

        Map<String, Object> result = new HashMap<>();
        ds
            .getDiff()
            .forEach((key, value) -> {
                if (PolicyStep.DIFF_KEY_HEADERS.equals(key)) {
                    // Headers are converted to Map<String, List<String>>
                    result.put(PolicyStep.DIFF_KEY_HEADERS, ((HttpHeaders) value).toListValuesMap());
                } else if (PolicyStep.DIFF_KEY_BODY_BUFFER.equals(key)) {
                    // Body is converted from Buffer to String
                    result.put(PolicyStep.DIFF_KEY_BODY, value.toString());
                } else {
                    // Everything else is kept as is
                    result.put(key, value);
                }
            });

        debugStep.setResult(result);
        return debugStep;
    }
}
