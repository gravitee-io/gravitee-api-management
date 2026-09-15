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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormsToPortalNavigationItemsUpgraderTest {

    private static final Environment DEFAULT_ENVIRONMENT = Environment.builder()
        .id("env-1")
        .hrids(List.of("default"))
        .name("Default")
        .organizationId("org-1")
        .build();

    @Mock
    SubscriptionFormRepository subscriptionFormRepository;

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    @Mock
    PortalPageContentCrudService pageContentCrudService;

    @Mock
    PortalNavigationItemDomainService portalNavigationItemDomainService;

    private SubscriptionFormsToPortalNavigationItemsUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new SubscriptionFormsToPortalNavigationItemsUpgrader(
            subscriptionFormRepository,
            environmentRepository,
            portalNavigationItemsQueryService,
            pageContentCrudService,
            portalNavigationItemDomainService
        );
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_are_no_forms() {
        when(subscriptionFormRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verify(pageContentCrudService, never()).create(any());
        verify(portalNavigationItemDomainService, never()).create(any(), any(), any());
    }

    @Test
    @SneakyThrows
    void should_throw_upgrader_exception_when_something_wrong_happens() {
        when(subscriptionFormRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        Executable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("this is a test exception");
    }

    @Test
    @SneakyThrows
    void should_migrate_a_legacy_form_into_page_content_and_a_subscription_form_navigation_item() {
        var legacyForm = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("# Subscribe\n\n<gmd-input fieldkey=\"email\" />")
            .enabled(true)
            .validationConstraints("{}")
            .build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(legacyForm));
        when(environmentRepository.findById("env-1")).thenReturn(java.util.Optional.of(DEFAULT_ENVIRONMENT));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea("env-1", PortalArea.SUBSCRIPTION_FORM)
        ).thenReturn(List.of());
        var createdContent = new GraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            "org-1",
            "env-1",
            GraviteeMarkdown.of(legacyForm.getGmdContent())
        );
        doReturn(createdContent).when(pageContentCrudService).create(any());

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<GraviteeMarkdownPageContent> contentCaptor = ArgumentCaptor.forClass(GraviteeMarkdownPageContent.class);
        verify(pageContentCrudService).create(contentCaptor.capture());
        assertThat(contentCaptor.getValue().getOrganizationId()).isEqualTo("org-1");
        assertThat(contentCaptor.getValue().getEnvironmentId()).isEqualTo("env-1");
        assertThat(contentCaptor.getValue().getContent().value()).isEqualTo(legacyForm.getGmdContent());

        ArgumentCaptor<CreatePortalNavigationItem> itemCaptor = ArgumentCaptor.forClass(CreatePortalNavigationItem.class);
        verify(portalNavigationItemDomainService).create(eq("org-1"), eq("env-1"), itemCaptor.capture());
        var createdItem = itemCaptor.getValue();
        assertThat(createdItem.getType()).isEqualTo(PortalNavigationItemType.SUBSCRIPTION_FORM);
        assertThat(createdItem.getArea()).isEqualTo(PortalArea.SUBSCRIPTION_FORM);
        assertThat(createdItem.getPortalPageContentId()).isEqualTo(createdContent.getId());
        assertThat(createdItem.getPublished()).isTrue();
        assertThat(createdItem.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(createdItem.getParentId()).isNull();
        assertThat(createdItem.getId()).isNotNull();
    }

    @Test
    @SneakyThrows
    void should_carry_over_the_legacy_enabled_flag_as_published() {
        var disabledForm = SubscriptionForm.builder()
            .id("form-2")
            .environmentId("env-1")
            .gmdContent("# Subscribe")
            .enabled(false)
            .validationConstraints("{}")
            .build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(disabledForm));
        when(environmentRepository.findById("env-1")).thenReturn(java.util.Optional.of(DEFAULT_ENVIRONMENT));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea("env-1", PortalArea.SUBSCRIPTION_FORM)
        ).thenReturn(List.of());
        doReturn(new GraviteeMarkdownPageContent(PortalPageContentId.random(), "org-1", "env-1", GraviteeMarkdown.of("# Subscribe")))
            .when(pageContentCrudService)
            .create(any());

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<CreatePortalNavigationItem> itemCaptor = ArgumentCaptor.forClass(CreatePortalNavigationItem.class);
        verify(portalNavigationItemDomainService).create(any(), any(), itemCaptor.capture());
        assertThat(itemCaptor.getValue().getPublished()).isFalse();
    }

    @Test
    @SneakyThrows
    void should_carry_over_legacy_validation_constraints_unchanged() {
        var formWithConstraints = SubscriptionForm.builder()
            .id("form-3")
            .environmentId("env-1")
            .gmdContent("# Subscribe")
            .enabled(true)
            .validationConstraints("{\"email\":[{\"type\":\"required\"}]}")
            .build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(formWithConstraints));
        when(environmentRepository.findById("env-1")).thenReturn(java.util.Optional.of(DEFAULT_ENVIRONMENT));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea("env-1", PortalArea.SUBSCRIPTION_FORM)
        ).thenReturn(List.of());
        doReturn(new GraviteeMarkdownPageContent(PortalPageContentId.random(), "org-1", "env-1", GraviteeMarkdown.of("# Subscribe")))
            .when(pageContentCrudService)
            .create(any());

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<CreatePortalNavigationItem> itemCaptor = ArgumentCaptor.forClass(CreatePortalNavigationItem.class);
        verify(portalNavigationItemDomainService).create(any(), any(), itemCaptor.capture());
        assertThat(itemCaptor.getValue().getValidationConstraints()).isEqualTo(
            new SubscriptionFormFieldConstraints(Map.of("email", List.of(new Constraint.Required())))
        );
    }

    @Test
    @SneakyThrows
    void should_skip_environment_that_already_has_a_subscription_form_navigation_item() {
        var legacyForm = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("env-1")
            .gmdContent("# Subscribe")
            .enabled(true)
            .validationConstraints("{}")
            .build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(legacyForm));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea("env-1", PortalArea.SUBSCRIPTION_FORM)
        ).thenReturn(
            List.of(
                fixtures.core.model.PortalNavigationItemFixtures.aSubscriptionForm(
                    "00000000-0000-0000-0000-000000000999",
                    PortalPageContentId.random()
                )
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(pageContentCrudService, never()).create(any());
        verify(portalNavigationItemDomainService, never()).create(any(), any(), any());
    }

    @Test
    @SneakyThrows
    void should_skip_a_form_whose_environment_no_longer_exists() {
        var orphanedForm = SubscriptionForm.builder()
            .id("form-1")
            .environmentId("deleted-env")
            .gmdContent("# Subscribe")
            .enabled(true)
            .validationConstraints("{}")
            .build();
        when(subscriptionFormRepository.findAll()).thenReturn(Set.of(orphanedForm));
        when(
            portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea("deleted-env", PortalArea.SUBSCRIPTION_FORM)
        ).thenReturn(List.of());
        when(environmentRepository.findById("deleted-env")).thenReturn(java.util.Optional.empty());

        assertThat(upgrader.upgrade()).isTrue();

        verify(pageContentCrudService, never()).create(any());
        verify(portalNavigationItemDomainService, never()).create(any(), any(), any());
    }
}
