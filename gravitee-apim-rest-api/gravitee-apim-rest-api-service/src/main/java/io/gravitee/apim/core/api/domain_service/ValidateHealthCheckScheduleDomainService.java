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
package io.gravitee.apim.core.api.domain_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.cron.CronTrigger;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateHealthCheckScheduleDomainService {

    static final String HTTP_HEALTH_CHECK_TYPE = "http-health-check";

    private final ObjectMapper objectMapper;

    public void validate(List<? extends AbstractEndpointGroup<?>> endpointGroups, List<Validator.Error> errors) {
        if (endpointGroups == null) {
            return;
        }
        for (AbstractEndpointGroup<?> group : endpointGroups) {
            if (!(group instanceof EndpointGroup httpGroup)) {
                continue;
            }
            validateGroupHealthCheck(httpGroup.getName(), httpGroup.getServices(), errors);
            if (httpGroup.getEndpoints() == null) {
                continue;
            }
            for (Endpoint endpoint : httpGroup.getEndpoints()) {
                validateEndpointHealthCheck(httpGroup, endpoint, errors);
            }
        }
    }

    private void validateEndpointHealthCheck(EndpointGroup group, Endpoint endpoint, List<Validator.Error> errors) {
        EndpointServices endpointServices = endpoint.getServices();
        if (endpointServices == null || endpointServices.getHealthCheck() == null) {
            return;
        }
        Service endpointHealthCheck = endpointServices.getHealthCheck();
        if (!isEnabledHttpHealthCheck(endpointHealthCheck)) {
            return;
        }
        if (!endpointHealthCheck.isOverrideConfiguration()) {
            return;
        }
        validateSchedule(
            "endpointGroups[%s].endpoints[%s].services.healthCheck.configuration.schedule".formatted(group.getName(), endpoint.getName()),
            endpointHealthCheck.getConfiguration(),
            errors
        );
    }

    private void validateGroupHealthCheck(String groupName, EndpointGroupServices services, List<Validator.Error> errors) {
        if (services == null || services.getHealthCheck() == null) {
            return;
        }
        Service groupHealthCheck = services.getHealthCheck();
        if (!isEnabledHttpHealthCheck(groupHealthCheck)) {
            return;
        }
        validateSchedule(
            "endpointGroups[%s].services.healthCheck.configuration.schedule".formatted(groupName),
            groupHealthCheck.getConfiguration(),
            errors
        );
    }

    private static boolean isEnabledHttpHealthCheck(Service healthCheck) {
        return healthCheck.isEnabled() && HTTP_HEALTH_CHECK_TYPE.equals(healthCheck.getType());
    }

    private void validateSchedule(String fieldPath, String configuration, List<Validator.Error> errors) {
        String schedule;
        try {
            schedule = readSchedule(configuration);
        } catch (JsonProcessingException e) {
            errors.add(Validator.Error.severe("property [%s] has invalid JSON configuration", fieldPath));
            return;
        }
        if (schedule == null || schedule.isBlank()) {
            errors.add(Validator.Error.severe("property [%s] is required", fieldPath));
            return;
        }
        try {
            new CronTrigger(schedule);
        } catch (IllegalArgumentException e) {
            errors.add(Validator.Error.severe("property [%s] value [%s] is invalid: %s", fieldPath, schedule, e.getMessage()));
        }
    }

    private String readSchedule(String configuration) throws JsonProcessingException {
        if (configuration == null || configuration.isBlank()) {
            return null;
        }
        JsonNode node = objectMapper.readTree(configuration);
        JsonNode scheduleNode = node.get("schedule");
        if (scheduleNode == null || scheduleNode.isNull()) {
            return null;
        }
        return scheduleNode.asText();
    }
}
