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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiCRDFixtures;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateHealthCheckScheduleDomainServiceTest {

    private ValidateHealthCheckScheduleDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new ValidateHealthCheckScheduleDomainService(new ObjectMapper());
    }

    @Test
    void should_reject_five_field_cron_on_group_health_check() {
        var errors = new ArrayList<Validator.Error>();
        var group = endpointGroupWithGroupHealthCheck("*/30 * * * *");

        cut.validate(List.of(group), errors);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().isSevere()).isTrue();
        assertThat(errors.getFirst().getMessage()).contains("*/30 * * * *");
        assertThat(errors.getFirst().getMessage()).contains("6 fields");
        assertThat(errors.getFirst().getMessage()).contains("endpointGroups[default-group].services.healthCheck.configuration.schedule");
    }

    @Test
    void should_accept_valid_six_field_cron_on_group_health_check() {
        var errors = new ArrayList<Validator.Error>();
        var group = endpointGroupWithGroupHealthCheck("0 */30 * * * *");

        cut.validate(List.of(group), errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void should_skip_disabled_health_check_with_invalid_schedule() {
        var errors = new ArrayList<Validator.Error>();
        var healthCheck = healthCheckService("*/30 * * * *", true);
        healthCheck.setEnabled(false);
        var group = (EndpointGroup) ApiCRDFixtures.newBaseSpec().build().getEndpointGroups().getFirst();
        group.getServices().setHealthCheck(healthCheck);

        cut.validate(List.of(group), errors);

        assertThat(errors).isEmpty();
    }

    @Test
    void should_reject_invalid_schedule_on_endpoint_override() {
        var errors = new ArrayList<Validator.Error>();
        var group = (EndpointGroup) ApiCRDFixtures.newBaseSpec().build().getEndpointGroups().getFirst();
        var endpoint = group.getEndpoints().getFirst();
        endpoint.setServices(EndpointServices.builder().healthCheck(healthCheckService("*/30 * * * *", true)).build());

        cut.validate(List.of(group), errors);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getMessage()).contains("endpoints[default-endpoint]");
        assertThat(errors.getFirst().getMessage()).contains("6 fields");
    }

    @Test
    void should_reject_missing_schedule_on_enabled_health_check() {
        var errors = new ArrayList<Validator.Error>();
        var group = endpointGroupWithGroupHealthCheck(null);

        cut.validate(List.of(group), errors);

        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getMessage()).contains("is required");
    }

    private static EndpointGroup endpointGroupWithGroupHealthCheck(String schedule) {
        var group = (EndpointGroup) ApiCRDFixtures.newBaseSpec().build().getEndpointGroups().getFirst();
        group.setServices(EndpointGroupServices.builder().healthCheck(healthCheckService(schedule, false)).build());
        return group;
    }

    private static Service healthCheckService(String schedule, boolean overrideConfiguration) {
        var configuration = schedule == null ? "{}" : "{\"schedule\":\"%s\"}".formatted(schedule);
        return Service.builder()
            .type(ValidateHealthCheckScheduleDomainService.HTTP_HEALTH_CHECK_TYPE)
            .enabled(true)
            .overrideConfiguration(overrideConfiguration)
            .configuration(configuration)
            .build();
    }
}
