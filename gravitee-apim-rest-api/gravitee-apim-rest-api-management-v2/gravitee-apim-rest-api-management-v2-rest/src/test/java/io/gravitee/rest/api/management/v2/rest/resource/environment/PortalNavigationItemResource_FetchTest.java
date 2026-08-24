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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.rest.api.management.v2.rest.model.FetchPortalNavigationItemResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemFetchResult;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemResource_FetchTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    @Inject
    private PortalNavigationItemCrudService portalNavigationItemCrudService;

    @Inject
    private PortalPageContentCrudService portalPageContentCrudService;

    @Inject
    private PortalPageContentQueryService portalPageContentQueryService;

    @Inject
    private PortalNavigationItemSourceDomainService portalNavigationItemSourceDomainService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        ((PortalNavigationItemsQueryServiceInMemory) portalNavigationItemsQueryService).initWith(List.of());
        ((PortalNavigationItemsCrudServiceInMemory) portalNavigationItemCrudService).initWith(List.of());
        ((PortalPageContentCrudServiceInMemory) portalPageContentCrudService).initWith(List.of());
        ((PortalPageContentQueryServiceInMemory) portalPageContentQueryService).initWith(List.of());
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemId parentId) {
        var folder = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .build();
        seed(folder);
        return folder;
    }

    private PortalNavigationPage aPage(String title, PortalNavigationItemId parentId, PortalNavigationItemSource source) {
        var content = GraviteeMarkdownPageContent.create(ORGANIZATION, ENVIRONMENT, "# Default content");
        portalPageContentCrudService.create(content);
        ((PortalPageContentQueryServiceInMemory) portalPageContentQueryService).storage().add(content);

        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(content.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(parentId)
            .source(source)
            .build();
        seed(page);
        return page;
    }

    private void seed(PortalNavigationItem item) {
        // The query fake shares the crud storage: creating through the crud service is enough
        portalNavigationItemCrudService.create(item);
    }

    private PortalNavigationItemSource aSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration(
                "{\"url\":\"https://example.com/doc.md\",\"token\":\"%s\"}".formatted(
                    PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA
                )
            )
            .build();
    }

    private Response fetch(String navId) {
        return target.path(navId).path("_fetch").request().post(Entity.json(""));
    }

    @Test
    void should_fetch_page_content_and_return_updated_item_with_masked_source() {
        var page = aPage("Sourced Page", null, aSource());

        Response response = fetch(page.getId().toString());

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(FetchPortalNavigationItemResponse.class);
        assertThat(body.getSummary()).isNull();
        var item = (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage) body.getItem().getActualInstance();
        assertThat(item.getSource()).isNotNull();
        assertThat(item.getSource().getLastFetchedAt()).isNotNull();
        assertThat(item.getSource().getLastFetchError()).isNull();
        assertThat((Map<String, Object>) item.getSource().getConfiguration()).containsEntry(
            "token",
            PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT
        );

        assertThat(storedContentOf(page)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_return_the_item_with_a_masked_source_and_a_server_built_error_when_the_page_fetch_fails() {
        var page = aPage("Sourced Page", null, aSource());
        ((PortalNavigationItemSourceDomainServiceInMemory) portalNavigationItemSourceDomainService).failNextFetchWith(
            new TechnicalDomainException("fetch went wrong")
        );

        Response response = fetch(page.getId().toString());

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(FetchPortalNavigationItemResponse.class);
        assertThat(body.getSummary()).isNull();
        var item = (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage) body.getItem().getActualInstance();
        assertThat(item.getSource().getLastFetchedAt()).isNull();
        assertThat(item.getSource().getLastFetchError()).isEqualTo("Unable to fetch content from source type http-fetcher.");
        assertThat(item.getSource().getLastFetchError()).doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
        assertThat((Map<String, Object>) item.getSource().getConfiguration()).containsEntry(
            "token",
            PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT
        );

        assertThat(storedContentOf(page)).isEqualTo("# Default content");
    }

    @Test
    void should_fetch_sourced_page_descendants_of_folder_and_return_summary() {
        var folder = aFolder("Guides", null);
        var subFolder = aFolder("Advanced", folder.getId());
        var sourcedPage = aPage("Sourced Page", folder.getId(), aSource());
        var nestedSourcedPage = aPage("Nested Sourced Page", subFolder.getId(), aSource());
        aPage("Inline Page", folder.getId(), null);

        Response response = fetch(folder.getId().toString());

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(FetchPortalNavigationItemResponse.class);
        assertThat(body.getItem()).isNull();
        var summary = body.getSummary();
        assertThat(summary.getSucceeded()).isEqualTo(2);
        assertThat(summary.getFailed()).isZero();
        assertThat(summary.getResults())
            .extracting(PortalNavigationItemFetchResult::getNavigationItemId, PortalNavigationItemFetchResult::getSuccess)
            .containsExactlyInAnyOrder(tuple(sourcedPage.getId().id(), true), tuple(nestedSourcedPage.getId().id(), true));

        assertThat(storedContentOf(sourcedPage)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
        assertThat(storedContentOf(nestedSourcedPage)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_return_summary_with_failure_when_one_page_fetch_fails() {
        var folder = aFolder("Guides", null);
        aPage("Failing Page", folder.getId(), aSource());
        aPage("Working Page", folder.getId(), aSource());
        ((PortalNavigationItemSourceDomainServiceInMemory) portalNavigationItemSourceDomainService).failNextFetchWith(
            new TechnicalDomainException("fetch went wrong")
        );

        Response response = fetch(folder.getId().toString());

        assertThat(response).hasStatus(OK_200);
        var summary = response.readEntity(FetchPortalNavigationItemResponse.class).getSummary();
        assertThat(summary.getSucceeded()).isEqualTo(1);
        assertThat(summary.getFailed()).isEqualTo(1);
        assertThat(summary.getResults())
            .filteredOn(result -> !result.getSuccess())
            .singleElement()
            .satisfies(result -> {
                assertThat(result.getError()).isEqualTo("Unable to fetch content from source type http-fetcher.");
                assertThat(result.getError()).doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
            });
    }

    @Test
    void should_return_400_when_page_has_no_source() {
        var page = aPage("Inline Page", null, null);

        assertThat(fetch(page.getId().toString())).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_folder_has_no_sourced_descendant() {
        var folder = aFolder("Guides", null);
        aPage("Inline Page", folder.getId(), null);

        assertThat(fetch(folder.getId().toString())).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_when_item_not_found() {
        assertThat(fetch(PortalNavigationItemId.random().toString())).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_403_when_user_lacks_update_permission() {
        var page = aPage("Sourced Page", null, aSource());
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        assertThat(fetch(page.getId().toString())).hasStatus(FORBIDDEN_403);
    }

    private String storedContentOf(PortalNavigationPage page) {
        return ((PortalPageContentCrudServiceInMemory) portalPageContentCrudService).storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .map(content -> ((GraviteeMarkdownPageContent) content).getContent().value())
            .orElseThrow();
    }
}
