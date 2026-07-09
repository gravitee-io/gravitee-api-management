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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemIdTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    @Nested
    class ForApiDocumentation {

        @Test
        void returns_the_same_id_as_hrid_to_uuid_model_id() {
            var navApiRowId = PortalNavigationItemId.random();
            var contentId = PortalPageContentId.of("22222222-2222-2222-2222-222222222222");

            var expected = HRIDToUUID.navigation()
                .context(AUDIT_INFO)
                .api(navApiRowId.toString())
                .documentation(contentId.toString())
                .modelId();

            assertThat(PortalNavigationItemId.forApiDocumentation(AUDIT_INFO, navApiRowId, contentId)).isEqualTo(expected);
        }
    }

    @Nested
    class ForApiFolder {

        @Test
        void returns_the_same_id_as_hrid_to_uuid_model_id() {
            var navApiRowId = PortalNavigationItemId.random();

            var expected = HRIDToUUID.navigation().context(AUDIT_INFO).api(navApiRowId.toString()).folder("/getting-started").modelId();

            assertThat(PortalNavigationItemId.forApiFolder(AUDIT_INFO, navApiRowId, "/getting-started")).isEqualTo(expected);
        }
    }
}
