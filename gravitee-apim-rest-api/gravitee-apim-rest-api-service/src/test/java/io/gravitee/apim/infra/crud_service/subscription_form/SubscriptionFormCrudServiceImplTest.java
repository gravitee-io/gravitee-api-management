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
package io.gravitee.apim.infra.crud_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.subscription_form.model.Constraint;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormCrudServiceImplTest {

    private static final String ENV_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final Environment ENVIRONMENT = Environment.builder().id(ENV_ID).organizationId("org-id").build();

    @Mock
    EnvironmentRepository environmentRepository;

    List<io.gravitee.apim.core.portal_page.model.PortalNavigationItem> navigationItemStorage;
    PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    PortalNavigationItemsCrudServiceInMemory navigationItemCrudService;
    PortalPageContentCrudServiceInMemory pageContentCrudService;
    PortalPageContentQueryServiceInMemory pageContentQueryService;

    SubscriptionFormCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        navigationItemStorage = new ArrayList<>();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(navigationItemStorage);
        navigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory(navigationItemStorage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage());
        service = new SubscriptionFormCrudServiceImpl(
            environmentRepository,
            navigationItemsQueryService,
            navigationItemCrudService,
            pageContentQueryService,
            pageContentCrudService
        );
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() throws io.gravitee.repository.exceptions.TechnicalException {
            when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.of(ENVIRONMENT));
        }

        @Test
        void should_create_a_page_content_and_a_subscription_form_navigation_item() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            var result = service.create(subscriptionForm);

            assertThat(pageContentCrudService.storage()).hasSize(1);
            assertThat(pageContentCrudService.storage().getFirst().getOrganizationId()).isEqualTo("org-id");
            assertThat(pageContentCrudService.storage().getFirst().getEnvironmentId()).isEqualTo(ENV_ID);

            assertThat(navigationItemStorage).hasSize(1);
            var navItem = (PortalNavigationSubscriptionForm) navigationItemStorage.getFirst();
            assertThat(navItem.getArea()).isEqualTo(PortalArea.SUBSCRIPTION_FORM);
            assertThat(navItem.getPublished()).isFalse();
            assertThat(navItem.getParentId()).isNull();

            assertThat(result.getEnvironmentId()).isEqualTo(ENV_ID);
            assertThat(result.getGmdContent()).isEqualTo(subscriptionForm.getGmdContent());
            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getId()).isNotNull();
        }

        @Test
        void should_throw_when_environment_is_unknown() throws io.gravitee.repository.exceptions.TechnicalException {
            when(environmentRepository.findById(ENV_ID)).thenReturn(Optional.empty());
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            assertThatThrownBy(() -> service.create(subscriptionForm)).isInstanceOf(TechnicalDomainException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_published_and_validation_constraints_and_page_content() {
            var contentId = PortalPageContentId.random();
            pageContentCrudService
                .storage()
                .add(
                    fixtures.core.model.PortalPageContentFixtures.aGraviteeMarkdownPageContent(contentId, "org-id", ENV_ID, "old content")
                );
            var existingItem = PortalNavigationItemFixtures.aSubscriptionForm(SubscriptionFormFixtures.FORM_ID, contentId)
                .toBuilder()
                .environmentId(ENV_ID)
                .published(false)
                .build();
            navigationItemStorage.add(existingItem);

            var newConstraints = new SubscriptionFormFieldConstraints(Map.of("email", List.of(new Constraint.Required())));
            var updatedForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .gmdContent(GraviteeMarkdown.of("new content"))
                .enabled(true)
                .validationConstraints(newConstraints)
                .build();

            var result = service.update(updatedForm);

            var storedItem = (PortalNavigationSubscriptionForm) navigationItemsQueryService.findByIdAndEnvironmentId(
                ENV_ID,
                existingItem.getId()
            );
            assertThat(storedItem.getPublished()).isTrue();
            assertThat(storedItem.getValidationConstraints()).isEqualTo(newConstraints);

            var storedContent = pageContentQueryService.findById(contentId).orElseThrow();
            assertThat(((io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent) storedContent).getContent()).isEqualTo(
                GraviteeMarkdown.of("new content")
            );

            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getValidationConstraints()).isEqualTo(newConstraints);
        }

        @Test
        void should_throw_when_the_navigation_item_does_not_exist() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            assertThatThrownBy(() -> service.update(subscriptionForm)).isInstanceOf(TechnicalDomainException.class);
        }
    }
}
