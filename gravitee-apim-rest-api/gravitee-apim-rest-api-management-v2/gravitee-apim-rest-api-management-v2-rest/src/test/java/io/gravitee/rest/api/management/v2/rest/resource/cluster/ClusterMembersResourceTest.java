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
package io.gravitee.rest.api.management.v2.rest.resource.cluster;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.use_case.members.AddClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.DeleteClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterMembersUseCase;
import io.gravitee.apim.core.cluster.use_case.members.TransferClusterOwnershipUseCase;
import io.gravitee.apim.core.cluster.use_case.members.UpdateClusterMemberUseCase;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.ClusterTransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateMember;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClusterMembersResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String CLUSTER_ID = "my-cluster";
    private static final String MEMBER_ID = "my-member";

    @Inject
    private GetClusterMembersUseCase getClusterMembersUseCase;

    @Inject
    private AddClusterMemberUseCase addClusterMemberUseCase;

    @Inject
    private UpdateClusterMemberUseCase updateClusterMemberUseCase;

    @Inject
    private DeleteClusterMemberUseCase deleteClusterMemberUseCase;

    @Inject
    private TransferClusterOwnershipUseCase transferClusterOwnershipUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/clusters/" + CLUSTER_ID + "/members";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(getClusterMembersUseCase, addClusterMemberUseCase, updateClusterMemberUseCase, deleteClusterMemberUseCase);
    }

    @Nested
    class GetClusterMembersTest {

        @Test
        void should_get_cluster_members() {
            var output = List.of(
                MemberEntity.builder().id("m1").build(),
                MemberEntity.builder().id("m2").build(),
                MemberEntity.builder().id("m3").build(),
                MemberEntity.builder().id("m4").build(),
                MemberEntity.builder().id("m5").build(),
                MemberEntity.builder().id("m6").build(),
                MemberEntity.builder().id("m7").build(),
                MemberEntity.builder().id("m8").build(),
                MemberEntity.builder().id("m9").build(),
                MemberEntity.builder().id("m10").build(),
                MemberEntity.builder().id("m11").build(),
                MemberEntity.builder().id("m12").build(),
                MemberEntity.builder().id("m13").build(),
                MemberEntity.builder().id("m14").build(),
                MemberEntity.builder().id("m15").build()
            );
            when(getClusterMembersUseCase.execute(any())).thenReturn(new GetClusterMembersUseCase.Output(output));

            final Response response = rootTarget().request().get();

            MembersResponse membersResponse = response.readEntity(MembersResponse.class);

            assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(OK_200),
                () -> assertThat(membersResponse.getData().size()).isEqualTo(10),
                () -> assertThat(membersResponse.getPagination().getPage()).isEqualTo(1),
                () -> assertThat(membersResponse.getPagination().getPerPage()).isEqualTo(10),
                () -> assertThat(membersResponse.getPagination().getPageCount()).isEqualTo(2),
                () -> assertThat(membersResponse.getPagination().getTotalCount()).isEqualTo(15),
                () -> assertThat(membersResponse.getPagination().getPageItemsCount()).isEqualTo(10),
                () -> assertThat(membersResponse.getLinks()).isNotNull()
            );
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.CLUSTER_MEMBER, CLUSTER_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }

    @Nested
    class AddClusterMemberTest {

        @Test
        void should_add_cluster_member() {
            AddMember addMember = new AddMember();
            addMember.setRoleName("OWNER");

            when(addClusterMemberUseCase.execute(any())).thenReturn(new AddClusterMemberUseCase.Output());

            final Response response = rootTarget().request().post(json(addMember));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
        }

        @Test
        void should_return_400_if_role_name_is_missing() {
            AddMember addMember = new AddMember();
            final Response response = rootTarget().request().post(json(addMember));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_missing_body() {
            final Response response = rootTarget().request().post(json(null));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.CLUSTER_MEMBER, CLUSTER_ID, CREATE, () -> rootTarget().request().post(json(new AddMember())));
        }
    }

    @Nested
    class UpdateClusterMemberTest {

        private WebTarget target;

        @BeforeEach
        public void setup() {
            target = rootTarget(MEMBER_ID);
        }

        @Test
        void should_update_cluster_member() {
            UpdateMember updateMember = new UpdateMember();
            updateMember.setRoleName("OWNER");
            Member updatedMember = Member.builder().roles(List.of(Member.Role.builder().name("OWNER").build())).build();

            when(updateClusterMemberUseCase.execute(any())).thenReturn(new UpdateClusterMemberUseCase.Output(updatedMember));

            Response response = target.request().put(json(updateMember));

            Member responseMember = response.readEntity(Member.class);

            assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(OK_200),
                () -> assertThat(responseMember.getRoles().get(0).getName()).isEqualTo("OWNER")
            );
        }

        @Test
        void should_return_400_if_role_name_is_missing() {
            UpdateMember updateMember = new UpdateMember();
            Response response = target.request().put(json(updateMember));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_missing_body() {
            final Response response = target.request().put(json(""));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.CLUSTER_MEMBER, CLUSTER_ID, UPDATE, () -> target.request().put(json(new UpdateMember())));
        }
    }

    @Nested
    class DeleteClusterMemberTest {

        private WebTarget target;

        @BeforeEach
        public void setup() {
            target = rootTarget(MEMBER_ID);
        }

        @Test
        void should_delete_cluster_member() {
            when(deleteClusterMemberUseCase.execute(any())).thenReturn(new DeleteClusterMemberUseCase.Output());
            Response response = target.request().delete();
            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.CLUSTER_MEMBER, CLUSTER_ID, DELETE, () -> target.request().delete());
        }
    }

    @Nested
    class TransferOwnershipTest {

        private WebTarget target;

        @BeforeEach
        public void setup() {
            target = rootTarget("_transfer-ownership");
        }

        @Test
        void should_transfer_ownership() {
            when(transferClusterOwnershipUseCase.execute(any())).thenReturn(new TransferClusterOwnershipUseCase.Output());
            Response response = target.request().post(json(new ClusterTransferOwnership()));
            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(
                RolePermission.CLUSTER_MEMBER,
                CLUSTER_ID,
                UPDATE,
                () -> target.request().post(json(new ClusterTransferOwnership()))
            );
        }
    }
}
