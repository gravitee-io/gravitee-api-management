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

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RemoveApiFromSubscriptionFormDomainServiceTest {

    private static final String ENVIRONMENT_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final String API_ID = "api-1";

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private RemoveApiFromSubscriptionFormDomainService service;

    @BeforeEach
    void setUp() {
        queryService.reset();
        crudService.reset();
        service = new RemoveApiFromSubscriptionFormDomainService(queryService, crudService);
    }

    @Test
    void should_remove_the_api_from_its_form_and_keep_the_other_apis() {
        var form = aForm(ENVIRONMENT_ID, List.of("api-0", API_ID, "api-2"));
        givenForms(form);

        service.removeApi(ENVIRONMENT_ID, API_ID);

        assertThat(crudService.storage()).singleElement().extracting(SubscriptionForm::getApiIds).isEqualTo(List.of("api-0", "api-2"));
    }

    @Test
    void should_leave_the_forms_untouched_when_the_api_has_no_form() {
        var form = aForm(ENVIRONMENT_ID, List.of("api-0"));
        givenForms(form);

        service.removeApi(ENVIRONMENT_ID, API_ID);

        assertThat(crudService.storage()).singleElement().extracting(SubscriptionForm::getApiIds).isEqualTo(List.of("api-0"));
    }

    @Test
    void should_leave_the_forms_of_another_environment_untouched() {
        var form = aForm("another-environment", List.of(API_ID));
        givenForms(form);

        service.removeApi(ENVIRONMENT_ID, API_ID);

        assertThat(crudService.storage()).singleElement().extracting(SubscriptionForm::getApiIds).isEqualTo(List.of(API_ID));
    }

    private void givenForms(SubscriptionForm... forms) {
        queryService.initWith(List.of(forms));
        crudService.initWith(List.of(forms));
    }

    private static SubscriptionForm aForm(String environmentId, List<String> apiIds) {
        return SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .environmentId(environmentId)
            .apiIds(apiIds)
            .build();
    }
}
