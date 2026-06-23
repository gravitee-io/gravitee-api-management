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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.service.common.ScheduleMinimumIntervalValidator;
import io.gravitee.rest.api.service.exceptions.HealthcheckInvalidException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EndpointHealthCheckScheduleValidator {

    private static final String SCHEDULE_CONFIGURATION_FIELD = "schedule";

    private final ObjectMapper objectMapper;
    private final ScheduleMinimumIntervalValidator scheduleMinimumIntervalValidator;

    public EndpointHealthCheckScheduleValidator(
        final ObjectMapper objectMapper,
        final ScheduleMinimumIntervalValidator scheduleMinimumIntervalValidator
    ) {
        this.objectMapper = objectMapper;
        this.scheduleMinimumIntervalValidator = scheduleMinimumIntervalValidator;
    }

    public void validate(List<EndpointGroup> endpointGroups) {
        if (endpointGroups == null) {
            return;
        }

        endpointGroups.forEach(group -> {
            if (group.getServices() != null) {
                validate(group.getServices().getHealthCheck());
            }
            if (group.getEndpoints() != null) {
                group
                    .getEndpoints()
                    .stream()
                    .filter(endpoint -> endpoint.getServices() != null)
                    .forEach(endpoint -> validate(endpoint.getServices().getHealthCheck()));
            }
        });
    }

    public void validate(Service healthCheck) {
        if (
            healthCheck == null || !scheduleMinimumIntervalValidator.isHealthcheckLimitEnabled() || isBlank(healthCheck.getConfiguration())
        ) {
            return;
        }

        try {
            var scheduleNode = objectMapper.readTree(healthCheck.getConfiguration()).get(SCHEDULE_CONFIGURATION_FIELD);
            if (scheduleNode != null && !scheduleNode.isNull()) {
                var schedule = scheduleNode.asText();
                scheduleMinimumIntervalValidator.validateHealthcheck("services.healthcheck.schedule", schedule);
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new HealthcheckInvalidException(healthCheck.getType());
        }
    }
}
