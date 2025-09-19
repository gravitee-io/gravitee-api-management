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

import static fixtures.PageModelFixtures.aModelPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.PageConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PageDuplicateServiceTest {

    private static final String API_ID = "myAPI";
    private static final String DUPLICATE_API_ID = "duplicate-api-id";

    private static final String ORGANIZATION_ID = "my-org";
    private static final String ENVIRONMENT_ID = "my-env";
    private static final String USER_ID = "user-id";

    @Mock
    private PageService pageService;

    @Captor
    ArgumentCaptor<NewPageEntity> newPageCaptor;

    PageConverter pageConverter = new PageConverter();

    PageDuplicateServiceImpl pageDuplicateService;

    @BeforeEach
    public void setUp() throws Exception {
        pageDuplicateService = new PageDuplicateServiceImpl(pageService, pageConverter);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        when(pageService.createPage(any(), any(String.class), any(NewPageEntity.class), any(String.class))).thenAnswer(invocation -> {
            NewPageEntity newPage = invocation.getArgument(2);
            String newId = invocation.getArgument(3);

            return PageEntity.builder()
                .id(newId)
                .crossId(newPage.getCrossId())
                .name(newPage.getName())
                .content(newPage.getContent())
                .type(newPage.getType().name())
                .order(newPage.getOrder())
                .published(newPage.isPublished())
                .visibility(newPage.getVisibility())
                .lastContributor(newPage.getLastContributor())
                .source(newPage.getSource())
                .configuration(newPage.getConfiguration())
                .homepage(newPage.isHomepage())
                .excludedGroups(newPage.getExcludedGroups())
                .accessControls(newPage.getAccessControls())
                .attachedMedia(newPage.getAttachedMedia())
                .parentId(newPage.getParentId())
                .referenceType("API")
                .referenceId(invocation.getArgument(1))
                .build();
        });
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void should_duplicate_pages_of_an_api() {
        PageEntity page1 = aModelPage().toBuilder().id("page-1-id").name("Page 1").type("SWAGGER").build();
        PageEntity page2 = aModelPage().toBuilder().id("page-2-id").name("Page 2").type("MARKDOWN").build();
        PageEntity page3 = aModelPage().toBuilder().id("page-3-id").name("Sub Page 3").type("ASCIIDOC").parentId(page2.getId()).build();

        when(pageService.search(GraviteeContext.getCurrentEnvironment(), new PageQuery.Builder().api(API_ID).build(), true)).thenReturn(
            List.of(page1, page2, page3)
        );

        pageDuplicateService.duplicatePages(GraviteeContext.getExecutionContext(), API_ID, DUPLICATE_API_ID, USER_ID);

        verify(pageService, times(3)).createPage(any(), eq(DUPLICATE_API_ID), newPageCaptor.capture(), any());

        String newIdPage1 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page1.getId());
        String newIdPage2 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page2.getId());
        String newIdPage3 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page3.getId());

        assertThat(newPageCaptor.getAllValues())
            .hasSize(3)
            .containsExactly(
                pageConverter.toNewPageEntity(page1.toBuilder().id(newIdPage1).crossId(null).lastContributor(USER_ID).build()),
                pageConverter.toNewPageEntity(page2.toBuilder().id(newIdPage2).crossId(null).lastContributor(USER_ID).build()),
                pageConverter.toNewPageEntity(
                    page3.toBuilder().id(newIdPage3).crossId(null).lastContributor(USER_ID).parentId(newIdPage2).build()
                )
            );
    }

    @Test
    public void should_return_mapping_between_source_page_id_and_duplicated_page_id() {
        PageEntity page1 = aModelPage().toBuilder().id("page-1-id").name("Page 1").type("SWAGGER").build();
        PageEntity page2 = aModelPage().toBuilder().id("page-2-id").name("Page 2").type("MARKDOWN").build();
        PageEntity page3 = aModelPage().toBuilder().id("page-3-id").name("Sub Page 3").type("ASCIIDOC").parentId(page2.getId()).build();

        when(pageService.search(GraviteeContext.getCurrentEnvironment(), new PageQuery.Builder().api(API_ID).build(), true)).thenReturn(
            List.of(page1, page2, page3)
        );

        Map<String, String> pagesIds = pageDuplicateService.duplicatePages(
            GraviteeContext.getExecutionContext(),
            API_ID,
            DUPLICATE_API_ID,
            USER_ID
        );

        String newIdPage1 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page1.getId());
        String newIdPage2 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page2.getId());
        String newIdPage3 = UuidString.generateForEnvironment(ENVIRONMENT_ID, DUPLICATE_API_ID, page3.getId());

        assertThat(pagesIds)
            .hasSize(3)
            .containsEntry(page1.getId(), newIdPage1)
            .containsEntry(page2.getId(), newIdPage2)
            .containsEntry(page3.getId(), newIdPage3);
    }
}
