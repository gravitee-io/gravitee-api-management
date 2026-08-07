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
import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.AutomationMetadata;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalPageContent;
import java.util.Collections;
import java.util.List;
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
class PortalNavigationItemAutomationMetadataUpgraderTest {

    private static final String ORG = "DEFAULT";
    private static final String ENV = "DEFAULT";
    private static final String ANOTHER_ORG = "ANOTHER_ORG";
    private static final String ANOTHER_ENV = "ANOTHER_ENVIRONMENT";

    @Mock
    PortalPageContentRepository portalPageContentRepository;

    @Mock
    PortalNavigationItemRepository portalNavigationItemRepository;

    private PortalNavigationItemAutomationMetadataUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new PortalNavigationItemAutomationMetadataUpgrader(portalPageContentRepository, portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_content_rows() {
        when(portalPageContentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_content_rows_have_automation_metadata() {
        PortalPageContent content = content("content-1", ORG, ENV, null);
        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_throw_upgrader_exception_on_repository_error() {
        when(portalPageContentRepository.findAll()).thenThrow(new TechnicalException("connection failed"));

        final Executable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("connection failed");
    }

    @Test
    @SneakyThrows
    void should_copy_automation_metadata_onto_matching_nav_item() {
        AutomationMetadata metadata = automationMetadata("api-1");
        PortalPageContent content = content("content-1", ORG, ENV, metadata);
        PortalNavigationItem page = navItem("page-1", ORG, ENV, "content-1", null);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(page));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(1)).update(captor.capture());

        PortalNavigationItem updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo("page-1");
        assertThat(updated.getAutomationMetadata()).usingRecursiveComparison().isEqualTo(metadata.trimmedForNavItem());
    }

    @Test
    @SneakyThrows
    void should_trim_name_and_order_off_backfilled_automation_metadata() {
        AutomationMetadata metadata = AutomationMetadata.builder()
            .referenceType(AutomationTargetReferenceType.API)
            .referenceId("api-1")
            .name("Some Page Name")
            .location("/some/location")
            .order(3)
            .build();
        PortalPageContent content = content("content-1", ORG, ENV, metadata);
        PortalNavigationItem page = navItem("page-1", ORG, ENV, "content-1", null);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(page));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(1)).update(captor.capture());

        AutomationMetadata updatedMetadata = captor.getValue().getAutomationMetadata();
        assertThat(updatedMetadata.getReferenceType()).isEqualTo(AutomationTargetReferenceType.API);
        assertThat(updatedMetadata.getReferenceId()).isEqualTo("api-1");
        assertThat(updatedMetadata.getLocation()).isEqualTo("/some/location");
        assertThat(updatedMetadata.getName()).isNull();
        assertThat(updatedMetadata.getOrder()).isNull();
    }

    @Test
    @SneakyThrows
    void should_backfill_every_nav_item_sharing_the_same_portal_page_content_id() {
        AutomationMetadata metadata = automationMetadata("api-1");
        PortalPageContent content = content("content-1", ORG, ENV, metadata);
        PortalNavigationItem pageA = navItem("page-a", ORG, ENV, "content-1", null);
        PortalNavigationItem pageB = navItem("page-b", ORG, ENV, "content-1", null);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(pageA, pageB));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());

        List<PortalNavigationItem> updated = captor.getAllValues();
        assertThat(updated).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder("page-a", "page-b");
        assertThat(updated).allSatisfy(item -> assertThat(item.getAutomationMetadata()).isNotNull());
    }

    @Test
    @SneakyThrows
    void should_skip_content_row_with_no_matching_nav_item() {
        PortalPageContent content = content("content-1", ORG, ENV, automationMetadata("api-1"));
        PortalNavigationItem unrelatedPage = navItem("page-1", ORG, ENV, "some-other-content-id", null);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(unrelatedPage));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_not_overwrite_nav_item_that_already_has_automation_metadata() {
        AutomationMetadata existing = automationMetadata("api-existing");
        AutomationMetadata fromContent = automationMetadata("api-1");
        PortalPageContent content = content("content-1", ORG, ENV, fromContent);
        PortalNavigationItem alreadyMigrated = navItem("page-1", ORG, ENV, "content-1", existing);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(alreadyMigrated));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_ignore_non_page_nav_items() {
        PortalPageContent content = content("content-1", ORG, ENV, automationMetadata("api-1"));
        PortalNavigationItem folder = PortalNavigationItem.builder()
            .id("folder-1")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.FOLDER)
            .rootId("folder-1")
            .configuration("{\"portalPageContentId\":\"content-1\"}")
            .build();

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(folder));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_process_multiple_environments() {
        AutomationMetadata metadataEnv1 = automationMetadata("api-1");
        AutomationMetadata metadataEnv2 = automationMetadata("api-2");
        PortalPageContent contentEnv1 = content("content-1", ORG, ENV, metadataEnv1);
        PortalPageContent contentEnv2 = content("content-2", ANOTHER_ORG, ANOTHER_ENV, metadataEnv2);
        PortalNavigationItem pageEnv1 = navItem("page-1", ORG, ENV, "content-1", null);
        PortalNavigationItem pageEnv2 = navItem("page-2", ANOTHER_ORG, ANOTHER_ENV, "content-2", null);

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(contentEnv1, contentEnv2));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(pageEnv1));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ANOTHER_ORG, ANOTHER_ENV)).thenReturn(
            List.of(pageEnv2)
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());

        List<PortalNavigationItem> updated = captor.getAllValues();
        assertThat(updated).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrder("page-1", "page-2");
        assertThat(updated)
            .filteredOn(item -> "page-1".equals(item.getId()))
            .singleElement()
            .satisfies(item -> assertThat(item.getAutomationMetadata()).isEqualTo(metadataEnv1));
        assertThat(updated)
            .filteredOn(item -> "page-2".equals(item.getId()))
            .singleElement()
            .satisfies(item -> assertThat(item.getAutomationMetadata()).isEqualTo(metadataEnv2));
    }

    @Test
    @SneakyThrows
    void should_skip_page_with_unparsable_configuration() {
        PortalPageContent content = content("content-1", ORG, ENV, automationMetadata("api-1"));
        PortalNavigationItem malformedPage = PortalNavigationItem.builder()
            .id("page-1")
            .organizationId(ORG)
            .environmentId(ENV)
            .type(PortalNavigationItem.Type.PAGE)
            .rootId("page-1")
            .configuration("not-json")
            .build();

        when(portalPageContentRepository.findAll()).thenReturn(Set.of(content));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId(ORG, ENV)).thenReturn(List.of(malformedPage));

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    void should_have_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.PORTAL_NAVIGATION_ITEM_AUTOMATION_METADATA_UPGRADER);
    }

    private static PortalPageContent content(String id, String organizationId, String environmentId, AutomationMetadata metadata) {
        return PortalPageContent.builder()
            .id(id)
            .organizationId(organizationId)
            .environmentId(environmentId)
            .type(PortalPageContent.Type.GRAVITEE_MARKDOWN)
            .content("some content")
            .automationMetadata(metadata)
            .build();
    }

    private static PortalNavigationItem navItem(
        String id,
        String organizationId,
        String environmentId,
        String portalPageContentId,
        AutomationMetadata automationMetadata
    ) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(organizationId)
            .environmentId(environmentId)
            .title("Nav Item " + id)
            .type(PortalNavigationItem.Type.PAGE)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .rootId(id)
            .order(0)
            .published(true)
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .configuration("{\"portalPageContentId\":\"" + portalPageContentId + "\"}")
            .automationMetadata(automationMetadata)
            .build();
    }

    private static AutomationMetadata automationMetadata(String referenceId) {
        return AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.API).referenceId(referenceId).build();
    }
}
