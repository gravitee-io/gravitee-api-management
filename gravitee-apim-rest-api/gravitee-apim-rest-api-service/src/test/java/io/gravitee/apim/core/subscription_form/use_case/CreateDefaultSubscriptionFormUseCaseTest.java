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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDefaultSubscriptionFormUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Environment ENVIRONMENT = Environment.builder().id(ENV_ID).organizationId("org-id").build();

    EnvironmentCrudServiceInMemory environmentCrudService;
    List<PortalNavigationItem> navigationItemStorage;
    PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    PortalNavigationItemsCrudServiceInMemory navigationItemCrudService;
    PortalPageContentCrudServiceInMemory pageContentCrudService;
    SubscriptionFormSchemaGeneratorImpl schemaGenerator;

    CreateDefaultSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        environmentCrudService = new EnvironmentCrudServiceInMemory();
        navigationItemStorage = new ArrayList<>();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(navigationItemStorage);
        navigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory(navigationItemStorage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();
        useCase = new CreateDefaultSubscriptionFormUseCase(
            environmentCrudService,
            navigationItemsQueryService,
            navigationItemCrudService,
            pageContentCrudService,
            schemaGenerator
        );
    }

    @Test
    void should_create_default_subscription_form_when_missing() throws Exception {
        environmentCrudService.initWith(List.of(ENVIRONMENT));
        var defaultContent = new ClassPathResource("templates/default-subscription-form.md").getContentAsString(StandardCharsets.UTF_8);
        var expectedConstraints = SubscriptionFormConstraintsFactory.fromSchema(
            schemaGenerator.generate(GraviteeMarkdown.of(defaultContent))
        );

        useCase.execute(ENV_ID);

        assertThat(pageContentCrudService.storage()).hasSize(1);
        assertThat(pageContentCrudService.storage().getFirst().getOrganizationId()).isEqualTo("org-id");
        assertThat(pageContentCrudService.storage().getFirst().getEnvironmentId()).isEqualTo(ENV_ID);

        assertThat(navigationItemStorage).hasSize(1);
        var navItem = (PortalNavigationSubscriptionForm) navigationItemStorage.getFirst();
        assertThat(navItem.getArea()).isEqualTo(PortalArea.SUBSCRIPTION_FORM);
        assertThat(navItem.getPublished()).isFalse();
        assertThat(navItem.getParentId()).isNull();
        assertThat(navItem.getValidationConstraints().isEmpty()).isFalse();
        assertThat(navItem.getValidationConstraints()).isEqualTo(expectedConstraints);
    }

    @Test
    void should_do_nothing_when_a_subscription_form_already_exists() {
        navigationItemStorage.add(existingSubscriptionForm());

        useCase.execute(ENV_ID);

        assertThat(pageContentCrudService.storage()).isEmpty();
        assertThat(navigationItemStorage).hasSize(1);
    }

    @Test
    void should_throw_when_environment_is_unknown() {
        assertThatThrownBy(() -> useCase.execute(ENV_ID)).isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void should_load_template_regardless_of_thread_context_classloader() {
        // Given: a context classloader that cannot see this module's classpath resources at all,
        // reproducing the production failure mode (a Vert.x/plugin-owned thread whose context
        // classloader isn't the one that loaded gravitee-apim-rest-api-service).
        environmentCrudService.initWith(List.of(ENVIRONMENT));
        final var originalClassLoader = Thread.currentThread().getContextClassLoader();
        final var isolatedClassLoader = new URLClassLoader(new URL[0], null);
        Thread.currentThread().setContextClassLoader(isolatedClassLoader);

        try {
            // When
            useCase.execute(ENV_ID);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // Then
        assertThat(navigationItemStorage).hasSize(1);
    }

    private PortalNavigationSubscriptionForm existingSubscriptionForm() {
        return PortalNavigationSubscriptionForm.builder()
            .id(PortalNavigationItemId.random())
            .organizationId("org-id")
            .environmentId(ENV_ID)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .validationConstraints(SubscriptionFormFieldConstraints.empty())
            .published(false)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
