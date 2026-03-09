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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionMetadataSanitizer;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionValidationServiceImplTest {

    private SubscriptionValidationService cut;

    @Mock
    private EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Mock
    private SubscriptionMetadataSanitizer subscriptionMetadataSanitizer;

    private PlanEntity planEntity;

    @BeforeEach
    public void setUp() {
        cut = new SubscriptionValidationServiceImpl(entrypointConnectorPluginService, subscriptionMetadataSanitizer);
        lenient()
            .when(subscriptionMetadataSanitizer.sanitizeAndValidate(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        planEntity = new PlanEntity();
        PlanSecurity security = new PlanSecurity();
        planEntity.setSecurity(security);
    }

    @Nested
    class SubscriptionType {

        @BeforeEach
        public void beforeEach() {
            planEntity.setMode(PlanMode.PUSH);
        }

        @Nested
        class NewSubscription {

            @Test
            void should_throw_when_no_entrypointId_defined() {
                NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
                newSubscriptionEntity.setConfiguration(new SubscriptionConfigurationEntity());

                PlanEntity planEntity = new PlanEntity();
                planEntity.setMode(PlanMode.PUSH);

                assertThrows(SubscriptionEntrypointIdMissingException.class, () ->
                    cut.validateAndSanitize(planEntity, newSubscriptionEntity)
                );
            }

            @Test
            void should_sanitize_configuration_when_valid_configuration() {
                NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
                SubscriptionConfigurationEntity configuration = new SubscriptionConfigurationEntity();
                configuration.setEntrypointId("entrypoint_id");
                configuration.setEntrypointConfiguration("{\"field\": \"to_sanitize\"}");
                newSubscriptionEntity.setConfiguration(configuration);

                String sanitizedCfg = "{\"field\": \"sanitized\"}";
                when(
                    entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                        configuration.getEntrypointId(),
                        configuration.getEntrypointConfiguration()
                    )
                ).thenReturn(sanitizedCfg);

                cut.validateAndSanitize(planEntity, newSubscriptionEntity);

                assertThat(newSubscriptionEntity.getConfiguration().getEntrypointConfiguration()).isEqualTo(sanitizedCfg);
            }
        }

        @Nested
        class UpdateSubscription {

            @Test
            void should_throw_when_no_entrypointId_defined() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                SubscriptionConfigurationEntity configuration = new SubscriptionConfigurationEntity();
                updateSubscriptionEntity.setConfiguration(configuration);

                assertThrows(SubscriptionEntrypointIdMissingException.class, () ->
                    cut.validateAndSanitize(planEntity, updateSubscriptionEntity)
                );
            }

            @Test
            void should_sanitize_configuration_when_valid_configuration() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                SubscriptionConfigurationEntity configuration = new SubscriptionConfigurationEntity();
                configuration.setEntrypointId("entrypoint_id");
                configuration.setEntrypointConfiguration("{\"field\": \"to_sanitize\"}");
                updateSubscriptionEntity.setConfiguration(configuration);

                String sanitizedCfg = "{\"field\": \"sanitized\"}";
                when(
                    entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                        configuration.getEntrypointId(),
                        configuration.getEntrypointConfiguration()
                    )
                ).thenReturn(sanitizedCfg);

                cut.validateAndSanitize(planEntity, updateSubscriptionEntity);

                assertThat(updateSubscriptionEntity.getConfiguration().getEntrypointConfiguration()).isEqualTo(sanitizedCfg);
            }
        }

        @Nested
        class UpdateSubscriptionConfiguration {

            @Test
            void should_throw_when_no_entrypointId_defined() {
                UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
                SubscriptionConfigurationEntity configuration = new SubscriptionConfigurationEntity();
                updateSubscriptionConfigurationEntity.setConfiguration(configuration);

                assertThrows(SubscriptionEntrypointIdMissingException.class, () ->
                    cut.validateAndSanitize(planEntity, updateSubscriptionConfigurationEntity)
                );
            }

            @Test
            void should_sanitize_configuration_when_valid_configuration() {
                UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
                SubscriptionConfigurationEntity configuration = new SubscriptionConfigurationEntity();
                configuration.setEntrypointId("entrypoint_id");
                configuration.setEntrypointConfiguration("{\"field\": \"to_sanitize\"}");
                updateSubscriptionConfigurationEntity.setConfiguration(configuration);

                String sanitizedCfg = "{\"field\": \"sanitized\"}";
                when(
                    entrypointConnectorPluginService.validateEntrypointSubscriptionConfiguration(
                        configuration.getEntrypointId(),
                        configuration.getEntrypointConfiguration()
                    )
                ).thenReturn(sanitizedCfg);

                cut.validateAndSanitize(planEntity, updateSubscriptionConfigurationEntity);

                assertThat(updateSubscriptionConfigurationEntity.getConfiguration().getEntrypointConfiguration()).isEqualTo(sanitizedCfg);
            }
        }
    }

    @Nested
    class OtherType {

        @BeforeEach
        public void beforeEach() {
            planEntity.getSecurity().setType(PlanSecurityType.JWT.getLabel());
        }

        @Nested
        class NewSubscription {

            @Test
            public void should_do_nothing() {
                NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
                newSubscriptionEntity.setConfiguration(null);
                planEntity.setSecurity(new PlanSecurity());

                cut.validateAndSanitize(planEntity, newSubscriptionEntity);

                assertThat(newSubscriptionEntity.getConfiguration()).isNull();
            }
        }

        @Nested
        class UpdateSubscription {

            @Test
            public void should_do_nothing() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setConfiguration(null);
                planEntity.setSecurity(new PlanSecurity());

                cut.validateAndSanitize(planEntity, updateSubscriptionEntity);

                assertThat(updateSubscriptionEntity.getConfiguration()).isNull();
            }
        }

        @Nested
        class UpdateSubscriptionConfiguration {

            @Test
            public void should_do_nothing() {
                UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
                updateSubscriptionConfigurationEntity.setConfiguration(null);
                planEntity.setSecurity(new PlanSecurity());

                cut.validateAndSanitize(planEntity, updateSubscriptionConfigurationEntity);

                assertThat(updateSubscriptionConfigurationEntity.getConfiguration()).isNull();
            }
        }
    }
}
