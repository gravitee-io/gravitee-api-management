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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_DOCUMENTATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdateApiLinkUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidateApiLinkUseCase;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiLinksResourceTest extends AbstractResourceTest {

    private static final String API_HRID = "pets-api";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final String API_ID = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
    private static final PortalNavigationItemId LINK_ID = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");

    @Inject
    private CreateOrUpdateApiLinkUseCase createOrUpdateApiLinkUseCase;

    @Inject
    private ValidateApiLinkUseCase validateApiLinkUseCase;

    @Inject
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService;

    @AfterEach
    void tearDown() {
        reset(createOrUpdateApiLinkUseCase, validateApiLinkUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/links";
    }

    private static PortalNavigationLink aPersistedLink() {
        return PortalNavigationLink.builder()
            .id(LINK_ID)
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title("External Docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    @Nested
    class Authorization {

        @BeforeEach
        void setUp() {
            when(createOrUpdateApiLinkUseCase.execute(any())).thenReturn(
                new CreateOrUpdateApiLinkUseCase.Output(aPersistedLink(), List.of())
            );
            when(validateApiLinkUseCase.execute(any())).thenReturn(new CreateOrUpdateApiLinkUseCase.Output(null, List.of()));
        }

        @AfterEach
        void tearDown() {
            portalNavigationItemsCrudService.reset();
            reset(permissionService);
        }

        @Test
        void should_return_403_when_the_caller_cannot_update_the_existing_link() {
            portalNavigationItemsCrudService.initWith(List.of(aPersistedLink()));
            deny(UPDATE);

            assertThat(put(false)).isEqualTo(403);
            verify(createOrUpdateApiLinkUseCase, never()).execute(any());
        }

        @Test
        void should_return_403_on_dry_run_when_the_caller_cannot_update_the_existing_link() {
            portalNavigationItemsCrudService.initWith(List.of(aPersistedLink()));
            deny(UPDATE);

            assertThat(put(true)).isEqualTo(403);
            verify(validateApiLinkUseCase, never()).execute(any());
        }

        @Test
        void should_update_the_existing_link_with_update_and_no_create() {
            portalNavigationItemsCrudService.initWith(List.of(aPersistedLink()));
            deny(CREATE);

            assertThat(put(false)).isEqualTo(200);
            verify(createOrUpdateApiLinkUseCase).execute(any());
        }

        @Test
        void should_create_a_new_link_with_create_and_no_update() {
            deny(UPDATE);

            assertThat(put(false)).isEqualTo(200);
            verify(createOrUpdateApiLinkUseCase).execute(any());
        }

        @Test
        void should_return_403_when_the_caller_cannot_create_a_new_link() {
            deny(CREATE);

            assertThat(put(false)).isEqualTo(403);
            verify(createOrUpdateApiLinkUseCase, never()).execute(any());
        }

        @Test
        void should_authorize_against_the_api_the_import_writes_when_the_api_hrid_is_flagged_as_an_id() {
            portalNavigationItemsCrudService.initWith(List.of(aPersistedLink()));
            deny(UPDATE);

            assertThat(put(false, true)).isEqualTo(403);
            verify(createOrUpdateApiLinkUseCase, never()).execute(any());
        }

        private void deny(RolePermissionAction action) {
            when(permissionService.hasPermission(any(), eq(API_DOCUMENTATION), eq(API_ID), eq(action))).thenReturn(false);
        }

        private int put(boolean dryRun) {
            return put(dryRun, false);
        }

        private int put(boolean dryRun, boolean hridContainsApiUUID) {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .queryParam("hridContainsApiUUID", hridContainsApiUUID)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("api-link.json")))
            ) {
                return response.getStatus();
            }
        }
    }
}
