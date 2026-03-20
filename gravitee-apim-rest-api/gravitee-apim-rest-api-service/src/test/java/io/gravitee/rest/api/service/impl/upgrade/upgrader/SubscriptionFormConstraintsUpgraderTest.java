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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormConstraintsUpgraderTest {

    @Mock
    SubscriptionFormRepository subscriptionFormRepository;

    @Mock
    SubscriptionFormSchemaGenerator schemaGenerator;

    SubscriptionFormConstraintsUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new SubscriptionFormConstraintsUpgrader(subscriptionFormRepository, schemaGenerator);
    }

    @Test
    void should_generate_constraints_for_forms_with_empty_sentinel() throws Exception {
        var form1 = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("<gmd-input name=\"email\" fieldKey=\"email\" required />")
            .validationConstraints("{}")
            .build();
        var form2 = SubscriptionForm.builder()
            .id("form-2")
            .environmentId("env-2")
            .gmdContent("<gmd-select name=\"plan\" fieldKey=\"plan\" options=\"basic,pro\" />")
            .validationConstraints("{}")
            .build();

        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(form1, form2));
        when(schemaGenerator.generate(any(GraviteeMarkdown.class))).thenReturn(
            new SubscriptionFormSchema(List.of(new SubscriptionFormSchema.InputField("email", true, null, null, null, null)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<SubscriptionForm> captor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository, times(2)).update(captor.capture());

        captor.getAllValues().forEach(updated -> assertThat(updated.getValidationConstraints()).isNotNull().contains("email"));
    }

    @Test
    void should_process_forms_that_have_empty_json_sentinel() throws Exception {
        var formWithSentinel = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("<gmd-input name=\"email\" fieldKey=\"email\" required />")
            .validationConstraints("{}")
            .build();

        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(formWithSentinel));
        when(schemaGenerator.generate(any(GraviteeMarkdown.class))).thenReturn(
            new SubscriptionFormSchema(List.of(new SubscriptionFormSchema.InputField("email", true, null, null, null, null)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<SubscriptionForm> captor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository, times(1)).update(captor.capture());
        assertThat(captor.getValue().getValidationConstraints()).isNotEqualTo("{}").contains("email");
    }

    @Test
    void should_skip_forms_that_already_have_constraints() throws Exception {
        var formWithConstraints = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("<gmd-input name=\"email\" fieldKey=\"email\" />")
            .validationConstraints("{\"email\":[{\"type\":\"required\"}]}")
            .build();

        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(formWithConstraints));

        assertThat(upgrader.upgrade()).isTrue();

        verify(schemaGenerator, never()).generate(any());
        verify(subscriptionFormRepository, never()).update(any());
    }

    @Test
    void should_update_with_empty_json_when_gmd_yields_no_constraints() throws Exception {
        var formWithNoFields = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("This is plain markdown with no form components.")
            .validationConstraints("{}")
            .build();

        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(formWithNoFields));
        when(schemaGenerator.generate(any(GraviteeMarkdown.class))).thenReturn(new SubscriptionFormSchema(List.of()));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<SubscriptionForm> captor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository, times(1)).update(captor.capture());
        assertThat(captor.getValue().getValidationConstraints()).isEqualTo("{}");
    }

    @Test
    void should_continue_processing_remaining_forms_when_one_fails() throws Exception {
        var failingForm = SubscriptionForm.builder()
            .id("form-fail")
            .environmentId("env-fail")
            .gmdContent("bad content")
            .validationConstraints("{}")
            .build();
        var successForm = SubscriptionForm.builder()
            .id("form-ok")
            .environmentId("env-ok")
            .gmdContent("<gmd-input name=\"x\" fieldKey=\"x\" />")
            .validationConstraints("{}")
            .build();

        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(failingForm, successForm));
        when(schemaGenerator.generate(GraviteeMarkdown.of("bad content"))).thenThrow(new RuntimeException("parse failed"));
        when(schemaGenerator.generate(GraviteeMarkdown.of("<gmd-input name=\"x\" fieldKey=\"x\" />"))).thenReturn(
            new SubscriptionFormSchema(List.of(new SubscriptionFormSchema.InputField("x", false, null, null, null, null)))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(subscriptionFormRepository, times(1)).update(any());
    }

    @Test
    void should_throw_UpgraderException_when_repository_findAll_throws() throws TechnicalException {
        when(subscriptionFormRepository.findAll()).thenThrow(new TechnicalException("db error"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }
}
