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
            validateHttpGroup(httpGroup, errors);
        }
    }

    private void validateHttpGroup(EndpointGroup group, List<Validator.Error> errors) {
        if (group.getEndpoints() == null) {
            return;
        }
        Service groupHealthCheck = healthCheckOf(group.getServices());
        boolean groupScheduleReported = false;
        for (Endpoint endpoint : group.getEndpoints()) {
            if (!probeMayRun(groupHealthCheck, endpoint)) {
                continue;
            }
            Service endpointHealthCheck = healthCheckOf(endpoint);
            if (overridesGroupConfiguration(endpointHealthCheck)) {
                validateSchedule(
                    "endpointGroups[%s].endpoints[%s].services.healthCheck.configuration.schedule".formatted(
                        group.getName(),
                        endpoint.getName()
                    ),
                    endpointHealthCheck,
                    errors
                );
            } else if (!groupScheduleReported) {
                validateSchedule(
                    "endpointGroups[%s].services.healthCheck.configuration.schedule".formatted(group.getName()),
                    groupHealthCheck,
                    errors
                );
                groupScheduleReported = true;
            }
        }
    }

    /**
     * Mirrors {@code HttpHealthCheckHelper.isServiceEnabled}: the probe runs when the endpoint enables it,
     * or when the group enables it and the endpoint does not enable its own.
     * This will check secondary endpoints CRON expression as well. This is done on purpose to cover the case when
     * the endpoint is promoted to primary. As a result, the Gateway will then start a probe;
     * hence a valid CRON expression is expected.
     */
    private static boolean probeMayRun(Service groupHealthCheck, Endpoint endpoint) {
        Service endpointHealthCheck = healthCheckOf(endpoint);
        if (isEnabledHttpHealthCheck(endpointHealthCheck)) {
            return true;
        }
        return isEnabledHttpHealthCheck(groupHealthCheck) && (endpointHealthCheck == null || !endpointHealthCheck.isEnabled());
    }

    private static boolean overridesGroupConfiguration(Service endpointHealthCheck) {
        return endpointHealthCheck != null && endpointHealthCheck.isOverrideConfiguration();
    }

    private void validateSchedule(String fieldPath, Service healthCheck, List<Validator.Error> errors) {
        ParsedSchedule schedule = parseSchedule(fieldPath, healthCheck, errors);
        if (schedule.jsonInvalid()) {
            return;
        }
        if (schedule.isPresent()) {
            validateCron(fieldPath, schedule.value(), errors);
            return;
        }
        errors.add(Validator.Error.severe("property [%s] is required", fieldPath));
    }

    private static Service healthCheckOf(EndpointGroupServices services) {
        return services == null ? null : services.getHealthCheck();
    }

    private static Service healthCheckOf(Endpoint endpoint) {
        if (endpoint.getServices() == null) {
            return null;
        }
        return endpoint.getServices().getHealthCheck();
    }

    private static boolean isEnabledHttpHealthCheck(Service healthCheck) {
        return healthCheck != null && healthCheck.isEnabled() && HTTP_HEALTH_CHECK_TYPE.equals(healthCheck.getType());
    }

    private ParsedSchedule parseSchedule(String fieldPath, Service healthCheck, List<Validator.Error> errors) {
        if (healthCheck == null) {
            return ParsedSchedule.missing();
        }
        try {
            return ParsedSchedule.of(readSchedule(healthCheck.getConfiguration()));
        } catch (JsonProcessingException e) {
            errors.add(Validator.Error.severe("property [%s] has invalid JSON configuration", fieldPath));
            return ParsedSchedule.jsonError();
        }
    }

    private void validateCron(String fieldPath, String schedule, List<Validator.Error> errors) {
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedSchedule(String value, boolean jsonInvalid) {
        static ParsedSchedule missing() {
            return new ParsedSchedule(null, false);
        }

        static ParsedSchedule jsonError() {
            return new ParsedSchedule(null, true);
        }

        static ParsedSchedule of(String value) {
            return new ParsedSchedule(value, false);
        }

        boolean isPresent() {
            return hasText(value);
        }
    }
}
