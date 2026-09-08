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
package io.gravitee.apim.infra.query_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.async_api.AsyncApi;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
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
class SubscriptionFormQueryServiceImplTest {

    private static final String FORM_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final PortalPageContentId CONTENT_ID = PortalPageContentId.of("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String GMD = "<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>";

    @Mock
    SubscriptionFormRepository repository;

    @Mock
    PortalPageContentQueryService pageContentQueryService;

    SubscriptionFormQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormQueryServiceImpl(repository, pageContentQueryService);
    }

    @Nested
    class FindDefaultForEnvironmentId {

        @Test
        void should_return_the_form_with_its_definition_loaded_from_the_page_content() throws TechnicalException {
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(aMigratedRow()));
            when(pageContentQueryService.findById(CONTENT_ID)).thenReturn(
                Optional.of(new GraviteeMarkdownPageContent(CONTENT_ID, "organization-id", ENVIRONMENT_ID, GraviteeMarkdown.of(GMD)))
            );

            var result = service.findDefaultForEnvironmentId(ENVIRONMENT_ID);

            assertThat(result).isPresent();
            var form = result.get();
            assertThat(form.getId()).hasToString(FORM_ID);
            assertThat(form.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(form.getPortalPageContentId()).isEqualTo(CONTENT_ID);
            assertThat(form.getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
            assertThat(form.isEnabled()).isTrue();
            assertThat(form.getValidationConstraints().byFieldKey()).containsOnlyKeys("company");
        }

        @Test
        void should_serve_a_legacy_row_from_its_inline_content_when_not_migrated_yet() throws TechnicalException {
            var legacyRow = aMigratedRow().toBuilder().portalPageContentId(null).gmdContent(GMD).build();
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(legacyRow));

            var result = service.findDefaultForEnvironmentId(ENVIRONMENT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getPortalPageContentId()).isNull();
            assertThat(result.get().getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
            verifyNoInteractions(pageContentQueryService);
        }

        @Test
        void should_throw_when_the_row_has_neither_inline_content_nor_page_content() throws TechnicalException {
            var brokenRow = aMigratedRow().toBuilder().portalPageContentId(null).gmdContent(null).build();
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(brokenRow));

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("SubscriptionForm " + FORM_ID + " has neither inline content nor a page content");
        }

        @Test
        void should_return_empty_when_form_not_found() throws TechnicalException {
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.empty());

            var result = service.findDefaultForEnvironmentId(ENVIRONMENT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_the_referenced_page_content_is_missing() throws TechnicalException {
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(aMigratedRow()));
            when(pageContentQueryService.findById(CONTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("SubscriptionForm " + FORM_ID + " references a missing page content: " + CONTENT_ID);
        }

        @Test
        void should_throw_when_the_referenced_page_content_is_not_gravitee_markdown() throws TechnicalException {
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(aMigratedRow()));
            when(pageContentQueryService.findById(CONTENT_ID)).thenReturn(
                Optional.of(new AsyncApiPageContent(CONTENT_ID, "organization-id", ENVIRONMENT_ID, AsyncApi.of("asyncapi: 3.0.0"), null))
            );

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessageContaining("of type ASYNCAPI instead of GRAVITEE_MARKDOWN");
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findByEnvironmentId(ENVIRONMENT_ID)).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find a SubscriptionForm for environment: environment-id")
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        void should_return_the_form_with_its_definition_loaded_from_the_page_content() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(FORM_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(aMigratedRow()));
            when(pageContentQueryService.findById(CONTENT_ID)).thenReturn(
                Optional.of(new GraviteeMarkdownPageContent(CONTENT_ID, "organization-id", ENVIRONMENT_ID, GraviteeMarkdown.of(GMD)))
            );

            var result = service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID));

            assertThat(result).isPresent();
            assertThat(result.get().getId()).hasToString(FORM_ID);
            assertThat(result.get().getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
            verify(pageContentQueryService).findById(CONTENT_ID);
        }

        @Test
        void should_return_empty_when_form_not_found() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(FORM_ID, ENVIRONMENT_ID)).thenReturn(Optional.empty());

            var result = service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID));

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to find a SubscriptionForm with id: " + FORM_ID + " in environment: " + ENVIRONMENT_ID
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    private static SubscriptionForm aMigratedRow() {
        return SubscriptionForm.builder()
            .id(FORM_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID.toString())
            .gmdContent(null)
            .enabled(true)
            .validationConstraints("{\"company\":[{\"type\":\"required\"}]}")
            .build();
    }
}
