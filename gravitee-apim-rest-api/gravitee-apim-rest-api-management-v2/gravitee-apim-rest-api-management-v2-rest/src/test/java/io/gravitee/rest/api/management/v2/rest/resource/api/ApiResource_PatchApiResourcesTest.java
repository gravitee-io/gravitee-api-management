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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiResourcesTest extends ApiResourceTest {

    private static final String MERGE_PATCH_TYPE = "application/merge-patch+json";
    private static final String JSON_PATCH_TYPE = "application/json-patch+json";

    @Inject
    UpdateApiDomainService updateApiDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void setUpApiAndPrimaryOwner() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API).build()));

        var apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setUpdatedAt(Date.from(Instant.ofEpochMilli(1000)));
        when(apiSearchServiceV4.findGenericById(any(), eq(API), any(boolean.class), any(boolean.class), any(boolean.class))).thenReturn(
            apiEntity
        );

        roleQueryService.resetSystemRoles(ORGANIZATION);
        primaryOwnerDomainService.initWith(
            List.of(Map.entry(API, PrimaryOwnerEntity.builder().id(USER_NAME).type(PrimaryOwnerEntity.Type.USER).build()))
        );
        membershipQueryServiceInMemory.initWith(
            List.of(
                Membership.builder()
                    .memberId(USER_NAME)
                    .referenceId(API)
                    .roleId("api-po-id-" + ORGANIZATION)
                    .referenceType(Membership.ReferenceType.API)
                    .memberType(Membership.Type.USER)
                    .build()
            )
        );

        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .updateV4(any(), any());
        doAnswer(inv -> inv.getArgument(0))
            .when(updateApiDomainService)
            .validateV4(any(), any());
    }

    @AfterEach
    public void tearDown() {
        Stream.of(apiCrudService, membershipQueryServiceInMemory, primaryOwnerDomainService).forEach(InMemoryAlternative::reset);
        reset(updateApiDomainService, apiSearchServiceV4);
    }

    @Test
    void merge_patch_resources_returns_them_in_response_body() {
        var body =
            "{\"resources\":[{\"name\":\"my-cache\",\"type\":\"cache\",\"enabled\":true,\"configuration\":{\"timeToIdleSeconds\":0,\"timeToLiveSeconds\":0,\"maxEntriesLocalHeap\":1000}}]}";

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getResources()).hasSize(1);
        Assertions.assertThat(apiV4.getResources().getFirst().getName()).isEqualTo("my-cache");
        Assertions.assertThat(apiV4.getResources().getFirst().getType()).isEqualTo("cache");
    }

    @Test
    void json_patch_tests_and_replaces_existing_resource_configuration() {
        var resource = Resource.builder().name("my-cache").type("cache").enabled(true).configuration("{\"timeToLiveSeconds\":10}").build();
        var baseApi = ApiFixtures.aProxyApiV4();
        apiCrudService.initWith(
            List.of(
                baseApi
                    .toBuilder()
                    .id(API)
                    .apiDefinitionValue(baseApi.getApiDefinitionHttpV4().toBuilder().resources(List.of(resource)).build())
                    .build()
            )
        );

        var body =
            "[" +
            "{\"op\":\"test\",\"path\":\"/resources/0/configuration\",\"value\":{\"timeToLiveSeconds\":10}}," +
            "{\"op\":\"replace\",\"path\":\"/resources/0/configuration\",\"value\":{\"timeToLiveSeconds\":60}}" +
            "]";

        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getResources()).hasSize(1);
        Assertions.assertThat(apiV4.getResources().getFirst().getConfiguration())
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("timeToLiveSeconds", 60);
    }
}
