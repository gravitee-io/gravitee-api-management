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
package io.gravitee.apim.core.subscription_form.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.SubscriptionFormFixtures;
import fixtures.core.model.SubscriptionFormSchemaFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormSpecDomainServiceTest {

    private static final String ENVIRONMENT_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("org-id").environmentId(ENVIRONMENT_ID).build();
    private static final String HRID = "partner-onboarding";
    private static final String GMD = "<gmd-input name=\"company\" fieldKey=\"company\" required=\"true\"/>";

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    // Mocked rather than the infra implementation: a core test must not depend on an infra slice.
    private final SubscriptionFormSchemaGenerator schemaGenerator = mock(SubscriptionFormSchemaGenerator.class);
    private SubscriptionFormSpecDomainService service;

    @BeforeEach
    void setUp() {
        queryService.reset();
        apiCrudService.reset();
        when(schemaGenerator.generate(any())).thenReturn(
            SubscriptionFormSchemaFixtures.schema(new SubscriptionFormSchema.InputField("company", true, null, null, null, null))
        );
        service = new SubscriptionFormSpecDomainService(
            new SubscriptionFormDefinitionDomainService(new GraviteeMarkdownValidator(), schemaGenerator, queryService, apiCrudService),
            queryService
        );
    }

    @Test
    void should_accept_a_valid_spec_and_return_what_to_persist() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").environmentId(ENVIRONMENT_ID).build()));

        var validation = service.validate(aSpec("Partners", true, false, List.of("api-1")));

        assertThat(validation.errors()).isEmpty();
        assertThat(validation.existing()).isEmpty();
        assertThat(validation.apiIds()).containsExactly("api-1");
        assertThat(validation.definition().constraints().byFieldKey()).containsOnlyKeys("company");
    }

    @Test
    void should_report_an_api_of_another_environment_as_unknown() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").environmentId("other-env").build()));

        var validation = service.validate(aSpec("Partners", true, false, List.of("api-1")));

        assertThat(validation.errors())
            .extracting(Validator.Error::getMessage)
            .singleElement()
            .asString()
            .contains("Unknown APIs in this environment: api-1");
        assertThat(validation.hasSevereErrors()).isTrue();
    }

    @Test
    void should_report_an_api_already_mapped_to_another_form() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").environmentId(ENVIRONMENT_ID).build()));
        queryService.initWith(
            List.of(
                SubscriptionFormFixtures.aSubscriptionFormBuilder()
                    .id(SubscriptionFormId.random())
                    .name("Other")
                    .defaultForm(false)
                    .apiIds(List.of("api-1"))
                    .build()
            )
        );

        var validation = service.validate(aSpec("Partners", true, false, List.of("api-1")));

        assertThat(validation.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(message -> message.contains("already mapped"));
    }

    @Test
    void should_report_a_name_used_by_another_form() {
        queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().name("Partners").build()));

        var validation = service.validate(aSpec("partners", true, false, null));

        assertThat(validation.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(message -> message.contains("already exists"));
    }

    @Test
    void should_refuse_to_demote_the_current_default_even_in_a_dry_run() {
        var currentDefault = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(aSpec("Default", true, true, null).subscriptionFormId())
            .build();
        queryService.initWith(List.of(currentDefault));

        var validation = service.validate(aSpec("Default", true, false, null));

        assertThat(validation.existing()).contains(currentDefault);
        assertThat(validation.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(message -> message.contains("cannot be demoted"));
    }

    @Test
    void should_accept_a_disabled_default_form_like_the_console_toggle_does() {
        queryService.initWith(
            List.of(
                SubscriptionFormFixtures.aSubscriptionFormBuilder().id(aSpec("Default", false, true, null).subscriptionFormId()).build()
            )
        );

        var validation = service.validate(aSpec("Default", false, true, null));

        assertThat(validation.errors()).isEmpty();
    }

    @Test
    void should_reject_a_blank_hrid_before_anything_else() {
        var validation = service.validate(new SubscriptionFormSpecDomainService.Spec(AUDIT_INFO, " ", "Partners", GMD, true, false, null));

        assertThat(validation.errors()).extracting(Validator.Error::getMessage).containsExactly("hrid must not be blank");
        assertThat(validation.definition()).isNull();
    }

    private static SubscriptionFormSpecDomainService.Spec aSpec(String name, boolean enabled, boolean defaultForm, List<String> apiIds) {
        return new SubscriptionFormSpecDomainService.Spec(AUDIT_INFO, HRID, name, GMD, enabled, defaultForm, apiIds);
    }
}
