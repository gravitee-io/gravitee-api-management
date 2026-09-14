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
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormResolutionDomainServiceTest {

    private static final String ENVIRONMENT_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final String API_ID = "api-1";

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private SubscriptionFormResolutionDomainService service;

    @BeforeEach
    void setUp() {
        queryService.reset();
        service = new SubscriptionFormResolutionDomainService(queryService);
    }

    @Test
    void should_prefer_the_enabled_form_dedicated_to_the_api() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().enabled(true).build();
        var dedicated = aDedicatedForm(true);
        queryService.initWith(List.of(defaultForm, dedicated));

        assertThat(service.resolveForApi(ENVIRONMENT_ID, API_ID)).contains(dedicated);
    }

    @Test
    void should_fall_back_to_the_enabled_default_when_the_api_has_no_dedicated_form() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().enabled(true).build();
        queryService.initWith(List.of(defaultForm));

        assertThat(service.resolveForApi(ENVIRONMENT_ID, API_ID)).contains(defaultForm);
    }

    @Test
    void should_return_nothing_when_the_dedicated_form_is_disabled_even_if_the_default_is_enabled() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().enabled(true).build();
        queryService.initWith(List.of(defaultForm, aDedicatedForm(false)));

        assertThat(service.resolveForApi(ENVIRONMENT_ID, API_ID)).isEmpty();
    }

    @Test
    void should_return_nothing_when_the_default_is_disabled() {
        queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().enabled(false).build()));

        assertThat(service.resolveForApi(ENVIRONMENT_ID, API_ID)).isEmpty();
        assertThat(service.resolveDefault(ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void should_return_nothing_when_the_environment_has_no_form() {
        assertThat(service.resolveForApi(ENVIRONMENT_ID, API_ID)).isEmpty();
        assertThat(service.resolveDefault(ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void should_resolve_the_enabled_default_only() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().enabled(true).build();
        queryService.initWith(List.of(defaultForm, aDedicatedForm(true)));

        assertThat(service.resolveDefault(ENVIRONMENT_ID)).contains(defaultForm);
    }

    private static SubscriptionForm aDedicatedForm(boolean enabled) {
        return SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .name("Dedicated")
            .defaultForm(false)
            .enabled(enabled)
            .apiIds(List.of(API_ID))
            .build();
    }
}
