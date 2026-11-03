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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PageActionException;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class PageService_ParentOwnershipTest {

    private static final String API_ID = "api";
    private static final ExecutionContext CONTEXT = new ExecutionContext("organization", "environment");

    @InjectMocks
    private PageServiceImpl pageService;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanSearchService planSearchService;

    @BeforeEach
    void set_up() throws Exception {
        when(pageRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pageRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    static Stream<Arguments> foreign_owners() {
        return Stream.of(
            Arguments.of(PageReferenceType.API, "other-api"),
            Arguments.of(PageReferenceType.ENVIRONMENT, "environment"),
            Arguments.of(PageReferenceType.API, null),
            Arguments.of(null, API_ID),
            Arguments.of(PageReferenceType.API, "")
        );
    }

    @ParameterizedTest
    @MethodSource("foreign_owners")
    void should_reject_foreign_parent_before_creating_page(PageReferenceType referenceType, String referenceId) throws Exception {
        var parent = page("parent", PageType.FOLDER);
        parent.setReferenceType(referenceType);
        parent.setReferenceId(referenceId);
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> pageService.createPage(CONTEXT, API_ID, newPage(PageType.MARKDOWN, "parent"))).isInstanceOf(
            PageNotFoundException.class
        );

        verify(pageRepository, never()).create(any());
        verify(pageRepository, never()).update(any());
        verifyNoInteractions(pageRevisionService, auditService, searchEngineService);
    }

    @Test
    void should_reject_missing_parent() throws Exception {
        assertThatThrownBy(() -> pageService.createPage(CONTEXT, API_ID, newPage(PageType.MARKDOWN, "missing"))).isInstanceOf(
            PageNotFoundException.class
        );
        verify(pageRepository, never()).create(any());
    }

    @Test
    void should_validate_translation_owner_before_parent_type() throws Exception {
        var parent = page("parent", PageType.ROOT);
        parent.setReferenceId("other-api");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> pageService.createPage(CONTEXT, API_ID, newPage(PageType.TRANSLATION, "parent"))).isInstanceOf(
            PageNotFoundException.class
        );
        verifyNoInteractions(pageRevisionService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_reject_foreign_parent_before_updating_page(boolean partial) throws Exception {
        var child = page("child", PageType.MARKDOWN);
        var parent = page("parent", PageType.FOLDER);
        parent.setReferenceId("other-api");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        var update = new UpdatePageEntity();
        update.setParentId("parent");
        update.setOrder(3);
        update.setHomepage(true);

        assertThatThrownBy(() -> pageService.update(CONTEXT, "child", update, partial)).isInstanceOf(PageNotFoundException.class);

        verify(pageRepository, never()).update(any());
        verifyNoInteractions(pageRevisionService, auditService, searchEngineService);
    }

    @Test
    void should_validate_unchanged_parent_on_patch() throws Exception {
        var child = page("child", PageType.TRANSLATION);
        child.setParentId("parent");
        var parent = page("parent", PageType.MARKDOWN);
        parent.setReferenceId("other-api");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        var update = new UpdatePageEntity();
        update.setName("updated translation");
        update.setOrder(0);

        assertThatThrownBy(() -> pageService.update(CONTEXT, "child", update, true)).isInstanceOf(PageNotFoundException.class);
        verify(pageRepository, never()).update(any());
        verifyNoInteractions(pageRevisionService);
    }

    @Test
    void should_keep_parent_when_omitted_from_patch() throws Exception {
        var child = page("child", PageType.MARKDOWN);
        child.setParentId("parent");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(page("parent", PageType.FOLDER)));

        var result = pageService.update(CONTEXT, "child", new UpdatePageEntity(), true);

        assertThat(result.getParentId()).isEqualTo("parent");
    }

    @Test
    void should_clear_parent_when_empty_in_patch() throws Exception {
        var child = page("child", PageType.MARKDOWN);
        child.setParentId("foreign-parent");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        var update = new UpdatePageEntity();
        update.setParentId("");

        assertThat(pageService.update(CONTEXT, "child", update, true).getParentId()).isNull();
    }

    @Test
    void should_reject_translation_without_effective_parent_on_put() throws Exception {
        var child = page("child", PageType.TRANSLATION);
        child.setParentId("parent");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(page("parent", PageType.MARKDOWN)));

        var update = new UpdatePageEntity();
        update.setOrder(0);
        assertThatThrownBy(() -> pageService.update(CONTEXT, "child", update, false)).isInstanceOf(PageActionException.class);
        verify(pageRepository, never()).update(any());
    }

    @Test
    void should_not_include_foreign_ancestor_names_in_parent_path() throws Exception {
        var child = page("child", PageType.MARKDOWN);
        child.setParentId("parent");
        var parent = page("parent", PageType.FOLDER);
        parent.setReferenceId("other-api");
        parent.setName("private name");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThat(pageService.findById("child").getParentPath()).isEmpty();
    }

    @Test
    void should_stop_parent_path_at_foreign_grandparent() throws Exception {
        var child = page("child", PageType.MARKDOWN);
        child.setParentId("parent");
        var parent = page("parent", PageType.FOLDER);
        parent.setParentId("grandparent");
        var grandparent = page("grandparent", PageType.FOLDER);
        grandparent.setReferenceId("other-api");
        grandparent.setName("private name");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(pageRepository.findById("grandparent")).thenReturn(Optional.of(grandparent));

        assertThat(pageService.findById("child").getParentPath()).isEqualTo("/parent");
    }

    @Test
    void should_reject_parent_from_another_environment() throws Exception {
        var parent = page("parent", PageType.FOLDER);
        parent.setReferenceType(PageReferenceType.ENVIRONMENT);
        parent.setReferenceId("other-environment");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> pageService.createPage(CONTEXT, newPage(PageType.MARKDOWN, "parent"))).isInstanceOf(
            PageNotFoundException.class
        );
        verify(pageRepository, never()).create(any());
    }

    @Test
    void should_accept_parent_in_same_environment() throws Exception {
        var parent = page("parent", PageType.FOLDER);
        parent.setReferenceType(PageReferenceType.ENVIRONMENT);
        parent.setReferenceId("environment");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThat(pageService.createPage(CONTEXT, newPage(PageType.MARKDOWN, "parent")).getParentId()).isEqualTo("parent");
    }

    @Test
    void should_ignore_foreign_translation_in_localized_read() throws Exception {
        var child = page("child", PageType.MARKDOWN);
        var translation = page("translation", PageType.TRANSLATION);
        translation.setParentId("child");
        translation.setReferenceId("other-api");
        translation.setContent("foreign content");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.search(any())).thenReturn(new ArrayList<>(List.of(translation)));

        var result = pageService.findById("child", "fr");

        assertThat(result.getName()).isEqualTo("child");
        assertThat(result.getContent()).isEqualTo("content");
    }

    @Test
    void should_exclude_foreign_translations_from_metadata() throws Exception {
        var translation = page("translation", PageType.TRANSLATION);
        translation.setParentId("child");
        translation.setReferenceId("other-api");
        when(pageRepository.findById("child")).thenReturn(Optional.of(page("child", PageType.MARKDOWN)));
        when(pageRepository.search(any())).thenReturn(new ArrayList<>(List.of(translation)));

        assertThat(pageService.findById("child").getTranslations()).isNullOrEmpty();
    }

    @Test
    void should_scope_inherited_link_translation_to_target_owner() throws Exception {
        var link = page("link", PageType.LINK);
        link.setReferenceType(PageReferenceType.ENVIRONMENT);
        link.setReferenceId("environment");
        link.setContent("target");
        link.setConfiguration(Map.of(PageConfigurationKeys.LINK_INHERIT, "true"));
        var target = page("target", PageType.MARKDOWN);
        var translation = page("translation", PageType.TRANSLATION);
        translation.setParentId("target");
        translation.setName("target translation");
        when(pageRepository.findById("link")).thenReturn(Optional.of(link));
        when(pageRepository.findById("target")).thenReturn(Optional.of(target));
        when(pageRepository.search(any())).thenReturn(new ArrayList<>(List.of(translation)));

        assertThat(pageService.findById("link", "fr").getName()).isEqualTo("target translation");
    }

    @Test
    void should_not_delete_foreign_translations_with_parent() throws Exception {
        var parent = page("parent", PageType.MARKDOWN);
        var own = page("own", PageType.TRANSLATION);
        own.setParentId("parent");
        var foreign = page("foreign", PageType.TRANSLATION);
        foreign.setParentId("parent");
        foreign.setReferenceId("other-api");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(pageRepository.search(argThat(criteria -> PageType.TRANSLATION.name().equals(criteria.getType())))).thenReturn(
            List.of(own, foreign)
        );

        pageService.delete(CONTEXT, "parent");

        verify(pageRepository).delete("parent");
        verify(pageRepository).delete("own");
        verify(pageRepository, never()).delete("foreign");
    }

    @Test
    void should_not_publish_foreign_translations_with_parent() throws Exception {
        var parent = page("parent", PageType.MARKDOWN);
        var own = page("own", PageType.TRANSLATION);
        own.setParentId("parent");
        var foreign = page("foreign", PageType.TRANSLATION);
        foreign.setParentId("parent");
        foreign.setReferenceId("other-api");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(pageRepository.search(argThat(criteria -> PageType.TRANSLATION.name().equals(criteria.getType())))).thenReturn(
            List.of(own, foreign)
        );
        var update = new UpdatePageEntity();
        update.setPublished(true);

        pageService.update(CONTEXT, "parent", update, true);

        verify(pageRepository).update(own);
        verify(pageRepository, never()).update(foreign);
        assertThat(foreign.isPublished()).isFalse();
    }

    @Test
    void should_validate_parent_before_fetching_existing_page() throws Exception {
        var child = page("child", PageType.TRANSLATION);
        child.setParentId("parent");
        var source = new PageSource();
        source.setConfiguration("{}");
        child.setSource(source);
        var parent = page("parent", PageType.MARKDOWN);
        parent.setReferenceId("other-api");
        when(pageRepository.findById("child")).thenReturn(Optional.of(child));
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> pageService.fetch(CONTEXT, "child", "contributor")).isInstanceOf(PageNotFoundException.class);
        verify(pageRepository, never()).update(any());
        verify(pageRepository, never()).delete(any());
    }

    @Test
    void should_reject_foreign_parent_when_resolving_translation_content_type() throws Exception {
        var parent = page("parent", PageType.MARKDOWN);
        parent.setReferenceId("other-api");
        when(pageRepository.findById("parent")).thenReturn(Optional.of(parent));
        var translation = PageEntity.builder()
            .id("translation")
            .type(PageType.TRANSLATION.name())
            .referenceType(PageReferenceType.API.name())
            .referenceId(API_ID)
            .parentId("parent")
            .content("translation content")
            .build();

        assertThatThrownBy(() -> pageService.validateSafeContent(CONTEXT, translation, API_ID)).isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void should_reject_missing_parent_when_transforming_translation() {
        var translation = PageEntity.builder()
            .id("translation")
            .type(PageType.TRANSLATION.name())
            .referenceType(PageReferenceType.API.name())
            .referenceId(API_ID)
            .parentId("missing")
            .content("translation content")
            .build();

        assertThatThrownBy(() -> pageService.transformSwagger(CONTEXT, translation)).isInstanceOf(PageNotFoundException.class);
    }

    @Test
    void should_reject_parentless_translation_content_validation() {
        var translation = PageEntity.builder().id("translation").type(PageType.TRANSLATION.name()).content("translation content").build();

        assertThatThrownBy(() -> pageService.validateSafeContent(CONTEXT, translation, API_ID)).isInstanceOf(PageActionException.class);
    }

    @Test
    void should_fail_content_validation_when_parent_lookup_fails() throws Exception {
        var failure = new TechnicalException("parent lookup failed");
        when(pageRepository.findById("parent")).thenThrow(failure);
        var translation = PageEntity.builder()
            .id("translation")
            .type(PageType.TRANSLATION.name())
            .referenceType(PageReferenceType.API.name())
            .referenceId(API_ID)
            .parentId("parent")
            .content("translation content")
            .build();

        assertThatThrownBy(() -> pageService.validateSafeContent(CONTEXT, translation, API_ID))
            .isInstanceOf(TechnicalManagementException.class)
            .hasCause(failure);
    }

    private static NewPageEntity newPage(PageType type, String parentId) {
        var page = new NewPageEntity();
        page.setName("page");
        page.setType(type);
        page.setParentId(parentId);
        page.setConfiguration(
            Map.of(PageConfigurationKeys.TRANSLATION_LANG, "fr", PageConfigurationKeys.TRANSLATION_INHERIT_CONTENT, "false")
        );
        return page;
    }

    private static Page page(String id, PageType type) {
        return Page.builder()
            .id(id)
            .name(id)
            .type(type.name())
            .referenceType(PageReferenceType.API)
            .referenceId(API_ID)
            .visibility("PUBLIC")
            .content("content")
            .configuration(Map.of(PageConfigurationKeys.TRANSLATION_LANG, "fr", PageConfigurationKeys.TRANSLATION_INHERIT_CONTENT, "false"))
            .build();
    }
}
