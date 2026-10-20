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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PortalNavigationItemRenderingDomainServiceTest {

    private static final String ENV_ID = "env-id";

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private final PortalNavigationItemRenderingDomainService service = new PortalNavigationItemRenderingDomainService(queryService);

    @ParameterizedTest
    @MethodSource("documentationRoots")
    void should_copy_all_fields_and_render_under_each_parent_without_mutating_the_root(PortalNavigationItem root) {
        root.markAsRoot();
        var firstParent = PortalNavigationItemFixtures.anApi();
        firstParent.markAsRoot();
        var secondParent = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().json(),
            "Second listing",
            null,
            firstParent.getApiId()
        );
        secondParent.setRootId(PortalNavigationItemId.random());

        var firstCopy = service.renderUnder(root, firstParent);
        var secondCopy = service.renderUnder(root, secondParent);

        assertThat(firstCopy).isNotSameAs(root).usingRecursiveComparison().ignoringFields("parentId", "rootId").isEqualTo(root);
        assertThat(secondCopy).isNotSameAs(root).usingRecursiveComparison().ignoringFields("parentId", "rootId").isEqualTo(root);
        assertThat(firstCopy.getParentId()).isEqualTo(firstParent.getId());
        assertThat(firstCopy.getRootId()).isEqualTo(firstParent.getRootId());
        assertThat(secondCopy.getParentId()).isEqualTo(secondParent.getId());
        assertThat(secondCopy.getRootId()).isEqualTo(secondParent.getRootId());
        assertThat(root.getParentId()).isNull();
        assertThat(root.getRootId()).isEqualTo(root.getId());
    }

    @Test
    void should_resolve_parent_from_created_items_and_preserve_result_order() {
        var parent = PortalNavigationItemFixtures.anApi();
        parent.markAsRoot();
        var folder = PortalNavigationItemFixtures.aFolder("API documentation");
        folder.markAsRoot();
        var nestedPage = PortalNavigationItemFixtures.aPage("Nested page", folder.getId());
        nestedPage.updateParent(folder);

        var commands = List.of(command(nestedPage, null), command(folder, parent.getId()), command(parent, null));
        var renderedParents = service.prepareRenderingParents(ENV_ID, commands);

        assertThat(renderedParents).isEmpty();
        var result = service.renderCreatedItems(commands, List.of(parent, folder, nestedPage), renderedParents);

        assertThat(result).extracting(PortalNavigationItem::getId).containsExactly(parent.getId(), folder.getId(), nestedPage.getId());
        assertThat(result.getFirst()).isSameAs(parent);
        assertThat(result.get(1).getParentId()).isEqualTo(parent.getId());
        assertThat(result.get(1).getRootId()).isEqualTo(parent.getRootId());
        assertThat(result.getLast()).isSameAs(nestedPage);
        assertThat(result.getLast().getRootId()).isEqualTo(folder.getId());
        assertThat(folder.getParentId()).isNull();
        assertThat(folder.getRootId()).isEqualTo(folder.getId());
    }

    @Test
    void should_resolve_selected_existing_parent_in_current_environment() {
        var selectedParent = PortalNavigationItemFixtures.anApi();
        selectedParent.markAsRoot();
        var otherEnvironmentParent = PortalNavigationItemFixtures.anApi();
        otherEnvironmentParent.setEnvironmentId("other-environment");
        otherEnvironmentParent.setRootId(PortalNavigationItemId.random());
        var otherListing = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().json(),
            "Other listing",
            null,
            selectedParent.getApiId()
        );
        queryService.initWith(List.of(otherEnvironmentParent, otherListing, selectedParent));
        var root = PortalNavigationItemFixtures.aFolder("API documentation");
        root.markAsRoot();

        var commands = List.of(command(root, selectedParent.getId()));
        var renderedParents = service.prepareRenderingParents(ENV_ID, commands);
        var result = service.renderCreatedItems(commands, List.of(root), renderedParents);

        assertThat(result)
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getParentId()).isEqualTo(selectedParent.getId());
                assertThat(item.getRootId()).isEqualTo(selectedParent.getRootId());
            });
    }

    @Test
    void should_not_resolve_parent_from_another_environment() {
        var parent = PortalNavigationItemFixtures.anApi();
        parent.setEnvironmentId("other-environment");
        queryService.initWith(List.of(parent));
        var root = PortalNavigationItemFixtures.aFolder("API documentation");
        root.markAsRoot();

        assertThatThrownBy(() -> service.prepareRenderingParents(ENV_ID, List.of(command(root, parent.getId())))).isInstanceOf(
            ParentNotFoundException.class
        );
        assertThat(root.getParentId()).isNull();
        assertThat(root.getRootId()).isEqualTo(root.getId());
    }

    @Test
    void should_preserve_prepared_parent_context_when_listing_changes_after_preparation() {
        var parent = PortalNavigationItemFixtures.anApi();
        parent.markAsRoot();
        var originalRootId = parent.getRootId();
        queryService.initWith(List.of(parent));
        var root = PortalNavigationItemFixtures.aFolder("API documentation");
        root.markAsRoot();
        var commands = List.of(command(root, parent.getId()));

        var renderedParents = service.prepareRenderingParents(ENV_ID, commands);
        parent.setRootId(PortalNavigationItemId.random());
        queryService.reset();
        var result = service.renderCreatedItems(commands, List.of(root), renderedParents);

        assertThat(result)
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getParentId()).isEqualTo(parent.getId());
                assertThat(item.getRootId()).isEqualTo(originalRootId);
            });
        assertThat(root.getParentId()).isNull();
        assertThat(root.getRootId()).isEqualTo(root.getId());
    }

    @Test
    void should_reject_non_container_during_parent_preparation() {
        var parent = PortalNavigationItemFixtures.aPage("Not a container", null);
        queryService.initWith(List.of(parent));
        var root = PortalNavigationItemFixtures.aFolder("API documentation");

        assertThatThrownBy(() -> service.prepareRenderingParents(ENV_ID, List.of(command(root, parent.getId()))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Rendered parent is not a container: " + parent.getId().json());
    }

    private static CreatePortalNavigationItem command(PortalNavigationItem item, PortalNavigationItemId renderedParentId) {
        return CreatePortalNavigationItem.builder().id(item.getId()).type(item.getType()).renderedParentId(renderedParentId).build();
    }

    private static Stream<PortalNavigationItem> documentationRoots() {
        var source = PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build();
        var metadata = new AutomationMetadata(AutomationMetadata.ReferenceType.API, "api-id", "docs", Optional.empty(), Optional.of(2));
        var reference = new NavigationItemReference.ApiReference("api-id");
        return Stream.of(
            PortalNavigationItemFixtures.aPage("Overview", null)
                .toBuilder()
                .reference(reference)
                .source(source)
                .automationMetadata(metadata)
                .build(),
            PortalNavigationItemFixtures.aFolder("Guides")
                .toBuilder()
                .reference(reference)
                .source(source)
                .automationMetadata(metadata)
                .build(),
            PortalNavigationItemFixtures.aLink().toBuilder().reference(reference).source(source).automationMetadata(metadata).build()
        );
    }
}
