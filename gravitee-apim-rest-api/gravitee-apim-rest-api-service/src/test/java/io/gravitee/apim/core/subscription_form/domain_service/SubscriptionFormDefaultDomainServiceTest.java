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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormDefaultDomainServiceTest {

    private final FailingCrudService crudService = new FailingCrudService();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private SubscriptionFormDefaultDomainService service;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        service = new SubscriptionFormDefaultDomainService(crudService, queryService);
    }

    @Test
    void should_promote_the_form_and_demote_the_previous_default() {
        var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = aNonDefaultForm("Partners");
        crudService.initWith(List.of(previousDefault, partnerForm));
        queryService.initWith(List.of(previousDefault, partnerForm));

        var promoted = service.promote(partnerForm);

        assertThat(promoted.isDefaultForm()).isTrue();
        assertThat(previousDefault.isDefaultForm()).isFalse();
        assertThat(crudService.storage())
            .filteredOn(SubscriptionForm::isDefaultForm)
            .extracting(SubscriptionForm::getId)
            .containsExactly(partnerForm.getId());
    }

    @Test
    void should_demote_the_previous_default_before_promoting_the_form() {
        var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = aNonDefaultForm("Partners");
        crudService.initWith(List.of(previousDefault, partnerForm));
        queryService.initWith(List.of(previousDefault, partnerForm));

        service.promote(partnerForm);

        assertThat(crudService.updates).extracting(SubscriptionForm::getId).containsExactly(previousDefault.getId(), partnerForm.getId());
    }

    @Test
    void should_do_nothing_when_the_form_already_is_the_default() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(defaultForm));
        queryService.initWith(List.of(defaultForm));

        assertThat(service.promote(defaultForm)).isSameAs(defaultForm);
        assertThat(crudService.updates).isEmpty();
    }

    @Test
    void should_promote_the_form_when_the_environment_has_no_default_yet() {
        var partnerForm = aNonDefaultForm("Partners");
        crudService.initWith(List.of(partnerForm));
        queryService.initWith(List.of(partnerForm));

        assertThat(service.promote(partnerForm).isDefaultForm()).isTrue();
    }

    @Test
    void should_hand_the_role_back_to_the_previous_default_when_the_promotion_fails() {
        var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = aNonDefaultForm("Partners");
        crudService.initWith(List.of(previousDefault, partnerForm));
        queryService.initWith(List.of(previousDefault, partnerForm));
        crudService.failOn = form -> form == partnerForm;

        assertThatThrownBy(() -> service.promote(partnerForm))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        assertThat(partnerForm.isDefaultForm()).isFalse();
        assertThat(previousDefault.isDefaultForm()).isTrue();
        assertThat(crudService.storage())
            .filteredOn(SubscriptionForm::isDefaultForm)
            .extracting(SubscriptionForm::getId)
            .containsExactly(previousDefault.getId());
    }

    @Test
    void should_report_a_failed_hand_back_as_suppressed() {
        var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = aNonDefaultForm("Partners");
        crudService.initWith(List.of(previousDefault, partnerForm));
        queryService.initWith(List.of(previousDefault, partnerForm));
        crudService.failOn = form -> form.isDefaultForm();

        assertThatThrownBy(() -> service.promote(partnerForm))
            .isInstanceOf(IllegalStateException.class)
            .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
        assertThat(crudService.storage()).noneMatch(SubscriptionForm::isDefaultForm);
    }

    private static SubscriptionForm aNonDefaultForm(String name) {
        return SubscriptionFormFixtures.aSubscriptionFormBuilder().id(SubscriptionFormId.random()).name(name).defaultForm(false).build();
    }

    /** In-memory CRUD service recording updates and failing on the forms matching {@link #failOn}. */
    private static class FailingCrudService extends SubscriptionFormCrudServiceInMemory {

        final List<SubscriptionForm> updates = new java.util.ArrayList<>();
        Predicate<SubscriptionForm> failOn = form -> false;

        @Override
        public SubscriptionForm update(SubscriptionForm subscriptionForm) {
            if (failOn.test(subscriptionForm)) {
                throw new IllegalStateException("boom");
            }
            updates.add(subscriptionForm);
            return super.update(subscriptionForm);
        }

        @Override
        public void reset() {
            super.reset();
            updates.clear();
            failOn = form -> false;
        }
    }
}
