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

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEndsAfterClientCertificateException;
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionMetadataSanitizer;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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

    public static final String APP_ID = "appId";
    private SubscriptionValidationService cut;

    @Mock
    private EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Mock
    private ClientCertificateCrudService clientCertificateCrudService;

    @Mock
    private SubscriptionMetadataSanitizer subscriptionMetadataSanitizer;

    private SubscriptionFormQueryServiceInMemory subscriptionFormQueryService;

    private PlanEntity planEntity;

    @BeforeEach
    void setUp() {
        subscriptionFormQueryService = new SubscriptionFormQueryServiceInMemory();
        cut = new SubscriptionValidationServiceImpl(
            entrypointConnectorPluginService,
            subscriptionMetadataSanitizer,
            subscriptionFormQueryService,
            clientCertificateCrudService
        );
        lenient()
            .when(subscriptionMetadataSanitizer.sanitizeAndValidate(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        planEntity = new PlanEntity();
        planEntity.setSecurity(new PlanSecurity());
    }

    @AfterEach
    void tearDown() {
        subscriptionFormQueryService.reset();
    }

    @Nested
    class PushType {

        @BeforeEach
        void beforeEach() {
            planEntity.setMode(PlanMode.PUSH);
        }

        @Nested
        class NewSubscription {

            @Test
            void should_throw_when_no_entrypointId_defined() {
                NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
                newSubscriptionEntity.setConfiguration(new SubscriptionConfigurationEntity());

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
                    cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)
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

                cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID);

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
    class MTLSType {

        @BeforeEach
        void beforeEach() {
            planEntity.getSecurity().setType(PlanSecurityType.MTLS.name());
        }

        @Nested
        class UpdateSubscription {

            @Test
            void should_throw_when_subscription_ends_before_certificate_expiration() {
                Instant now = Instant.now();
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setEndingAt(Date.from(now.plus(10, ChronoUnit.DAYS)));

                var cert = new io.gravitee.apim.core.application_certificate.model.ClientCertificate(
                    "cert-name",
                    null,
                    Date.from(now),
                    Date.from(now)
                );
                when(
                    clientCertificateCrudService.findByApplicationIdAndStatuses(APP_ID, ClientCertificateStatus.ACTIVE_WITH_END)
                ).thenReturn(List.of(cert));

                assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)).isInstanceOf(
                    SubscriptionEndsAfterClientCertificateException.class
                );
            }

            @Test
            void should_not_throw_when_subscription_ends_after_all_certificate_expirations() {
                Instant now = Instant.now();
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setEndingAt(Date.from(now.plus(30, ChronoUnit.DAYS)));

                var cert = new io.gravitee.apim.core.application_certificate.model.ClientCertificate(
                    "cert-name",
                    null,
                    Date.from(now),
                    Date.from(now.plus(60, ChronoUnit.DAYS))
                );
                when(
                    clientCertificateCrudService.findByApplicationIdAndStatuses(APP_ID, ClientCertificateStatus.ACTIVE_WITH_END)
                ).thenReturn(List.of(cert));

                assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)).doesNotThrowAnyException();
            }

            @Test
            void should_not_throw_when_subscription_ends_before_one_certificate_expirations() {
                Instant now = Instant.now();
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setEndingAt(Date.from(now.plus(30, ChronoUnit.DAYS)));

                var after = new io.gravitee.apim.core.application_certificate.model.ClientCertificate(
                    "cert-name",
                    null,
                    Date.from(now),
                    Date.from(now.plus(60, ChronoUnit.DAYS))
                );
                var before = new io.gravitee.apim.core.application_certificate.model.ClientCertificate(
                    "cert-name",
                    null,
                    Date.from(now),
                    Date.from(now.plus(20, ChronoUnit.DAYS))
                );
                when(
                    clientCertificateCrudService.findByApplicationIdAndStatuses(APP_ID, ClientCertificateStatus.ACTIVE_WITH_END)
                ).thenReturn(List.of(before, after));

                assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)).doesNotThrowAnyException();
            }

            @Test
            void should_not_throw_when_no_ending_date_on_subscription() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setEndingAt(null);

                assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)).doesNotThrowAnyException();
            }

            @Test
            void should_not_throw_when_no_active_certificates() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setEndingAt(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));

                when(
                    clientCertificateCrudService.findByApplicationIdAndStatuses(APP_ID, ClientCertificateStatus.ACTIVE_WITH_END)
                ).thenReturn(List.of());

                assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID)).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    class JWTType {

        @BeforeEach
        void beforeEach() {
            planEntity.getSecurity().setType(PlanSecurityType.JWT.getLabel());
        }

        @Nested
        class NewSubscription {

            @Test
            void should_do_nothing() {
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
            void should_do_nothing() {
                UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                updateSubscriptionEntity.setConfiguration(null);
                planEntity.setSecurity(new PlanSecurity());

                cut.validateAndSanitize(planEntity, updateSubscriptionEntity, APP_ID);

                assertThat(updateSubscriptionEntity.getConfiguration()).isNull();
            }
        }

        @Nested
        class UpdateSubscriptionConfiguration {

            @Test
            void should_do_nothing() {
                UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
                updateSubscriptionConfigurationEntity.setConfiguration(null);
                planEntity.setSecurity(new PlanSecurity());

                cut.validateAndSanitize(planEntity, updateSubscriptionConfigurationEntity);

                assertThat(updateSubscriptionConfigurationEntity.getConfiguration()).isNull();
            }
        }
    }

    @Nested
    class Subscription_form_metadata {

        private static SubscriptionFormFieldConstraints required_email_constraints() {
            return SubscriptionFormConstraintsFactory.fromSchema(
                new SubscriptionFormSchema(List.of(new SubscriptionFormSchema.InputField("email", true, null, null, null, null)))
            );
        }

        @BeforeEach
        void beforeEach() {
            planEntity.setEnvironmentId(SubscriptionFormFixtures.ENVIRONMENT_ID);
        }

        @Test
        void should_throw_when_form_enabled_and_metadata_invalid() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .enabled(true)
                        .validationConstraints(required_email_constraints())
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of());

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscription)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }

        @Test
        void should_not_throw_when_form_enabled_and_metadata_valid() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .enabled(true)
                        .validationConstraints(required_email_constraints())
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of("email", "user@example.com"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_validation_constraints_null() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionForm.builder()
                        .id(SubscriptionFormId.of(SubscriptionFormFixtures.FORM_ID))
                        .environmentId(SubscriptionFormFixtures.ENVIRONMENT_ID)
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .enabled(true)
                        .validationConstraints(null)
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_validation_constraints_empty() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .enabled(true)
                        .validationConstraints(SubscriptionFormFieldConstraints.empty())
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_form_disabled_even_if_constraints_present() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .enabled(false)
                        .validationConstraints(required_email_constraints())
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_no_form_for_environment() {
            // storage is empty — no form registered for any environment

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_treat_null_metadata_as_empty_map_when_validating() {
            subscriptionFormQueryService.initWith(
                List.of(
                    SubscriptionFormFixtures.aSubscriptionFormBuilder()
                        .enabled(true)
                        .validationConstraints(required_email_constraints())
                        .gmdContent(GraviteeMarkdown.of("<p/>"))
                        .build()
                )
            );

            var subscription = new NewSubscriptionEntity();
            subscription.setMetadata(null);

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscription)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }
    }
}
