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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFormFixtures;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormCrudServiceImplTest {

    private static final String ORGANIZATION_ID = "organization-id";

    @Mock
    SubscriptionFormRepository repository;

    @Mock
    EnvironmentCrudService environmentCrudService;

    @Mock
    PortalPageContentCrudService pageContentCrudService;

    @Mock
    PortalPageContentQueryService pageContentQueryService;

    @Captor
    ArgumentCaptor<SubscriptionForm> formCaptor;

    @Captor
    ArgumentCaptor<PortalPageContent<?>> contentCaptor;

    SubscriptionFormCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormCrudServiceImpl(repository, environmentCrudService, pageContentCrudService, pageContentQueryService);
        lenient()
            .when(environmentCrudService.get(SubscriptionFormFixtures.ENVIRONMENT_ID))
            .thenReturn(Environment.builder().id(SubscriptionFormFixtures.ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build());
        lenient()
            .when(pageContentCrudService.create(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
            .when(pageContentCrudService.update(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() throws TechnicalException {
            lenient()
                .when(repository.create(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_create_the_gmd_page_content_in_the_form_environment() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            service.create(subscriptionForm);

            verify(pageContentCrudService).create(contentCaptor.capture());
            var content = contentCaptor.getValue();
            assertThat(content).isInstanceOf(GraviteeMarkdownPageContent.class);
            assertThat(content.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(content.getEnvironmentId()).isEqualTo(SubscriptionFormFixtures.ENVIRONMENT_ID);
            assertThat(((GraviteeMarkdownPageContent) content).getContent()).isEqualTo(
                GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT)
            );
        }

        @Test
        void should_persist_the_row_linked_to_the_page_content_without_inline_content() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            service.create(subscriptionForm);

            verify(pageContentCrudService).create(contentCaptor.capture());
            verify(repository).create(formCaptor.capture());
            var row = formCaptor.getValue();
            assertThat(row.getEnvironmentId()).isEqualTo(SubscriptionFormFixtures.ENVIRONMENT_ID);
            assertThat(row.getPortalPageContentId()).isEqualTo(contentCaptor.getValue().getId().toString());
            assertThat(row.getGmdContent()).isNull();
            assertThat(row.isEnabled()).isFalse();
            assertThat(row.getValidationConstraints()).isEqualTo("{}");
        }

        @Test
        void should_return_the_created_form_with_its_content_and_page_content_id() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            var result = service.create(subscriptionForm);

            verify(pageContentCrudService).create(contentCaptor.capture());
            assertThat(result.getEnvironmentId()).isEqualTo(SubscriptionFormFixtures.ENVIRONMENT_ID);
            assertThat(result.getPortalPageContentId()).isEqualTo(contentCaptor.getValue().getId());
            assertThat(result.getGmdContent()).isEqualTo(GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT));
            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getValidationConstraints()).isEqualTo(subscriptionForm.getValidationConstraints());
        }

        @Test
        void should_generate_id_when_null_and_return_created_form_with_it() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            var result = service.create(subscriptionForm);

            assertThat(result.getId()).isNotNull();
            verify(repository).create(formCaptor.capture());
            assertThat(formCaptor.getValue().getId()).isEqualTo(result.getId().toString());
        }

        @Test
        void should_keep_the_given_id_and_ignore_the_given_page_content_id() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();

            var result = service.create(subscriptionForm);

            verify(pageContentCrudService).create(contentCaptor.capture());
            assertThat(result.getId()).isEqualTo(subscriptionForm.getId());
            assertThat(result.getPortalPageContentId())
                .isEqualTo(contentCaptor.getValue().getId())
                .isNotEqualTo(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID);
        }

        @Test
        void should_delete_the_created_page_content_and_throw_when_the_row_cannot_be_created() throws TechnicalException {
            when(repository.create(any())).thenThrow(TechnicalException.class);
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormWithNullId();

            assertThatThrownBy(() -> service.create(subscriptionForm))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to create a SubscriptionForm for env: " + SubscriptionFormFixtures.ENVIRONMENT_ID
                );

            verify(pageContentCrudService).create(contentCaptor.capture());
            verify(pageContentCrudService).delete(contentCaptor.getValue().getId());
        }
    }

    @Nested
    class Update {

        @BeforeEach
        void setUp() throws TechnicalException {
            lenient()
                .when(repository.update(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void should_update_the_referenced_page_content_with_the_new_definition() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .gmdContent(GraviteeMarkdown.of("<gmd-input name=\"updated\" fieldKey=\"updated\"/>"))
                .build();
            var existingContent = aPageContent(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID);
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(
                Optional.of(existingContent)
            );

            service.update(subscriptionForm);

            verify(pageContentCrudService).update(contentCaptor.capture());
            assertThat(contentCaptor.getValue()).isSameAs(existingContent);
            assertThat(existingContent.getContent()).isEqualTo(GraviteeMarkdown.of("<gmd-input name=\"updated\" fieldKey=\"updated\"/>"));
            verify(pageContentCrudService, never()).create(any());
        }

        @Test
        void should_persist_the_row_linked_to_the_page_content_without_inline_content() throws TechnicalException {
            var subscriptionForm = SubscriptionFormFixtures.anEnabledSubscriptionForm();
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(
                Optional.of(aPageContent(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID))
            );

            service.update(subscriptionForm);

            verify(repository).update(formCaptor.capture());
            var row = formCaptor.getValue();
            assertThat(row.getId()).isEqualTo(SubscriptionFormFixtures.FORM_ID);
            assertThat(row.getPortalPageContentId()).isEqualTo(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID.toString());
            assertThat(row.getGmdContent()).isNull();
            assertThat(row.isEnabled()).isTrue();
        }

        @Test
        void should_return_the_updated_form_with_its_content() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(
                Optional.of(aPageContent(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID))
            );

            var result = service.update(subscriptionForm);

            assertThat(result).usingRecursiveComparison().isEqualTo(subscriptionForm);
        }

        @Test
        void should_move_inline_content_to_a_new_page_content_when_the_form_was_not_migrated_yet() throws TechnicalException {
            var legacyForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().portalPageContentId(null).build();

            var result = service.update(legacyForm);

            verify(pageContentCrudService).create(contentCaptor.capture());
            var content = contentCaptor.getValue();
            assertThat(((GraviteeMarkdownPageContent) content).getContent()).isEqualTo(legacyForm.getGmdContent());
            verify(repository).update(formCaptor.capture());
            assertThat(formCaptor.getValue().getPortalPageContentId()).isEqualTo(content.getId().toString());
            assertThat(formCaptor.getValue().getGmdContent()).isNull();
            assertThat(result.getPortalPageContentId()).isEqualTo(content.getId());
            verify(pageContentQueryService, never()).findById(any());
        }

        @Test
        void should_delete_the_new_page_content_and_throw_when_the_legacy_row_cannot_be_updated() throws TechnicalException {
            when(repository.update(any())).thenThrow(TechnicalException.class);
            var legacyForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().portalPageContentId(null).build();

            assertThatThrownBy(() -> service.update(legacyForm)).isInstanceOf(TechnicalDomainException.class);

            verify(pageContentCrudService).create(contentCaptor.capture());
            verify(pageContentCrudService).delete(contentCaptor.getValue().getId());
        }

        @Test
        void should_throw_when_the_referenced_page_content_is_missing() {
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionForm();
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(subscriptionForm))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "SubscriptionForm " +
                        SubscriptionFormFixtures.FORM_ID +
                        " references a missing page content: " +
                        SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID
                );
            verify(pageContentCrudService, never()).update(any());
        }

        @Test
        void should_restore_the_previous_definition_and_throw_when_the_row_cannot_be_updated() throws TechnicalException {
            var existingContent = aPageContent(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID);
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(
                Optional.of(existingContent)
            );
            when(repository.update(any())).thenThrow(TechnicalException.class);
            var subscriptionForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
                .gmdContent(GraviteeMarkdown.of("<gmd-input name=\"updated\" fieldKey=\"updated\"/>"))
                .build();

            assertThatThrownBy(() -> service.update(subscriptionForm))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update a SubscriptionForm with id: " + SubscriptionFormFixtures.FORM_ID);

            verify(pageContentCrudService, times(2)).update(existingContent);
            assertThat(existingContent.getContent()).isEqualTo(GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT));
            verify(pageContentCrudService, never()).delete(any());
        }

        @Test
        void should_report_a_failed_restore_as_suppressed_and_still_throw_the_row_failure() throws TechnicalException {
            var existingContent = aPageContent(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID);
            when(pageContentQueryService.findById(SubscriptionFormFixtures.PORTAL_PAGE_CONTENT_ID)).thenReturn(
                Optional.of(existingContent)
            );
            when(repository.update(any())).thenThrow(new TechnicalException("Database error"));
            doReturn(existingContent).doThrow(new IllegalStateException("restore failed")).when(pageContentCrudService).update(any());

            assertThatThrownBy(() -> service.update(SubscriptionFormFixtures.aSubscriptionForm()))
                .isInstanceOf(TechnicalDomainException.class)
                .satisfies(e ->
                    assertThat(e.getCause().getSuppressed()).extracting(Throwable::getMessage).containsExactly("restore failed")
                );
        }
    }

    private static GraviteeMarkdownPageContent aPageContent(PortalPageContentId id) {
        return new GraviteeMarkdownPageContent(
            id,
            ORGANIZATION_ID,
            SubscriptionFormFixtures.ENVIRONMENT_ID,
            GraviteeMarkdown.of(SubscriptionFormFixtures.GMD_CONTENT)
        );
    }
}
