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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
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
class PortalNavigationItemDefaultSegmentUpgraderTest {

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalNavigationItemRepository portalNavigationItemRepository;

    private PortalNavigationItemDefaultSegmentUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new PortalNavigationItemDefaultSegmentUpgrader(environmentRepository, portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_environments() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(portalNavigationItemRepository);
    }

    @Test
    @SneakyThrows
    void should_return_true_when_no_nav_items_in_environment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            Collections.emptyList()
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_slugify_title_and_set_segment_for_items_with_null_segment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(navItemNullSegment("item-1", "My Projects"), navItemNullSegment("item-2", "APIs & SDKs!"))
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(PortalNavigationItem::getSegment)
            .containsExactlyInAnyOrder("my-projects", "apis-sdks");
    }

    @Test
    @SneakyThrows
    void should_slugify_title_and_set_segment_for_items_with_unset_placeholder_segment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(
                navItemWithSegment("item-1", "My Projects", "__CHANGE_ME__"),
                navItemWithSegment("item-2", "APIs & SDKs!", "__CHANGE_ME__")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(PortalNavigationItem::getSegment)
            .containsExactlyInAnyOrder("my-projects", "apis-sdks");
    }

    @Test
    @SneakyThrows
    void should_skip_items_that_already_have_a_segment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(navItemWithSegment("item-1", "My Projects", "my-projects"))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, never()).update(any());
    }

    @Test
    @SneakyThrows
    void should_process_multiple_environments() {
        var anotherEnv = Environment.builder().id("ENV2").organizationId("ORG2").hrids(List.of("env2")).name("env2").build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, anotherEnv));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(navItemNullSegment("item-1", "Getting Started"))
        );
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("ORG2", "ENV2")).thenReturn(
            List.of(navItemNullSegment("item-2", "Overview"))
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(portalNavigationItemRepository, times(2)).update(any());
    }

    @Test
    @SneakyThrows
    void should_deduplicate_segments_within_sibling_group() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("DEFAULT", "DEFAULT")).thenReturn(
            List.of(navItemNullSegment("item-1", "Q&A"), navItemNullSegment("item-2", "Q/A"))
        );

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<PortalNavigationItem> captor = ArgumentCaptor.forClass(PortalNavigationItem.class);
        verify(portalNavigationItemRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues()).extracting(PortalNavigationItem::getSegment).containsExactlyInAnyOrder("q-a", "q-a-2");
    }

    @Test
    void should_have_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.PORTAL_NAVIGATION_ITEM_DEFAULT_SEGMENT_UPGRADER);
    }

    private static PortalNavigationItem navItemNullSegment(String id, String title) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId("DEFAULT")
            .environmentId("DEFAULT")
            .title(title)
            .segment(null)
            .type(PortalNavigationItem.Type.FOLDER)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(0)
            .rootId(id)
            .published(true)
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();
    }

    private static PortalNavigationItem navItemWithSegment(String id, String title, String segment) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId("DEFAULT")
            .environmentId("DEFAULT")
            .title(title)
            .segment(segment)
            .type(PortalNavigationItem.Type.FOLDER)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(0)
            .rootId(id)
            .published(true)
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();
    }
}
