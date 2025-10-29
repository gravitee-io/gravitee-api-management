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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.PortalNavigationItem;
import java.util.List;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalnavigationitem-tests/";
    }

    @Test
    public void should_find_all_navigation_items_for_organization_and_environment() throws Exception {
        List<PortalNavigationItem> items = portalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId("org-1", "env-1");

        assertThat(items).isNotNull();
        assertThat(items).hasSize(2);
        assertThat(items).anyMatch(i -> "nav-item-1".equals(i.getId()));
        assertThat(items).anyMatch(i -> "nav-item-2".equals(i.getId()));
    }

    @Test
    public void should_create_and_delete_navigation_item() throws Exception {
        PortalNavigationItem item = PortalNavigationItem.builder()
            .id("new-nav-item")
            .organizationId("org-1")
            .environmentId("env-1")
            .title("Support")
            .type(PortalNavigationItem.Type.LINK)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .order(3)
            .configuration("{ \"url\": \"https://support.example.com\" }")
            .build();

        PortalNavigationItem created = portalNavigationItemRepository.create(item);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(item.getId());

        portalNavigationItemRepository.delete(item.getId());
        var maybeFound = portalNavigationItemRepository.findById(item.getId());
        assertThat(maybeFound).isEmpty();
    }
}
