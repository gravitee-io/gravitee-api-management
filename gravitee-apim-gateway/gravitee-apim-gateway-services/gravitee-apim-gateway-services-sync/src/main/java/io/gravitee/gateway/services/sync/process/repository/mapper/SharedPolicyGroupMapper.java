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

import static io.gravitee.repository.management.model.Event.EventProperties.SHARED_POLICY_GROUP_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SharedPolicyGroupMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    public Maybe<String> toId(Event sharedPolicyGroupEvent) {
        return Maybe.fromCallable(() -> {
            String sharedPolicyGroupId = null;
            if (sharedPolicyGroupEvent.getProperties() != null) {
                sharedPolicyGroupId = sharedPolicyGroupEvent.getProperties().get(SHARED_POLICY_GROUP_ID.getValue());
            }
            if (sharedPolicyGroupId == null) {
                log.warn("Unable to extract shared policy group info from event [{}].", sharedPolicyGroupEvent.getId());
            }
            return sharedPolicyGroupId;
        });
    }

    public Maybe<ReactableSharedPolicyGroup> to(Event sharedPolicyGroupEvent) {
        return Maybe.fromCallable(() -> {
            try {
                var sharedPolicyGroupDefinition = objectMapper.readValue(sharedPolicyGroupEvent.getPayload(), SharedPolicyGroup.class);

                final ReactableSharedPolicyGroup reactableSharedPolicyGroup = ReactableSharedPolicyGroup
                    .builder()
                    .id(sharedPolicyGroupDefinition.getId())
                    .name(sharedPolicyGroupDefinition.getName())
                    .environmentId(sharedPolicyGroupDefinition.getEnvironmentId())
                    .definition(sharedPolicyGroupDefinition)
                    .deployedAt(sharedPolicyGroupDefinition.getDeployedAt())
                    .build();

                environmentService.fill(sharedPolicyGroupDefinition.getEnvironmentId(), reactableSharedPolicyGroup);

                return reactableSharedPolicyGroup;
            } catch (Exception e) {
                // Log the error and ignore this event.
                log.error("Unable to extract shared policy group definition from event [{}].", sharedPolicyGroupEvent.getId(), e);
                return null;
            }
        });
    }
}
