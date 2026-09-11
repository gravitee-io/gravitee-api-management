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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormIsDefaultException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.use_case.DeleteSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.GetSubscriptionFormUseCase;
import io.gravitee.apim.rest.api.automation.model.SubscriptionFormState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormResourceTest extends AbstractResourceTest {

    private static final String FORM_HRID = "partner-onboarding";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final String FORM_ID = HRIDToUUID.subscriptionForm().context(AUDIT_INFO).hrid(FORM_HRID).id();
    private static final String API_HRID = "weather";
    private static final String API_ID = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();

    @Inject
    private GetSubscriptionFormUseCase getSubscriptionFormUseCase;

    @Inject
    private DeleteSubscriptionFormUseCase deleteSubscriptionFormUseCase;

    @Autowired
    private ApiCrudServiceInMemory apiCrudService;

    @AfterEach
    void tearDown() {
        reset(getSubscriptionFormUseCase, deleteSubscriptionFormUseCase);
        apiCrudService.reset();
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/subscription-forms";
    }

    @Nested
    class Get {

        @Test
        void should_return_the_form() {
            apiCrudService.initWith(
                List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).hrid(API_HRID).environmentId(ENVIRONMENT).build())
            );
            when(getSubscriptionFormUseCase.execute(any())).thenReturn(new GetSubscriptionFormUseCase.Output(aForm(), Map.of()));

            try (var response = rootTarget(FORM_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getSubscriptionFormUseCase).execute(any());

                var state = response.readEntity(SubscriptionFormState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(FORM_HRID);
                    soft.assertThat(state.getId()).isEqualTo(FORM_ID);
                    soft.assertThat(state.getName()).isEqualTo("Partner onboarding");
                    soft.assertThat(state.getEnabled()).isTrue();
                    soft.assertThat(state.getDefault()).isFalse();
                    soft.assertThat(state.getApiHrids()).containsExactly(API_HRID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_give_an_api_it_did_not_create_by_id() {
            // A console-made API keeps a hrid that does not derive into its id: only the id can be applied back.
            apiCrudService.initWith(
                List.of(ApiFixtures.aProxyApiV4().toBuilder().id("console-made").hrid("cross-id").environmentId(ENVIRONMENT).build())
            );
            when(getSubscriptionFormUseCase.execute(any())).thenReturn(
                new GetSubscriptionFormUseCase.Output(aForm(List.of("console-made")), Map.of())
            );

            try (var response = rootTarget(FORM_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.readEntity(SubscriptionFormState.class).getApiHrids()).containsExactly("console-made");
            }
        }

        @Test
        void should_return_404_when_form_is_missing() {
            when(getSubscriptionFormUseCase.execute(any())).thenThrow(new SubscriptionFormNotFoundException("not found", FORM_ID));

            try (var response = rootTarget(FORM_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_the_form() {
            try (var response = rootTarget(FORM_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deleteSubscriptionFormUseCase).execute(any());
            }
        }

        @Test
        void should_return_404_when_form_is_missing() {
            doThrow(new SubscriptionFormNotFoundException("not found", FORM_ID)).when(deleteSubscriptionFormUseCase).execute(any());

            try (var response = rootTarget(FORM_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        @Test
        void should_return_409_when_deleting_the_default_form() {
            doThrow(new SubscriptionFormIsDefaultException(FORM_ID)).when(deleteSubscriptionFormUseCase).execute(any());

            try (var response = rootTarget(FORM_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(409);
            }
        }
    }

    private static SubscriptionForm aForm() {
        return aForm(List.of(API_ID));
    }

    private static SubscriptionForm aForm(List<String> apiIds) {
        return SubscriptionForm.builder()
            .id(SubscriptionFormId.of(FORM_ID))
            .environmentId(ENVIRONMENT)
            .name("Partner onboarding")
            .gmdContent(GraviteeMarkdown.of("<gmd-input name=\"company\" fieldKey=\"company\" required=\"true\"/>"))
            .enabled(true)
            .defaultForm(false)
            .apiIds(apiIds)
            .validationConstraints(SubscriptionFormFieldConstraints.empty())
            .build();
    }
}
