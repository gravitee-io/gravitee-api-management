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
package io.gravitee.rest.api.management.v2.rest.resource.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.documentation.PageSourceDomainServiceImpl;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentation;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.model.PageSource;
import io.gravitee.rest.api.management.v2.rest.model.SourceConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentation;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.model.Visibility;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every v2 response carrying a page masks the secret of its source, and only the returned copy is masked: the stored page
 * keeps the secret it needs to fetch.
 */
class ApiPagesResource_SensitiveSourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final String API_ID = "api-id";
    private static final String PAGE_ID = "page-id";
    private static final String ROLE_ID = "role-id";
    // Value that the in-memory page source service recognises as a stored secret and masks.
    private static final String SOURCE_VALUE = "I'm a sensitive" + " data";
    private static final String MASK = PageSourceDomainServiceImpl.SENSITIVE_DATA_REPLACEMENT;
    private static final String STORED_CONFIGURATION = "{\"field\":\"" + SOURCE_VALUE + "\"}";

    @Autowired
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    private PageQueryServiceInMemory pageQueryServiceInMemory;

    @Autowired
    private PageCrudServiceInMemory pageCrudServiceInMemory;

    @Autowired
    private UserCrudServiceInMemory userCrudService;

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Autowired
    private MembershipQueryServiceInMemory membershipQueryService;

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));
        givenApiPrimaryOwner();
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(
            pageQueryServiceInMemory,
            pageCrudServiceInMemory,
            apiCrudServiceInMemory,
            userCrudService,
            roleQueryService,
            membershipQueryService
        ).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/pages";
    }

    @Test
    void should_mask_the_source_secret_in_the_create_response() {
        var request = (CreateDocumentationMarkdown) new CreateDocumentationMarkdown()
            .content("content")
            .source(new SourceConfiguration().type("github-fetcher").configuration(sourceConfiguration()))
            .type(CreateDocumentation.TypeEnum.MARKDOWN)
            .visibility(Visibility.PUBLIC)
            .name("created page");

        final Response response = rootTarget().request().post(Entity.json(request));

        assertThat(response.getStatus()).isEqualTo(201);
        assertResponseMasksTheSecret(response);
        assertThat(pageCrudServiceInMemory.storage()).singleElement().satisfies(this::keepsTheSecret);
    }

    @Test
    void should_mask_the_source_secret_restored_by_the_update_response() {
        givenStoredPage(false);
        var request = new UpdateDocumentationMarkdown()
            .content("content")
            .type(UpdateDocumentation.TypeEnum.MARKDOWN)
            .order(0)
            .visibility(Visibility.PUBLIC)
            .source(new PageSource().type("github-fetcher").configuration(Map.of("field", MASK)))
            .name("page");

        final Response response = rootTarget().path(PAGE_ID).request().put(Entity.json(request));

        assertThat(response.getStatus()).isEqualTo(200);
        assertResponseMasksTheSecret(response);
        // The masked secret sent back is restored, not stored as the placeholder (PORTAL-242 behaviour)
        assertThat(pageCrudServiceInMemory.get(PAGE_ID)).satisfies(this::keepsTheSecret);
    }

    @Test
    void should_mask_the_source_secret_in_the_publish_response() {
        givenStoredPage(false);

        final Response response = rootTarget().path(PAGE_ID + "/_publish").request().post(Entity.json(""));

        assertThat(response.getStatus()).isEqualTo(200);
        assertResponseMasksTheSecret(response);
        assertThat(pageCrudServiceInMemory.get(PAGE_ID)).satisfies(this::keepsTheSecret);
    }

    @Test
    void should_mask_the_source_secret_in_the_unpublish_response() {
        givenStoredPage(true);

        final Response response = rootTarget().path(PAGE_ID + "/_unpublish").request().post(Entity.json(""));

        assertThat(response.getStatus()).isEqualTo(200);
        assertResponseMasksTheSecret(response);
        assertThat(pageCrudServiceInMemory.get(PAGE_ID)).satisfies(this::keepsTheSecret);
    }

    @Test
    void should_mask_the_source_secret_in_the_fetch_response() {
        givenStoredPage(false);

        final Response response = rootTarget().path(PAGE_ID + "/_fetch").request().post(Entity.json(""));

        assertThat(response.getStatus()).isEqualTo(200);
        assertResponseMasksTheSecret(response);
        assertThat(pageCrudServiceInMemory.get(PAGE_ID)).satisfies(this::keepsTheSecret);
    }

    @Test
    void should_still_mask_the_source_secret_in_the_read_response() {
        givenStoredPage(true);

        final Response response = rootTarget().path(PAGE_ID).request().get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertResponseMasksTheSecret(response);
    }

    private static Map<String, Object> sourceConfiguration() {
        return Map.of("field", SOURCE_VALUE);
    }

    private void givenStoredPage(boolean published) {
        var source = io.gravitee.apim.core.documentation.model.PageSource.builder()
            .type("github-fetcher")
            .configuration(STORED_CONFIGURATION)
            .build();
        var page = Page.builder()
            .referenceType(Page.ReferenceType.API)
            .referenceId(API_ID)
            .type(Page.Type.MARKDOWN)
            .id(PAGE_ID)
            .name("page")
            .content("old content")
            .visibility(Page.Visibility.PUBLIC)
            .published(published)
            .source(source)
            .build();
        pageQueryServiceInMemory.initWith(List.of(page));
        pageCrudServiceInMemory.initWith(List.of(page));
    }

    private void givenApiPrimaryOwner() {
        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .id(ROLE_ID)
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(API_ID)
                    .roleId(ROLE_ID)
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").build()));
    }

    private static void assertResponseMasksTheSecret(Response response) {
        assertThat(response.readEntity(String.class))
            .as("the response must carry the source with its secret masked")
            .doesNotContain(SOURCE_VALUE)
            .contains(MASK);
    }

    private void keepsTheSecret(Page page) {
        assertThat(page.getSource().getConfiguration()).as("the stored page keeps the real secret").contains(SOURCE_VALUE);
    }
}
