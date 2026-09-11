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
package io.gravitee.apim.core.subscription_form.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefaultDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefinitionDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSpecDomainService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdateSubscriptionFormUseCaseTest {

    private static final String ENVIRONMENT_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId(ENVIRONMENT_ID).build();
    private static final String HRID = "partner-onboarding";
    private static final String GMD = "<gmd-input name=\"company\" fieldKey=\"company\" required=\"true\"/>";

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private CreateOrUpdateSubscriptionFormUseCase useCase;
    private ValidateSubscriptionFormUseCase validateUseCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        apiCrudService.reset();
        var definitionDomainService = new SubscriptionFormDefinitionDomainService(
            new GraviteeMarkdownValidator(),
            new SubscriptionFormSchemaGeneratorImpl(),
            queryService,
            apiCrudService
        );
        var specDomainService = new SubscriptionFormSpecDomainService(definitionDomainService, queryService);
        useCase = new CreateOrUpdateSubscriptionFormUseCase(
            crudService,
            specDomainService,
            new SubscriptionFormDefaultDomainService(crudService, queryService)
        );
        validateUseCase = new ValidateSubscriptionFormUseCase(specDomainService);
    }

    private static SubscriptionFormSpecDomainService.Spec aSpec(String name, boolean enabled, boolean defaultForm, List<String> apiIds) {
        return new SubscriptionFormSpecDomainService.Spec(AUDIT_INFO, HRID, name, GMD, enabled, defaultForm, apiIds);
    }

    @Nested
    class Apply {

        @Test
        void should_create_the_form_with_an_id_derived_from_the_hrid() {
            var output = useCase.execute(aSpec("Partners", true, false, null));

            assertThat(output.errors()).isEmpty();
            var created = output.subscriptionForm();
            assertThat(created.getId()).isEqualTo(aSpec("Partners", true, false, null).subscriptionFormId());
            assertThat(created.getName()).isEqualTo("Partners");
            assertThat(created.getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
            assertThat(created.isEnabled()).isTrue();
            assertThat(created.isDefaultForm()).isFalse();
            assertThat(created.getValidationConstraints().byFieldKey()).containsOnlyKeys("company");
            assertThat(crudService.storage()).containsExactly(created);
        }

        @Test
        void should_update_the_same_form_when_the_spec_is_applied_again() {
            var first = useCase.execute(aSpec("Partners", false, false, null)).subscriptionForm();
            queryService.initWith(List.of(first));
            apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").environmentId(ENVIRONMENT_ID).build()));

            var output = useCase.execute(aSpec("Partners v2", true, false, List.of("api-1")));

            assertThat(output.errors()).isEmpty();
            assertThat(output.subscriptionForm().getId()).isEqualTo(first.getId());
            assertThat(output.subscriptionForm().getName()).isEqualTo("Partners v2");
            assertThat(output.subscriptionForm().isEnabled()).isTrue();
            assertThat(output.subscriptionForm().getApiIds()).containsExactly("api-1");
            assertThat(crudService.storage()).hasSize(1);
        }

        @Test
        void should_promote_the_form_and_demote_the_previous_default() {
            var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
            crudService.initWith(List.of(previousDefault));
            queryService.initWith(List.of(previousDefault));

            var output = useCase.execute(aSpec("Partners", true, true, null));

            assertThat(output.subscriptionForm().isDefaultForm()).isTrue();
            assertThat(previousDefault.isDefaultForm()).isFalse();
            assertThat(crudService.storage()).filteredOn(SubscriptionForm::isDefaultForm).hasSize(1);
        }

        @Test
        void should_report_severe_errors_and_persist_nothing() {
            queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().name("Partners").build()));

            var output = useCase.execute(aSpec("partners", true, false, null));

            assertThat(output.subscriptionForm()).isNull();
            assertThat(output.errors())
                .extracting(Validator.Error::getMessage)
                .anyMatch(message -> message.contains("already exists"));
            assertThat(crudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_to_demote_the_current_default() {
            var currentDefault = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(aSpec("Default", true, true, null).subscriptionFormId())
                .build();
            crudService.initWith(List.of(currentDefault));
            queryService.initWith(List.of(currentDefault));

            var output = useCase.execute(aSpec("Default", true, false, null));

            assertThat(output.errors())
                .extracting(Validator.Error::getMessage)
                .anyMatch(message -> message.contains("cannot be demoted"));
            assertThat(currentDefault.isDefaultForm()).isTrue();
        }

        @Test
        void should_reject_a_blank_hrid() {
            var output = useCase.execute(new SubscriptionFormSpecDomainService.Spec(AUDIT_INFO, " ", "Partners", GMD, false, false, null));

            assertThat(output.errors()).extracting(Validator.Error::getMessage).containsExactly("hrid must not be blank");
            assertThat(crudService.storage()).isEmpty();
        }
    }

    @Nested
    class DryRun {

        @Test
        void should_report_the_findings_without_persisting() {
            var output = validateUseCase.execute(aSpec("Partners", true, false, List.of("ghost")));

            assertThat(output.errors())
                .extracting(Validator.Error::getMessage)
                .anyMatch(message -> message.contains("ghost"));
            assertThat(crudService.storage()).isEmpty();
        }

        @Test
        void should_preview_the_form_the_apply_would_write() {
            var existing = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .id(aSpec("Partners", false, false, null).subscriptionFormId())
                .name("Before")
                .enabled(false)
                .defaultForm(false)
                .build();
            crudService.initWith(List.of(existing));
            queryService.initWith(List.of(existing));

            var output = validateUseCase.execute(aSpec("After", true, false, null));

            assertThat(output.errors()).isEmpty();
            assertThat(output.subscriptionForm()).isNotSameAs(existing);
            assertThat(output.subscriptionForm().getName()).isEqualTo("After");
            assertThat(output.subscriptionForm().isEnabled()).isTrue();
            assertThat(existing.getName()).isEqualTo("Before");
            assertThat(crudService.storage()).containsExactly(existing);
        }

        @Test
        void should_return_no_error_for_a_valid_spec() {
            var spec = aSpec("Partners", true, false, null);

            var output = validateUseCase.execute(spec);

            assertThat(output.errors()).isEmpty();
            assertThat(output.subscriptionForm().getId()).isEqualTo(spec.subscriptionFormId());
            assertThat(crudService.storage()).isEmpty();
        }
    }
}
