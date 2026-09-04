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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionFormElResolverInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
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
import io.gravitee.rest.api.service.v4.exception.SubscriptionEntrypointIdMissingException;
import io.gravitee.rest.api.service.v4.validation.SubscriptionMetadataSanitizer;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
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
    private static final String ENV_ID = "environment-id";
    private static final String ORG_ID = "org-id";

    private SubscriptionValidationService cut;

    @Mock
    private EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Mock
    private SubscriptionMetadataSanitizer subscriptionMetadataSanitizer;

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    private PlanEntity planEntity;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        cut = new SubscriptionValidationServiceImpl(
            entrypointConnectorPluginService,
            subscriptionMetadataSanitizer,
            navigationItemsQueryService,
            new SubscriptionFormElResolverInMemory()
        );
        lenient()
            .when(subscriptionMetadataSanitizer.sanitizeAndValidate(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        planEntity = new PlanEntity();
        planEntity.setSecurity(new PlanSecurity());
    }

    @AfterEach
    void tearDown() {
        navigationItemsQueryService.reset();
    }

    private static SubscriptionFormFieldConstraints requiredEmailConstraints() {
        return SubscriptionFormConstraintsFactory.fromSchema(
            new SubscriptionFormSchema(List.of(new SubscriptionFormSchema.InputField("email", true, null, null, null, null)))
        );
    }

    private static PortalNavigationSubscriptionForm aForm(boolean published, SubscriptionFormFieldConstraints constraints) {
        return PortalNavigationSubscriptionForm.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .validationConstraints(constraints)
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
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

        @BeforeEach
        void beforeEach() {
            planEntity.setEnvironmentId(ENV_ID);
        }

        @Test
        void should_throw_when_form_enabled_and_metadata_invalid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(Map.of());

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscription)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }

        @Test
        void should_not_throw_when_form_enabled_and_metadata_valid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(Map.of("email", "user@example.com"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_validation_constraints_empty() {
            navigationItemsQueryService.initWith(List.of(aForm(true, SubscriptionFormFieldConstraints.empty())));

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_form_disabled_even_if_constraints_present() {
            navigationItemsQueryService.initWith(List.of(aForm(false, requiredEmailConstraints())));

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_no_form_for_environment() {
            // storage is empty — no form registered for any environment

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
        }

        @Test
        void should_treat_null_metadata_as_empty_map_when_validating() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(true);
            subscription.setMetadata(null);

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscription)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }
    }

    @Nested
    class Subscription_form_metadata_on_update_configuration {

        @BeforeEach
        void beforeEach() {
            planEntity.setEnvironmentId(ENV_ID);
        }

        @Test
        void should_throw_when_form_enabled_and_metadata_invalid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(true);
            subscriptionConfig.setMetadata(Map.of());

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }

        @Test
        void should_not_throw_when_form_enabled_and_metadata_valid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(true);
            subscriptionConfig.setMetadata(Map.of("email", "user@example.com"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_form_disabled() {
            navigationItemsQueryService.initWith(List.of(aForm(false, requiredEmailConstraints())));

            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(true);
            subscriptionConfig.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_no_form_for_environment() {
            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(true);
            subscriptionConfig.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).doesNotThrowAnyException();
        }

        @Test
        void should_treat_null_metadata_as_empty_map_when_validating() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(true);
            subscriptionConfig.setMetadata(null);

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }
    }

    @Nested
    class Subscription_form_metadata_on_update_subscription {

        @BeforeEach
        void beforeEach() {
            planEntity.setEnvironmentId(ENV_ID);
        }

        @Test
        void should_throw_when_form_enabled_and_metadata_invalid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(true);
            updateSubscription.setMetadata(Map.of());

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }

        @Test
        void should_not_throw_when_form_enabled_and_metadata_valid() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(true);
            updateSubscription.setMetadata(Map.of("email", "user@example.com"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_form_disabled() {
            navigationItemsQueryService.initWith(List.of(aForm(false, requiredEmailConstraints())));

            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(true);
            updateSubscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).doesNotThrowAnyException();
        }

        @Test
        void should_not_validate_when_no_form_for_environment() {
            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(true);
            updateSubscription.setMetadata(Map.of());

            assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).doesNotThrowAnyException();
        }

        @Test
        void should_treat_null_metadata_as_empty_map_when_validating() {
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));

            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(true);
            updateSubscription.setMetadata(null);

            assertThatThrownBy(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).isInstanceOf(
                SubscriptionFormValidationException.class
            );
        }
    }

    @Nested
    class When_subscription_form_metadata_validation_disabled {

        @BeforeEach
        void beforeEach() {
            clearInvocations(subscriptionMetadataSanitizer);
            planEntity.setEnvironmentId(ENV_ID);
            navigationItemsQueryService.initWith(List.of(aForm(true, requiredEmailConstraints())));
        }

        @Test
        void should_invoke_metadata_sanitizer_for_new_subscription_without_form_validation() {
            var subscription = new NewSubscriptionEntity();
            subscription.setSubscriptionFormMetadataValidationRequired(false);
            subscription.setMetadata(Map.of("note", "v"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscription)).doesNotThrowAnyException();
            verify(subscriptionMetadataSanitizer, times(1)).sanitizeAndValidate(any());
        }

        @Test
        void should_invoke_metadata_sanitizer_for_update_subscription_without_form_validation() {
            var updateSubscription = new UpdateSubscriptionEntity();
            updateSubscription.setSubscriptionFormMetadataValidationRequired(false);
            updateSubscription.setMetadata(Map.of("note", "v"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, updateSubscription, APP_ID)).doesNotThrowAnyException();
            verify(subscriptionMetadataSanitizer, times(1)).sanitizeAndValidate(any());
        }

        @Test
        void should_invoke_metadata_sanitizer_for_update_configuration_without_form_validation() {
            var subscriptionConfig = new UpdateSubscriptionConfigurationEntity();
            subscriptionConfig.setSubscriptionFormMetadataValidationRequired(false);
            subscriptionConfig.setMetadata(Map.of("note", "v"));

            assertThatCode(() -> cut.validateAndSanitize(planEntity, subscriptionConfig)).doesNotThrowAnyException();
            verify(subscriptionMetadataSanitizer, times(1)).sanitizeAndValidate(any());
        }
    }
}
