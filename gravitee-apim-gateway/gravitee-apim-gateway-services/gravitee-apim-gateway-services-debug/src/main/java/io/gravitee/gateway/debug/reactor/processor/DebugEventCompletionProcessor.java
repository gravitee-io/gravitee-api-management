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

        LOGGER.info("We are finally in the processor for event {}", debugApiComponent.getEventId());

        try {
            // FIXME: instead of fetching the event, maybe we should use DebugApi from above
            // Plutot que décrire dans le contexte, peut-être qu'il faut écrire dans cet objet Api directement ?
            final Optional<Event> optionalEvent = eventRepository.findById(debugApiComponent.getEventId());
            if (optionalEvent.isPresent()) {
                final Event event = optionalEvent.get();
                // Read API definition from event
                io.gravitee.definition.model.debug.DebugApi debugApi = null;
                try {
                    debugApi = objectMapper.readValue(event.getPayload(), io.gravitee.definition.model.debug.DebugApi.class);
                } catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }

                debugApi.setDebugSteps(convert(debugContext.getDebugSteps()));

                LOGGER.error("Is response ended ? {}", debugContext.response().ended());
                LOGGER.error(
                    "Response content {}",
                    ((VertxHttpServerResponseDebugDecorator) debugContext.response()).getBuffer().toString()
                );

                HttpResponse response = new HttpResponse();
                Map<String, List<String>> headers = convertHeaders(debugContext.response().headers());
                response.setHeaders(headers);
                response.statusCode(debugContext.response().status());
                response.setBody(((VertxHttpServerResponseDebugDecorator) debugContext.response()).getBuffer().toString());
                debugApi.setResponse(response);
                event
                    .getProperties()
                    .put(
                        io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(),
                        ApiDebugStatus.SUCCESS.name()
                    );

                // FIXME: doing the update here make we do not have the response
                event.setPayload(objectMapper.writeValueAsString(debugApi));
                eventRepository.update(event);
            }
        } catch (TechnicalException | JsonProcessingException e) {
            // TODO: throw exception to make handling fail ?
            e.printStackTrace();
        }
    }

    Map<String, List<String>> convertHeaders(HttpHeaders headersMultimap) {
        Map<String, List<String>> headers = new HashMap<>();
        if (headersMultimap != null) {
            headersMultimap.forEach(e -> headers.put(e.getKey(), headersMultimap.getAll(e.getKey())));
        }
        return headers;
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
