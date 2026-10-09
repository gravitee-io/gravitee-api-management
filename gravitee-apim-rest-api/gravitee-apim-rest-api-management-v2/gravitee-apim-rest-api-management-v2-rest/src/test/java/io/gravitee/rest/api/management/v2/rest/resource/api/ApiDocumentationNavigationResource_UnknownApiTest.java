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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fixtures.PortalNavigationItemsFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ImportPortalNavigationRequest;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource;
import io.gravitee.rest.api.management.v2.rest.model.PublishApiToPortal;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The permission check answers for any API id an administrator sends, so each operation has to make sure
 * itself that the API is one of the environment.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationResource_UnknownApiTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String UNKNOWN_API_ID = "unknown-api-id";
    private static final String API_OF_ANOTHER_ENVIRONMENT = "api-of-another-environment";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudService.initWith(
            List.of(Api.builder().id(API_OF_ANOTHER_ENVIRONMENT).name("Elsewhere").environmentId("another-environment").build())
        );
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalNavigationItemCrudService.reset();
    }

    @ParameterizedTest
    @MethodSource("operations")
    void should_return_404_for_an_api_that_does_not_exist(Function<WebTarget, Response> operation) {
        Response response = operation.apply(documentationOf(UNKNOWN_API_ID));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("operations")
    void should_return_404_for_an_api_of_another_environment(Function<WebTarget, Response> operation) {
        Response response = operation.apply(documentationOf(API_OF_ANOTHER_ENVIRONMENT));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("operations")
    void should_return_403_without_revealing_that_the_api_does_not_exist(Function<WebTarget, Response> operation) {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

        Response response = operation.apply(documentationOf(UNKNOWN_API_ID));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    private WebTarget documentationOf(String apiId) {
        return rootTarget().path(apiId).path("portal-navigation-items");
    }

    private static Stream<Named<Function<WebTarget, Response>>> operations() {
        return Stream.of(
            Named.of("list", target -> target.request().get()),
            Named.of("publish locations", target -> target.path("_publish-locations").request().get()),
            Named.of("create", target ->
                target.request().post(json(PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null)))
            ),
            Named.of("import", target ->
                target
                    .path("_import")
                    .request()
                    .post(
                        json(
                            new ImportPortalNavigationRequest()
                                .title("Imported Docs")
                                .source(
                                    new PortalNavigationItemSource()
                                        .type("http-fetcher")
                                        .configuration(Map.of("url", "https://example.com/repo"))
                                )
                        )
                    )
            ),
            Named.of("publish", target ->
                target.path("_publish").request().post(json(new PublishApiToPortal().sectionId(UUID.randomUUID())))
            ),
            Named.of("unpublish", target -> target.path("_unpublish").request().post(json("")))
        );
    }
}
