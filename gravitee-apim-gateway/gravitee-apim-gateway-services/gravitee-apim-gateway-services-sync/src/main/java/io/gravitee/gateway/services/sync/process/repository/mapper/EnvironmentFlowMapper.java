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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.ENVIRONMENT_FLOW_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class EnvironmentFlowMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    public Maybe<String> toId(Event environmentFlowEvent) {
        return Maybe.fromCallable(() -> {
            String environmentFlowId = null;
            if (environmentFlowEvent.getProperties() != null) {
                environmentFlowId = environmentFlowEvent.getProperties().get(ENVIRONMENT_FLOW_ID.getValue());
            }
            if (environmentFlowId == null) {
                log.warn("Unable to extract environment flow info from event [{}].", environmentFlowEvent.getId());
            }
            return environmentFlowId;
        });
    }

    public Maybe<ReactableEnvironmentFlow> to(Event environmentFlowEvent) {
        return Maybe.fromCallable(() -> {
            try {
                // Read API definition from event
                var environmentFlow = objectMapper.readValue(environmentFlowEvent.getPayload(), SharedPolicyGroup.class);

                var environmentFlowDefinition = objectMapper.readValue(
                    environmentFlow.getDefinition(),
                    io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow.class
                );

                final ReactableEnvironmentFlow reactableEnvironmentFlow = ReactableEnvironmentFlow
                    .builder()
                    .id(environmentFlow.getId())
                    .name(environmentFlow.getName())
                    .definition(environmentFlowDefinition)
                    .deployedAt(environmentFlow.getDeployedAt())
                    .build();

                environmentService.fill(environmentFlow.getEnvironmentId(), reactableEnvironmentFlow);

                return reactableEnvironmentFlow;
            } catch (Exception e) {
                // Log the error and ignore this event.
                log.error("Unable to extract environment flow definition from event [{}].", environmentFlowEvent.getId(), e);
                return null;
            }
        });
    }
}
