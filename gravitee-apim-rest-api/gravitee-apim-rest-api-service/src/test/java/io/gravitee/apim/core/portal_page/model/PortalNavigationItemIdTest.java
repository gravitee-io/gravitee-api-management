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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemIdTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .build();
    private static final String API_ID = "00000000-0000-0000-0000-0000000000a1";

    @Test
    void forApiLink_is_deterministic_and_independent_of_any_portal() {
        var first = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");
        var second = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void forApiLink_differs_per_api_and_per_link_hrid() {
        var link = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");
        var otherLink = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "support");
        var otherApi = PortalNavigationItemId.forApiLink(AUDIT_INFO, "other-api-id", "external-docs");

        assertThat(link).isNotEqualTo(otherLink);
        assertThat(link).isNotEqualTo(otherApi);
    }

    @Test
    void api_folder_id_is_the_same_id_regardless_of_which_portal_lists_the_api() {
        var folder = HRIDToUUID.navigation().context(AUDIT_INFO).api(API_ID).folderId("/guides");

        assertThat(folder).isEqualTo(HRIDToUUID.navigation().context(AUDIT_INFO).api(API_ID).folderId("/guides"));
        assertThat(folder).isNotEqualTo(HRIDToUUID.navigation().context(AUDIT_INFO).api("other-api-id").folderId("/guides"));
    }

    @Test
    void api_documentation_id_is_keyed_on_the_api_not_on_a_listing_row() {
        var contentId = PortalPageContentId.random();

        assertThat(HRIDToUUID.navigation().context(AUDIT_INFO).api(API_ID).documentation(contentId).modelId()).isEqualTo(
            HRIDToUUID.navigation().context(AUDIT_INFO).api(API_ID).documentation(contentId).modelId()
        );
        assertThat(HRIDToUUID.navigation().context(AUDIT_INFO).api(API_ID).documentation(contentId).modelId()).isNotEqualTo(
            HRIDToUUID.navigation().context(AUDIT_INFO).api("other-api-id").documentation(contentId).modelId()
        );
    }
}
