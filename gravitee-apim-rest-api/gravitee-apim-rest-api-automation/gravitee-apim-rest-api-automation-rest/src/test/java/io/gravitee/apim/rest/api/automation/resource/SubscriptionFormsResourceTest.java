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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.use_case.CreateOrUpdateSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.ValidateSubscriptionFormUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.SubscriptionFormState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormsResourceTest extends AbstractResourceTest {

    private static final String FORM_HRID = "partner-onboarding";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final String FORM_ID = HRIDToUUID.subscriptionForm().context(AUDIT_INFO).hrid(FORM_HRID).id();

    @Inject
    private CreateOrUpdateSubscriptionFormUseCase createOrUpdateSubscriptionFormUseCase;

    @Inject
    private ValidateSubscriptionFormUseCase validateSubscriptionFormUseCase;

    @Autowired
    private ApiCrudServiceInMemory apiCrudService;

    @AfterEach
    void tearDown() {
        reset(createOrUpdateSubscriptionFormUseCase, validateSubscriptionFormUseCase);
        apiCrudService.reset();
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/subscription-forms";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_the_spec_as_state_when_validation_passes() {
            when(validateSubscriptionFormUseCase.execute(any())).thenReturn(
                new CreateOrUpdateSubscriptionFormUseCase.Output(null, List.of())
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-form.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdateSubscriptionFormUseCase);
                verify(validateSubscriptionFormUseCase).execute(any());

                var state = response.readEntity(SubscriptionFormState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(FORM_HRID);
                    soft.assertThat(state.getId()).isEqualTo(FORM_ID);
                    soft.assertThat(state.getName()).isEqualTo("Partner onboarding");
                    soft.assertThat(state.getEnabled()).isTrue();
                    soft.assertThat(state.getDefault()).isFalse();
                    soft.assertThat(state.getApiHrids()).containsExactly("weather");
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getErrors()).isNull();
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validateSubscriptionFormUseCase.execute(any())).thenReturn(
                new CreateOrUpdateSubscriptionFormUseCase.Output(
                    null,
                    List.of(Validator.Error.severe("Unknown APIs in this environment: api-1"))
                )
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-form.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(SubscriptionFormState.class);
                assertThat(state.getErrors().getSevere()).containsExactly("Unknown APIs in this environment: api-1");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-form-missing-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validateSubscriptionFormUseCase, createOrUpdateSubscriptionFormUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_the_form() {
            apiCrudService.initWith(
                List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").hrid("weather").environmentId(ENVIRONMENT).build())
            );
            when(createOrUpdateSubscriptionFormUseCase.execute(any())).thenReturn(
                new CreateOrUpdateSubscriptionFormUseCase.Output(aForm(), List.of())
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-form.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdateSubscriptionFormUseCase).execute(any());

                var state = response.readEntity(SubscriptionFormState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo(FORM_ID);
                    soft.assertThat(state.getHrid()).isEqualTo(FORM_HRID);
                    soft.assertThat(state.getName()).isEqualTo("Partner onboarding");
                    soft.assertThat(state.getGmdContent()).contains("gmd-input");
                    soft.assertThat(state.getApiHrids()).containsExactly("weather");
                });
            }
        }

        @Test
        void should_return_400_with_the_errors_when_the_apply_fails() {
            when(createOrUpdateSubscriptionFormUseCase.execute(any())).thenReturn(
                new CreateOrUpdateSubscriptionFormUseCase.Output(
                    null,
                    List.of(Validator.Error.severe("A subscription form named 'Partner onboarding' already exists in this environment."))
                )
            );

            try (
                var response = rootTarget()
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("subscription-form.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                var state = response.readEntity(SubscriptionFormState.class);
                assertThat(state.getErrors().getSevere()).hasSize(1);
            }
        }
    }

    private static SubscriptionForm aForm() {
        return SubscriptionForm.builder()
            .id(SubscriptionFormId.of(FORM_ID))
            .environmentId(ENVIRONMENT)
            .name("Partner onboarding")
            .gmdContent(GraviteeMarkdown.of("<gmd-input name=\"company\" fieldKey=\"company\" required=\"true\"/>"))
            .enabled(true)
            .defaultForm(false)
            .apiIds(List.of("api-1"))
            .validationConstraints(SubscriptionFormFieldConstraints.empty())
            .build();
    }
}
