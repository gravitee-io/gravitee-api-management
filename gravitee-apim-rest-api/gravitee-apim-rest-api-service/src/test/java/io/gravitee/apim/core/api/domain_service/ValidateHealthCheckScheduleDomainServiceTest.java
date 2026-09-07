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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateHealthCheckScheduleDomainServiceTest {

    private static final String VALID_CRON = "0 */30 * * * *";
    private static final String INVALID_FIVE_FIELD_CRON = "*/30 * * * *";

    private ValidateHealthCheckScheduleDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new ValidateHealthCheckScheduleDomainService(new ObjectMapper());
    }

    @Nested
    class CronExpressionSyntax {

        @Test
        void should_reject_five_field_cron_on_group_health_check() {
            var errors = validate(groupWithHealthCheck(INVALID_FIVE_FIELD_CRON, inheritingEndpoint("default-endpoint")));

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().isSevere()).isTrue();
            assertThat(errors.getFirst().getMessage()).contains(INVALID_FIVE_FIELD_CRON);
            assertThat(errors.getFirst().getMessage()).contains("6 fields");
            assertThat(errors.getFirst().getMessage()).contains(
                "endpointGroups[default-group].services.healthCheck.configuration.schedule"
            );
        }

        @Test
        void should_accept_valid_six_field_cron_on_group_health_check() {
            var errors = validate(groupWithHealthCheck(VALID_CRON, inheritingEndpoint("default-endpoint")));

            assertThat(errors).isEmpty();
        }

        @Test
        void should_ignore_invalid_group_cron_when_no_endpoint_uses_it() {
            var errors = validate(group(enabledHealthCheck(false, INVALID_FIVE_FIELD_CRON), overridingEndpoint("ep-1", VALID_CRON)));

            assertThat(errors).isEmpty();
        }

        @Test
        void should_ignore_invalid_cron_on_inheriting_endpoint_when_group_config_is_used() {
            var errors = validate(group(enabledHealthCheck(false, VALID_CRON), inheritingEnabledEndpoint("ep-1", INVALID_FIVE_FIELD_CRON)));

            assertThat(errors).isEmpty();
        }

        @Test
        void should_reject_invalid_cron_on_disabled_overriding_endpoint_when_group_probe_runs() {
            var errors = validate(
                group(enabledHealthCheck(false, VALID_CRON), overridingDisabledEndpoint("ep-1", INVALID_FIVE_FIELD_CRON))
            );

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("endpoints[ep-1]");
            assertThat(errors.getFirst().getMessage()).contains("6 fields");
        }

        @Test
        void should_reject_invalid_schedule_on_endpoint_override() {
            var errors = validate(group(null, overridingEndpoint("default-endpoint", INVALID_FIVE_FIELD_CRON)));

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("endpoints[default-endpoint]");
            assertThat(errors.getFirst().getMessage()).contains("6 fields");
        }

        @Test
        void should_skip_disabled_health_check_with_invalid_schedule() {
            var errors = validate(group(healthCheck(false, false, INVALID_FIVE_FIELD_CRON), inheritingEndpoint("default-endpoint")));

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    class EmptyScheduleOnGroup {

        @Test
        void should_allow_group_without_cron_when_all_endpoints_have_one() {
            var errors = validate(
                group(enabledHealthCheck(false, null), overridingEndpoint("ep-1", VALID_CRON), overridingEndpoint("ep-2", VALID_CRON))
            );

            assertThat(errors).isEmpty();
        }

        @Test
        void should_reject_group_without_cron_when_an_endpoint_does_not_have_one() {
            var errors = validate(
                group(enabledHealthCheck(false, null), overridingEndpoint("ep-1", VALID_CRON), inheritingEndpoint("ep-2"))
            );

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("is required");
            assertThat(errors.getFirst().getMessage()).contains(
                "endpointGroups[default-group].services.healthCheck.configuration.schedule"
            );
        }
    }

    @Nested
    class EmptyScheduleOnInheritedEndpoint {

        @Test
        void should_allow_inheriting_endpoint_without_cron_when_group_has_one() {
            var errors = validate(group(enabledHealthCheck(false, VALID_CRON), inheritingEnabledEndpoint("ep-1", null)));

            assertThat(errors).isEmpty();
        }

        @Test
        void should_allow_inheriting_disabled_endpoint_without_cron_when_group_has_one() {
            var errors = validate(group(enabledHealthCheck(false, VALID_CRON), inheritingEndpoint("ep-1")));

            assertThat(errors).isEmpty();
        }

        @Test
        void should_require_group_cron_when_inheriting_endpoint_is_enabled_and_group_has_none() {
            var errors = validate(group(healthCheck(false, false, null), inheritingEnabledEndpoint("ep-1", null)));

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("is required");
            assertThat(errors.getFirst().getMessage()).contains(
                "endpointGroups[default-group].services.healthCheck.configuration.schedule"
            );
        }

        @Test
        void should_allow_inheriting_enabled_endpoint_when_disabled_group_has_a_cron() {
            var errors = validate(group(healthCheck(false, false, VALID_CRON), inheritingEnabledEndpoint("canary", null)));

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    class EmptyScheduleOnOverridingEndpoint {

        @Test
        void should_require_cron_on_enabled_overriding_endpoint_even_when_group_has_one() {
            var errors = validate(group(enabledHealthCheck(false, VALID_CRON), overridingEndpoint("ep-1", null)));

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("is required");
            assertThat(errors.getFirst().getMessage()).contains("endpoints[ep-1]");
        }

        @Test
        void should_require_cron_on_enabled_overriding_endpoint_when_group_has_none() {
            var errors = validate(group(null, overridingEndpoint("ep-1", null)));

            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().getMessage()).contains("is required");
            assertThat(errors.getFirst().getMessage()).contains("endpoints[ep-1]");
        }
    }

    private List<Validator.Error> validate(EndpointGroup group) {
        var errors = new ArrayList<Validator.Error>();
        cut.validate(List.of(group), errors);
        return errors;
    }

    private static EndpointGroup groupWithHealthCheck(String schedule, Endpoint... endpoints) {
        return group(enabledHealthCheck(false, schedule), endpoints);
    }

    private static EndpointGroup group(Service groupHealthCheck, Endpoint... endpoints) {
        return EndpointGroup.builder()
            .name("default-group")
            .type("http-proxy")
            .services(EndpointGroupServices.builder().healthCheck(groupHealthCheck).build())
            .endpoints(List.of(endpoints))
            .build();
    }

    private static Endpoint inheritingEndpoint(String name) {
        return Endpoint.builder().name(name).type("http-proxy").build();
    }

    private static Endpoint inheritingEnabledEndpoint(String name, String schedule) {
        return endpoint(name, healthCheck(true, false, schedule));
    }

    private static Endpoint overridingEndpoint(String name, String schedule) {
        return endpoint(name, healthCheck(true, true, schedule));
    }

    private static Endpoint overridingDisabledEndpoint(String name, String schedule) {
        return endpoint(name, healthCheck(false, true, schedule));
    }

    private static Endpoint endpoint(String name, Service healthCheck) {
        return Endpoint.builder()
            .name(name)
            .type("http-proxy")
            .services(EndpointServices.builder().healthCheck(healthCheck).build())
            .build();
    }

    private static Service enabledHealthCheck(boolean overrideConfiguration, String schedule) {
        return healthCheck(true, overrideConfiguration, schedule);
    }

    private static Service healthCheck(boolean enabled, boolean overrideConfiguration, String schedule) {
        var configuration = schedule == null ? "{}" : "{\"schedule\":\"%s\"}".formatted(schedule);
        return Service.builder()
            .type(ValidateHealthCheckScheduleDomainService.HTTP_HEALTH_CHECK_TYPE)
            .enabled(enabled)
            .overrideConfiguration(overrideConfiguration)
            .configuration(configuration)
            .build();
    }
}
