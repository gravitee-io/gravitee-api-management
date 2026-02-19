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
package io.gravitee.gateway.debug.reactor.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.debug.DebugMetrics;
import io.gravitee.definition.model.debug.PreprocessorStep;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.debug.vertx.VertxHttpServerResponseDebugDecorator;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.debug.vertx.TimeoutServerResponseDebugDecorator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DebugEventCompletionProcessor extends AbstractProcessor<ExecutionContext> {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DebugEventCompletionProcessor(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(ExecutionContext context) {
        final DebugExecutionContext debugContext = (DebugExecutionContext) context;
        final DebugApiV2 debugApiComponent = (DebugApiV2) debugContext.getComponent(Api.class);

        final Vertx vertx = context.getComponent(Vertx.class);
        vertx.executeBlocking((Handler<Promise<Void>>) promise -> {
            Event event = null;
            try {
                event = eventRepository.findById(debugApiComponent.getEventId()).orElseThrow(TechnicalException::new);
                final io.gravitee.definition.model.debug.DebugApiV2 debugApi = computeDebugApiEventPayload(debugContext, debugApiComponent);
                updateEvent(event.updatePayload(objectMapper.writeValueAsString(debugApi)),ApiDebugStatus.SUCCESS);
            } catch (JsonProcessingException | TechnicalException e) {
                log.error("Error occurs while saving debug event", e);
                failEvent(event);
            }
            promise.complete();
        }, result -> {
            // Push response to the next handler
            next.handle(context);
        });
    }

    private io.gravitee.definition.model.debug.DebugApiV2 computeDebugApiEventPayload(
            DebugExecutionContext debugContext,
            DebugApiV2 debugApiComponent
    ) {
        final io.gravitee.definition.model.debug.DebugApiV2 debugApi = convert(debugApiComponent);
        PreprocessorStep preprocessorStep = createPreprocessorStep(debugContext);
        debugApi.setPreprocessorStep(preprocessorStep);
        debugApi.setDebugSteps(convert(debugContext.getDebugSteps()));

        HttpResponse response = createResponse(
                debugContext.response().headers(),
                debugContext.response().status(),
                getResponseBuffer(debugContext)
        );
        debugApi.setResponse(response);

        HttpResponse invokerResponse = createResponse(
                debugContext.getInvokerResponse().getHeaders(),
                debugContext.getInvokerResponse().getStatus(),
                debugContext.getInvokerResponse().getBuffer()
        );
        debugApi.setBackendResponse(invokerResponse);

        debugApi.setMetrics(createMetrics(debugContext.request().metrics()));

        return debugApi;
    }

    private Buffer getResponseBuffer(DebugExecutionContext debugContext) {
        Response response = debugContext.response();
        if (response instanceof TimeoutServerResponseDebugDecorator) {
            response = ((TimeoutServerResponseDebugDecorator) response).response();
        }
        return ((VertxHttpServerResponseDebugDecorator) response).getBuffer();
    }

    private DebugMetrics createMetrics(io.gravitee.reporter.api.http.Metrics requestMetrics) {
        final DebugMetrics metrics = new DebugMetrics();
        if (requestMetrics != null) {
            metrics.setApiResponseTimeMs(requestMetrics.getApiResponseTimeMs());
            metrics.setProxyLatencyMs(requestMetrics.getProxyLatencyMs());
            metrics.setProxyResponseTimeMs(requestMetrics.getProxyResponseTimeMs());
        }
        return metrics;
    }

    private PreprocessorStep createPreprocessorStep(DebugExecutionContext debugContext) {
        final PreprocessorStep preprocessorStep = new PreprocessorStep();
        preprocessorStep.setAttributes(debugContext.getInitialAttributes());
        preprocessorStep.setHeaders(convertHeaders(debugContext.getInitialHeaders()));
        return preprocessorStep;
    }

    private HttpResponse createResponse(HttpHeaders httpHeaders, int statusCode, Buffer bodyBuffer) {
        HttpResponse response = new HttpResponse();
        Map<String, List<String>> headers = convertHeaders(httpHeaders);
        response.setHeaders(headers);
        response.statusCode(statusCode);
        response.setBody(bodyBuffer.toString());
        return response;
    }

    Map<String, List<String>> convertHeaders(HttpHeaders headersMultimap) {
        Map<String, List<String>> headers = new HashMap<>();
        if (headersMultimap != null) {
            headersMultimap.forEach(e -> headers.put(e.getKey(), headersMultimap.getAll(e.getKey())));
        }
        return headers;
    }

    private void failEvent(io.gravitee.repository.management.model.Event debugEvent) {
        try {
            if (debugEvent != null) {
                updateEvent(debugEvent, ApiDebugStatus.ERROR);
            }
        } catch (TechnicalException e) {
            log.error("Error when updating event {} with ERROR status", debugEvent.getId());
        }
    }

    private void updateEvent(io.gravitee.repository.management.model.Event debugEvent, ApiDebugStatus apiDebugStatus)
            throws TechnicalException {
        eventRepository.update(debugEvent
                .updateProperties(io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(), apiDebugStatus.name()));
    }

    protected io.gravitee.definition.model.debug.DebugApiV2 convert(DebugApiV2 content) {
        io.gravitee.definition.model.debug.DebugApiV2 debugAPI = new io.gravitee.definition.model.debug.DebugApiV2();
        debugAPI.setName(content.getName());
        debugAPI.setId(content.getId());
        debugAPI.setDefinitionVersion(content.getDefinitionVersion());
        debugAPI.setResponse(content.getResponse());
        debugAPI.setRequest(content.getRequest());
        debugAPI.setFlowMode(content.getDefinition().getFlowMode());
        debugAPI.setFlows(content.getDefinition().getFlows());
        debugAPI.setPathMappings(content.getDefinition().getPathMappings());
        debugAPI.setPlans(
                content
                        .getDefinition()
                        .getPlans()
                        .stream()
                        .filter(plan -> !PlanStatus.CLOSED.getLabel().equalsIgnoreCase(plan.getStatus()))
                        .collect(Collectors.toList())
        );
        debugAPI.setPaths(content.getDefinition().getPaths());
        debugAPI.setServices(content.getDefinition().getServices());
        debugAPI.setProxy(content.getDefinition().getProxy());
        debugAPI.setProperties(content.getDefinition().getProperties());
        debugAPI.setResources(content.getDefinition().getResources());
        debugAPI.setServices(content.getDefinition().getServices());
        debugAPI.setResponseTemplates(content.getDefinition().getResponseTemplates());
        return debugAPI;
    }

    private List<io.gravitee.definition.model.debug.DebugStep> convert(List<DebugStep<?>> debugSteps) {
        return debugSteps.stream().map(this::convert).collect(Collectors.toList());
    }

    private io.gravitee.definition.model.debug.DebugStep convert(DebugStep<?> ds) {
        final io.gravitee.definition.model.debug.DebugStep debugStep = new io.gravitee.definition.model.debug.DebugStep();
        debugStep.setPolicyInstanceId(ds.getPolicyInstanceId());
        debugStep.setPolicyId(ds.getPolicyId());
        debugStep.setDuration(ds.elapsedTime().toNanos());
        debugStep.setStatus(ds.getStatus());
        debugStep.setCondition(ds.getCondition());
        debugStep.setError(ds.getError());
        debugStep.setScope(ds.getPolicyScope());
        debugStep.setStage(ds.policyMetadata().metadata().getOrDefault(PolicyMetadata.MetadataKeys.STAGE, "UNDEFINED").toString());

        Map<String, Object> result = new HashMap<>();
        ds
                .getDebugDiffContent()
                .forEach((key, value) -> {
                    if (DebugStep.DIFF_KEY_HEADERS.equals(key)) {
                        // Headers are converted to Map<String, List<String>>
                        result.put(DebugStep.DIFF_KEY_HEADERS, convertHeaders((HttpHeaders) value));
                    } else if (DebugStep.DIFF_KEY_BODY_BUFFER.equals(key)) {
                        // Body is converted from Buffer to String
                        result.put(DebugStep.DIFF_KEY_BODY, value.toString());
                    } else {
                        // Everything else is kept as is
                        result.put(key, value);
                    }
                });

        debugStep.setResult(result);
        return debugStep;
    }
}
