/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.reactor.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.debug.vertx.VertxHttpServerResponseDebugDecorator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugEventCompletionProcessor extends AbstractProcessor<ExecutionContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(DebugEventCompletionProcessor.class);
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DebugEventCompletionProcessor(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(ExecutionContext context) {
        final DebugExecutionContext debugContext = (DebugExecutionContext) context;
        final DebugApi debugApiComponent = (DebugApi) debugContext.getComponent(Api.class);

        Event event = null;

        try {
             event = eventRepository.findById(debugApiComponent.getEventId()).orElseThrow(TechnicalException::new);
            final io.gravitee.definition.model.debug.DebugApi debugApi = computeDebugApiEventPayload(debugContext, debugApiComponent);

            event.setPayload(objectMapper.writeValueAsString(debugApi));
            updateEvent(event, ApiDebugStatus.SUCCESS);
        } catch (JsonProcessingException | TechnicalException e) {
            LOGGER.error("Error occurs while saving debug event", e);
            failEvent(event);
        }

        // Push response to the next handler
        next.handle(context);
    }

    private io.gravitee.definition.model.debug.DebugApi computeDebugApiEventPayload(DebugExecutionContext debugContext, DebugApi debugApiComponent) {
        final io.gravitee.definition.model.debug.DebugApi debugApi = convert(debugApiComponent);
        // FIXME : to handle properly adfter discussion
        if (debugContext.getInitialAttributes().containsKey("gravitee.attribute.entrypoint")) {
            debugContext.getInitialAttributes().put("gravitee.attribute.entrypoint", debugContext.getInitialAttributes().get("gravitee.attribute.entrypoint").getClass().getSimpleName());
        }
        debugApi.setInitialAttributes(debugContext.getInitialAttributes());
        debugApi.setDebugSteps(convert(debugContext.getDebugSteps()));
        HttpResponse response = new HttpResponse();
        Map<String, List<String>> headers = convertHeaders(debugContext.response().headers());
        response.setHeaders(headers);
        response.statusCode(debugContext.response().status());
        response.setBody(((VertxHttpServerResponseDebugDecorator) debugContext.response()).getBuffer().toString());
        debugApi.setResponse(response);
        return debugApi;
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
            LOGGER.error("Error when updating event {} with ERROR status", debugEvent.getId());
        }
    }

    private void updateEvent(io.gravitee.repository.management.model.Event debugEvent, ApiDebugStatus apiDebugStatus)
            throws TechnicalException {
        debugEvent
                .getProperties()
                .put(io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(), apiDebugStatus.name());
        eventRepository.update(debugEvent);
    }

    private io.gravitee.definition.model.debug.DebugApi convert(DebugApi content) {
        io.gravitee.definition.model.debug.DebugApi debugAPI = new io.gravitee.definition.model.debug.DebugApi();
        debugAPI.setName(content.getName());
        debugAPI.setId(content.getId());
        debugAPI.setDefinitionVersion(content.getDefinitionVersion());
        debugAPI.setResponse(content.getResponse());
        debugAPI.setRequest(content.getRequest());
        debugAPI.setFlowMode(content.getFlowMode());
        debugAPI.setFlows(content.getFlows());
        debugAPI.setPathMappings(content.getPathMappings());
        debugAPI.setPlans(content.getPlans());
        debugAPI.setPaths(content.getPaths());
        debugAPI.setServices(content.getServices());
        debugAPI.setProxy(content.getProxy());
        debugAPI.setProperties(content.getProperties());
        debugAPI.setResources(content.getResources());
        debugAPI.setServices(content.getServices());
        debugAPI.setResponseTemplates(content.getResponseTemplates());
        return debugAPI;
    }

    private List<io.gravitee.definition.model.debug.DebugStep> convert(List<DebugStep<?>> debugSteps) {
        return debugSteps
                .stream()
                .map(
                        ds -> {
                            final io.gravitee.definition.model.debug.DebugStep debugStep = new io.gravitee.definition.model.debug.DebugStep();
                            debugStep.setPolicyInstanceId(ds.getPolicyInstanceId());
                            debugStep.setPolicyId(ds.getPolicyId());
                            debugStep.setDuration(ds.elapsedTime().toNanos());
                            debugStep.setStatus(io.gravitee.definition.model.debug.DebugStep.Status.COMPLETED);
                            debugStep.setScope(ds.getPolicyScope());
                            if (ds.getDebugDiffContent().containsKey("headers")) {
                                ds.getDebugDiffContent().put("headers", convertHeaders((HttpHeaders) ds.getDebugDiffContent().get("headers")));
                            }
                            if (ds.getDebugDiffContent().containsKey("bodyBuffer")) {
                                ds.getDebugDiffContent().put("body", ds.getDebugDiffContent().get("bodyBuffer").toString());
                                ds.getDebugDiffContent().remove("bodyBuffer");
                            }
                            debugStep.setResult(ds.getDebugDiffContent());
                            return debugStep;
                        }
                )
                .collect(Collectors.toList());
    }
}
