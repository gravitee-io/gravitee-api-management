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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.Optional;
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
class SubscriptionFormPageContentUpgraderTest {

    private static final String GMD = "<gmd-input name=\"email\" fieldKey=\"email\" required />";

    @Mock
    SubscriptionFormRepository subscriptionFormRepository;

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalPageContentCrudService pageContentCrudService;

    SubscriptionFormPageContentUpgrader upgrader;

    @BeforeEach
    void setUp() throws TechnicalException {
        upgrader = new SubscriptionFormPageContentUpgrader(subscriptionFormRepository, environmentRepository, pageContentCrudService);
        lenient()
            .when(pageContentCrudService.create(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
            .when(subscriptionFormRepository.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_have_the_expected_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.SUBSCRIPTION_FORM_PAGE_CONTENT_UPGRADER);
    }

    @Test
    void should_move_inline_content_to_a_gmd_page_content_and_link_the_row_to_it() throws Exception {
        var form = aLegacyForm("form-1", "env-1");
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(form));
        when(environmentRepository.findById("env-1")).thenReturn(Optional.of(anEnvironment("env-1", "org-1")));

        assertThat(upgrader.upgrade()).isTrue();

        var contentCaptor = ArgumentCaptor.forClass(PortalPageContent.class);
        verify(pageContentCrudService).create(contentCaptor.capture());
        var content = contentCaptor.getValue();
        assertThat(content).isInstanceOf(GraviteeMarkdownPageContent.class);
        assertThat(content.getOrganizationId()).isEqualTo("org-1");
        assertThat(content.getEnvironmentId()).isEqualTo("env-1");
        assertThat(((GraviteeMarkdownPageContent) content).getContent()).isEqualTo(GraviteeMarkdown.of(GMD));

        var formCaptor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository).update(formCaptor.capture());
        var migrated = formCaptor.getValue();
        assertThat(migrated.getId()).isEqualTo("form-1");
        assertThat(migrated.getPortalPageContentId()).isEqualTo(content.getId().toString());
        assertThat(migrated.getGmdContent()).isNull();
        assertThat(migrated.isEnabled()).isTrue();
        assertThat(migrated.getValidationConstraints()).isEqualTo("{\"email\":[{\"type\":\"required\"}]}");
    }

    @Test
    void should_skip_forms_already_linked_to_a_page_content() throws Exception {
        var migrated = aLegacyForm("form-1", "env-1").toBuilder().gmdContent(null).portalPageContentId("content-1").build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(migrated));

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(environmentRepository, pageContentCrudService);
        verify(subscriptionFormRepository, never()).update(any());
    }

    @Test
    void should_skip_forms_without_any_content() throws Exception {
        var empty = aLegacyForm("form-1", "env-1").toBuilder().gmdContent(null).build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(empty));

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(environmentRepository, pageContentCrudService);
        verify(subscriptionFormRepository, never()).update(any());
    }

    @Test
    void should_skip_forms_whose_environment_no_longer_exists() throws Exception {
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(aLegacyForm("form-1", "env-gone")));
        when(environmentRepository.findById("env-gone")).thenReturn(Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(pageContentCrudService);
        verify(subscriptionFormRepository, never()).update(any());
    }

    @Test
    void should_continue_with_remaining_forms_when_one_fails() throws Exception {
        var failing = aLegacyForm("form-fail", "env-fail");
        var ok = aLegacyForm("form-ok", "env-ok");
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(failing, ok));
        when(environmentRepository.findById("env-fail")).thenReturn(Optional.of(anEnvironment("env-fail", "org")));
        when(environmentRepository.findById("env-ok")).thenReturn(Optional.of(anEnvironment("env-ok", "org")));
        when(pageContentCrudService.create(any())).thenAnswer(invocation -> {
            PortalPageContent<?> content = invocation.getArgument(0);
            if ("env-fail".equals(content.getEnvironmentId())) {
                throw new IllegalStateException("boom");
            }
            return content;
        });

        assertThat(upgrader.upgrade()).isTrue();

        var formCaptor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository).update(formCaptor.capture());
        assertThat(formCaptor.getValue().getId()).isEqualTo("form-ok");
        assertThat(failing.getGmdContent()).isEqualTo(GMD);
        assertThat(failing.getPortalPageContentId()).isNull();
    }

    @Test
    void should_wrap_technical_exception_from_repository() throws Exception {
        when(subscriptionFormRepository.findAll()).thenThrow(new TechnicalException("Database error"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    private static SubscriptionForm aLegacyForm(String id, String environmentId) {
        return SubscriptionForm.builder()
            .id(id)
            .environmentId(environmentId)
            .gmdContent(GMD)
            .enabled(true)
            .validationConstraints("{\"email\":[{\"type\":\"required\"}]}")
            .build();
    }

    private static Environment anEnvironment(String id, String organizationId) {
        var environment = new Environment();
        environment.setId(id);
        environment.setOrganizationId(organizationId);
        return environment;
    }
}
