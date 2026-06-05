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
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiResource_PatchApiGroupsTest extends ApiResourceTest {

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
        givenApiWithGroups(Set.of("group-1"));

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

    private void givenApiWithGroups(Set<String> groups) {
        var api = ApiFixtures.aProxyApiV4().toBuilder().groups(new HashSet<>(groups)).build();
        apiCrudService.initWith(List.of(api));
    }

    @Test
    void merge_patch_sets_groups_returns_200_and_reflects_groups() {
        var body = "{\"groups\":[\"group-2\"]}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getGroups()).containsExactly("group-2");
        Assertions.assertThat(capturePersistedGroups()).containsExactly("group-2");
    }

    @Test
    void merge_patch_groups_null_clears_groups() {
        var body = "{\"groups\":null}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getGroups()).isNullOrEmpty();
        Assertions.assertThat(capturePersistedGroups()).isNullOrEmpty();
    }

    @Test
    void json_patch_replace_groups_returns_200_and_reflects_groups() {
        var body = "[{\"op\":\"replace\",\"path\":\"/groups\",\"value\":[\"group-2\"]}]";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getGroups()).containsExactly("group-2");
    }

    @Test
    void json_patch_remove_groups_clears_groups() {
        var body = "[{\"op\":\"remove\",\"path\":\"/groups\"}]";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, JSON_PATCH_TYPE));

        var apiV4 = assertThat(response).hasStatus(OK_200).asEntity(ApiV4.class).actual();
        Assertions.assertThat(apiV4.getGroups()).isNullOrEmpty();
        Assertions.assertThat(capturePersistedGroups()).isNullOrEmpty();
    }

    @Test
    void patch_with_unknown_group_is_rejected_and_not_persisted() {
        doThrow(new InvalidDataException("These groups [unknown-group] do not exist")).when(updateApiDomainService).updateV4(any(), any());

        var body = "{\"groups\":[\"unknown-group\"]}";
        var response = rootTarget(API).request().method("PATCH", Entity.entity(body, MERGE_PATCH_TYPE));

        var error = assertThat(response).hasStatus(BAD_REQUEST_400).asError().actual();
        Assertions.assertThat(error.getTechnicalCode()).isEqualTo("data.invalid");
        Assertions.assertThat(apiCrudService.get(API).getGroups()).containsExactly("group-1");
    }

    private Set<String> capturePersistedGroups() {
        var captor = ArgumentCaptor.forClass(Api.class);
        Mockito.verify(updateApiDomainService).updateV4(captor.capture(), any());
        return captor.getValue().getGroups();
    }
}
