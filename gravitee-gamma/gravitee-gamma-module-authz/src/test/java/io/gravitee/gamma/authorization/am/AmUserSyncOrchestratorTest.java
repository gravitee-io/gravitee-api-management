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
package io.gravitee.gamma.authorization.am;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.am.sdk.management.api.UserApi;
import io.gravitee.am.sdk.management.model.User;
import io.gravitee.am.sdk.management.model.UserPage;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmUserSyncOrchestratorTest {

    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", "env-1", "user-1");
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);

    private AmSdkUserClientFactory clientFactory;
    private UserApi userApi;
    private AuthzEntityAdminApi authzEntityAdminApi;
    private AmUserSyncOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        clientFactory = mock(AmSdkUserClientFactory.class);
        userApi = mock(UserApi.class);
        authzEntityAdminApi = mock(AuthzEntityAdminApi.class);
        when(clientFactory.userApi(CONNECTION)).thenReturn(userApi);
        orchestrator = new AmUserSyncOrchestrator(clientFactory, authzEntityAdminApi);
    }

    private void stubPage(int page, UserPage userPage) {
        when(userApi.listUsers(eq("DEFAULT"), eq("DEFAULT"), eq("domain-1"), isNull(), isNull(), eq(page), eq(1000)))
            .thenReturn(Future.succeededFuture(userPage));
    }

    private static User user(String id, String username, String email, String displayName, Boolean enabled) {
        return new User().id(id).username(username).email(email).displayName(displayName).enabled(enabled);
    }

    private static UserPage page(long totalCount, User... users) {
        return new UserPage().data(List.of(users)).totalCount(totalCount).currentPage(0);
    }

    @SuppressWarnings("unchecked")
    private List<CreateOrReplaceAuthzEntityCommand> captureSingleBulkUpsert() {
        ArgumentCaptor<List<CreateOrReplaceAuthzEntityCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(authzEntityAdminApi).bulkUpsert(eq(CALLER), captor.capture());
        return captor.getValue();
    }

    @Test
    void maps_each_am_user_to_a_principal_entity() {
        stubPage(0, page(2, user("sub-1", "alice", "alice@corp.io", "Alice", true), user("sub-2", "bob", "bob@corp.io", "Bob", false)));

        AmUserSyncOrchestrator.Result result = orchestrator.run(CALLER, CONNECTION);

        assertThat(result).isEqualTo(new AmUserSyncOrchestrator.Result(2, 2));
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands).hasSize(2);
        CreateOrReplaceAuthzEntityCommand first = commands.get(0);
        assertThat(first.environmentId()).isEqualTo("env-1");
        assertThat(first.entityId()).isEqualTo("sub-1");
        assertThat(first.kind()).isEqualTo(AuthzEntityKind.PRINCIPAL);
        assertThat(first.source()).isEqualTo("gravitee_am");
        assertThat(first.parents()).isEmpty();
        assertThat(first.attributes())
            .containsEntry("username", "alice")
            .containsEntry("email", "alice@corp.io")
            .containsEntry("displayName", "Alice")
            .containsEntry("enabled", true);
    }

    @Test
    void omits_attributes_that_am_did_not_populate() {
        stubPage(0, page(1, user("sub-1", "alice", null, null, null)));

        orchestrator.run(CALLER, CONNECTION);

        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands.get(0).attributes()).containsOnlyKeys("username");
    }

    @Test
    void pages_through_all_users_until_total_count_is_reached() {
        stubPage(0, new UserPage().data(List.of(user("sub-1", "a", null, null, null), user("sub-2", "b", null, null, null))).totalCount(3L));
        stubPage(1, new UserPage().data(List.of(user("sub-3", "c", null, null, null))).totalCount(3L));

        AmUserSyncOrchestrator.Result result = orchestrator.run(CALLER, CONNECTION);

        assertThat(result.usersFetched()).isEqualTo(3);
        verify(userApi).listUsers(eq("DEFAULT"), eq("DEFAULT"), eq("domain-1"), isNull(), isNull(), eq(0), eq(1000));
        verify(userApi).listUsers(eq("DEFAULT"), eq("DEFAULT"), eq("domain-1"), isNull(), isNull(), eq(1), eq(1000));
    }

    @Test
    void stops_and_upserts_nothing_when_the_domain_has_no_users() {
        stubPage(0, page(0));

        AmUserSyncOrchestrator.Result result = orchestrator.run(CALLER, CONNECTION);

        assertThat(result).isEqualTo(new AmUserSyncOrchestrator.Result(0, 0));
        verify(authzEntityAdminApi, never()).bulkUpsert(any(), any());
    }

    @Test
    void flushes_in_batches_of_500() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            users.add(user("sub-" + i, "user-" + i, null, null, null));
        }
        when(userApi.listUsers(eq("DEFAULT"), eq("DEFAULT"), eq("domain-1"), isNull(), isNull(), eq(0), eq(1000)))
            .thenReturn(Future.succeededFuture(new UserPage().data(users).totalCount(600L)));

        AmUserSyncOrchestrator.Result result = orchestrator.run(CALLER, CONNECTION);

        assertThat(result).isEqualTo(new AmUserSyncOrchestrator.Result(600, 600));
        ArgumentCaptor<List<CreateOrReplaceAuthzEntityCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(authzEntityAdminApi, org.mockito.Mockito.times(2)).bulkUpsert(eq(CALLER), captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(500);
        assertThat(captor.getAllValues().get(1)).hasSize(100);
    }

    @Test
    void surfaces_an_upstream_failure_as_an_am_sync_exception() {
        when(userApi.listUsers(eq("DEFAULT"), eq("DEFAULT"), eq("domain-1"), isNull(), isNull(), eq(0), eq(1000)))
            .thenReturn(Future.failedFuture(new RuntimeException("boom")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.run(CALLER, CONNECTION)).isInstanceOf(AmSyncException.class);
    }
}
