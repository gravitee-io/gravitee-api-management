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
package io.gravitee.gamma.authorization.core.am.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.core.am.exception.AmSyncException;
import io.gravitee.gamma.authorization.core.am.model.AmAgent;
import io.gravitee.gamma.authorization.core.am.model.AmAgentPage;
import io.gravitee.gamma.authorization.core.am.model.AmGroup;
import io.gravitee.gamma.authorization.core.am.model.AmGroupPage;
import io.gravitee.gamma.authorization.core.am.model.AmRole;
import io.gravitee.gamma.authorization.core.am.model.AmRolePage;
import io.gravitee.gamma.authorization.core.am.model.AmUser;
import io.gravitee.gamma.authorization.core.am.model.AmUserPage;
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.definition.authz.AgentEntityId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SyncAmUsersUseCaseTest {

    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", "env-1", "user-1");
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);

    private static final int MAX_ENTITIES = 1000;
    private static final int PAGE_SIZE = 50;
    private static final int BATCH_SIZE = 50;


    private AmDirectoryClient amDirectoryClient;
    private AmDirectoryClient.Session session;
    private AuthzEntityAdminApi authzEntityAdminApi;

    @BeforeEach
    void setUp() {
        amDirectoryClient = mock(AmDirectoryClient.class);
        session = mock(AmDirectoryClient.Session.class);
        when(amDirectoryClient.openSession(CONNECTION)).thenReturn(session);
        authzEntityAdminApi = mock(AuthzEntityAdminApi.class);
        when(session.fetchGroups(anyInt(), anyInt())).thenReturn(new AmGroupPage(List.of(), 0));
        when(session.fetchRoles(anyInt(), anyInt())).thenReturn(new AmRolePage(List.of(), 0));
        when(session.fetchAgents(anyInt(), anyInt())).thenReturn(new AmAgentPage(List.of(), 0));
    }

    private SyncAmUsersUseCase.Output run() {
        return run(new SyncAmUsersUseCase.SyncConfig(MAX_ENTITIES, PAGE_SIZE, BATCH_SIZE));
    }

    private SyncAmUsersUseCase.Output run(SyncAmUsersUseCase.SyncConfig syncConfig) {
        SyncAmUsersUseCase useCase = new SyncAmUsersUseCase(amDirectoryClient, authzEntityAdminApi, syncConfig);
        return useCase.execute(new SyncAmUsersUseCase.Input(CALLER, CONNECTION));
    }

    private void stubPage(int page, AmUserPage userPage) {
        when(session.fetchUsers(eq(page), eq(50))).thenReturn(userPage);
    }

    private void stubGroupPage(int page, AmGroupPage groupPage) {
        when(session.fetchGroups(eq(page), eq(50))).thenReturn(groupPage);
    }

    private void stubRolePage(int page, AmRolePage rolePage) {
        when(session.fetchRoles(eq(page), eq(50))).thenReturn(rolePage);
    }

    private void stubAgentPage(int page, AmAgentPage agentPage) {
        when(session.fetchAgents(eq(page), eq(50))).thenReturn(agentPage);
    }

    private static AmUser user(String id, String username, String email, String displayName, Boolean enabled) {
        return new AmUser(id, null, null, email, username, displayName, enabled, List.of(), List.of());
    }

    private static AmUser userWithMemberships(String id, List<String> groups, List<String> roles) {
        return new AmUser(id, null, null, null, "user-" + id, null, null, groups, roles);
    }

    private static AmUserPage page(long totalCount, AmUser... users) {
        return new AmUserPage(List.of(users), totalCount);
    }

    @SuppressWarnings("unchecked")
    private List<CreateOrReplaceAuthzEntityCommand> captureSingleBulkUpsert() {
        ArgumentCaptor<List<CreateOrReplaceAuthzEntityCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(authzEntityAdminApi).bulkUpsert(eq(CALLER), captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<CreateOrReplaceAuthzEntityCommand> captureAllUpserts() {
        ArgumentCaptor<List<CreateOrReplaceAuthzEntityCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(authzEntityAdminApi, org.mockito.Mockito.atLeastOnce()).bulkUpsert(eq(CALLER), captor.capture());
        return captor.getAllValues().stream().flatMap(List::stream).toList();
    }

    @Test
    void maps_each_am_user_to_a_principal_entity() {
        stubPage(0, page(2, user("sub-1", "alice", "alice@corp.io", "Alice", true), user("sub-2", "bob", "bob@corp.io", "Bob", false)));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(2, 0, 2));
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands).hasSize(2);
        CreateOrReplaceAuthzEntityCommand first = commands.get(0);
        assertThat(first.environmentId()).isEqualTo("env-1");
        assertThat(first.entityId()).isEqualTo("sub-1");
        assertThat(first.kind()).isEqualTo(AuthzEntityKind.PRINCIPAL);
        assertThat(first.source()).isEqualTo("gravitee_am");
        assertThat(first.parents()).isEmpty();
        assertThat(first.attributes())
            .containsEntry("_kind", "user")
            .containsEntry("sub", "sub-1")
            .containsEntry("username", "alice")
            .containsEntry("email", "alice@corp.io")
            .containsEntry("displayName", "Alice")
            .containsEntry("enabled", true);
    }

    @Test
    void keys_the_entity_on_the_token_sub_when_source_is_present() {
        // AM V2 issues sub = MD5-UUID("source:externalId"); the entity must be keyed on that, not the user id.
        AmUser user = new AmUser("internal-id", "github", "ext-42", null, "alice", null, null, List.of(), List.of());
        stubPage(0, page(1, user));

        run();

        String expectedSub = java.util.UUID.nameUUIDFromBytes("github:ext-42".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands.get(0).entityId()).isEqualTo(expectedSub);
    }

    @Test
    void falls_back_to_the_user_id_when_external_id_is_absent() {
        // Source present but no externalId (e.g. a misconfigured provider): AM can't build
        // "source:externalId", so the sub falls back to the user id. The entity must follow.
        AmUser user = new AmUser("internal-id", "github", null, null, "alice", null, null, List.of(), List.of());
        stubPage(0, page(1, user));

        run();

        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands.get(0).entityId()).isEqualTo("internal-id");
    }

    @Test
    void omits_attributes_that_am_did_not_populate() {
        stubPage(0, page(1, user("sub-1", "alice", null, null, null)));

        run();

        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        // _kind + sub are always set; only the AM-populated profile attributes (here: username) carry through.
        assertThat(commands.get(0).attributes()).containsOnlyKeys("_kind", "sub", "username");
    }

    @Test
    void pages_through_all_users_until_total_count_is_reached() {
        stubPage(0, new AmUserPage(List.of(user("sub-1", "a", null, null, null), user("sub-2", "b", null, null, null)), 3L));
        stubPage(1, new AmUserPage(List.of(user("sub-3", "c", null, null, null)), 3L));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result.usersFetched()).isEqualTo(3);
        verify(session).fetchUsers(eq(0), eq(50));
        verify(session).fetchUsers(eq(1), eq(50));
        // The per-run client is opened once and closed when the run completes.
        verify(amDirectoryClient).openSession(CONNECTION);
        verify(session).close();
    }

    @Test
    void stops_and_upserts_nothing_when_the_domain_has_no_users() {
        stubPage(0, page(0));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(0, 0, 0));
        verify(authzEntityAdminApi, never()).bulkUpsert(any(), any());
    }

    @Test
    void flushes_in_batches_of_50() {
        List<AmUser> users = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            users.add(user("sub-" + i, "user-" + i, null, null, null));
        }
        stubPage(0, new AmUserPage(users, 120L));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(120, 0, 120));
        ArgumentCaptor<List<CreateOrReplaceAuthzEntityCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(authzEntityAdminApi, times(3)).bulkUpsert(eq(CALLER), captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(50);
        assertThat(captor.getAllValues().get(1)).hasSize(50);
        assertThat(captor.getAllValues().get(2)).hasSize(20);
    }

    @Test
    void stops_syncing_once_the_configured_max_is_reached() {
        // AM reports far more users than the cap; the sync must stop at the ceiling and not page further.
        List<AmUser> firstPage = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            firstPage.add(user("sub-" + i, "user-" + i, null, null, null));
        }
        stubPage(0, new AmUserPage(firstPage, 500L));

        SyncAmUsersUseCase.Output result = run(new SyncAmUsersUseCase.SyncConfig(20, PAGE_SIZE, BATCH_SIZE));

        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(20, 0, 20));
        verify(session).fetchUsers(eq(0), eq(50));
        verify(session, never()).fetchUsers(eq(1), eq(50));
    }

    @Test
    void surfaces_an_upstream_failure_as_an_am_sync_exception() {
        when(session.fetchUsers(eq(0), eq(50)))
            .thenThrow(new AmSyncException("Access Management request failed: boom", new RuntimeException("boom")));

        assertThatThrownBy(this::run).isInstanceOf(AmSyncException.class);
    }

    @Test
    void closes_the_session_even_when_the_fetch_fails() {
        when(session.fetchUsers(eq(0), eq(50)))
            .thenThrow(new AmSyncException("Access Management request failed: boom", new RuntimeException("boom")));

        assertThatThrownBy(this::run).isInstanceOf(AmSyncException.class);
        verify(session).close();
    }

    @Test
    void syncs_groups_and_roles_as_principal_entities_and_links_them_to_users() {
        stubGroupPage(0, new AmGroupPage(List.of(new AmGroup("g-1", "Engineering")), 1L));
        stubRolePage(0, new AmRolePage(List.of(new AmRole("r-1", "ADMIN")), 1L));
        stubPage(0, page(1, userWithMemberships("sub-1", List.of("g-1"), List.of("r-1"))));

        run();

        List<CreateOrReplaceAuthzEntityCommand> all = captureAllUpserts();

        CreateOrReplaceAuthzEntityCommand group = all.stream().filter(c -> c.entityId().equals("g-1")).findFirst().orElseThrow();
        assertThat(group.kind()).isEqualTo(AuthzEntityKind.PRINCIPAL);
        assertThat(group.source()).isEqualTo("gravitee_am");
        assertThat(group.parents()).isEmpty();
        assertThat(group.attributes()).containsEntry("_kind", "group").containsEntry("name", "Engineering");

        CreateOrReplaceAuthzEntityCommand role = all.stream().filter(c -> c.entityId().equals("r-1")).findFirst().orElseThrow();
        assertThat(role.attributes()).containsEntry("_kind", "role").containsEntry("name", "ADMIN");

        CreateOrReplaceAuthzEntityCommand user = all.stream().filter(c -> c.entityId().equals("sub-1")).findFirst().orElseThrow();
        assertThat(user.attributes()).containsEntry("_kind", "user");
        assertThat(user.parents()).containsExactlyInAnyOrder("g-1", "r-1");
    }

    @Test
    void falls_back_to_the_id_when_a_group_or_role_has_no_name() {
        stubGroupPage(0, new AmGroupPage(List.of(new AmGroup("g-1", null)), 1L));
        stubRolePage(0, new AmRolePage(List.of(new AmRole("r-1", null)), 1L));
        stubPage(0, page(0)); // no users — exercise only the group/role labelling path

        run();

        List<CreateOrReplaceAuthzEntityCommand> all = captureAllUpserts();
        assertThat(all.stream().filter(c -> c.entityId().equals("g-1")).findFirst().orElseThrow().attributes())
            .containsEntry("name", "g-1");
        assertThat(all.stream().filter(c -> c.entityId().equals("r-1")).findFirst().orElseThrow().attributes())
            .containsEntry("name", "r-1");
    }

    @Test
    void leaves_user_parents_empty_when_the_user_has_no_memberships() {
        stubPage(0, page(1, user("sub-1", "alice", null, null, null)));

        run();

        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands.get(0).parents()).isEmpty();
    }

    private static AmAgent agent(String id, String clientId, String name, String agentType) {
        return new AmAgent(id, clientId, name, agentType);
    }

    @Test
    void maps_each_am_agent_to_a_generic_principal_entity() {
        stubPage(0, page(0)); // no users — exercise only the agent path
        stubAgentPage(0, new AmAgentPage(List.of(agent("app-1", "agent-client", "Booking bot", "AUTONOMOUS")), 1L));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(0, 1, 1));
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands).hasSize(1);
        CreateOrReplaceAuthzEntityCommand cmd = commands.get(0);
        assertThat(cmd.environmentId()).isEqualTo("env-1");
        assertThat(cmd.kind()).isEqualTo(AuthzEntityKind.PRINCIPAL);
        assertThat(cmd.source()).isEqualTo("gravitee_am");
        // Agent identities are generic — no parents.
        assertThat(cmd.parents()).isEmpty();
        assertThat(cmd.attributes())
            .containsEntry("_kind", "agent-identity")
            .containsEntry("clientId", "agent-client")
            .containsEntry("domain", "domain-1")
            .containsEntry("name", "Booking bot")
            .containsEntry("agentType", "AUTONOMOUS");
    }

    @Test
    void keys_the_agent_entity_on_the_id_the_pep_derives_from_domain_and_client_id() {
        stubPage(0, page(0));
        stubAgentPage(0, new AmAgentPage(List.of(agent("app-1", "agent-client", "Bot", "AUTONOMOUS")), 1L));

        run();

        // CONNECTION.defaultDomainId() == "domain-1"; the entity id must equal the shared derivation.
        String expectedId = AgentEntityId.derive("domain-1", "agent-client");
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands.get(0).entityId()).isEqualTo(expectedId);
    }

    @Test
    void omits_agent_name_and_type_when_am_did_not_populate_them() {
        stubPage(0, page(0));
        stubAgentPage(0, new AmAgentPage(List.of(agent("app-1", "agent-client", null, null)), 1L));

        run();

        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        // _kind, clientId, domain are always set; name/agentType only carry through when AM populated them.
        assertThat(commands.get(0).attributes()).containsOnlyKeys("_kind", "clientId", "domain");
    }

    @Test
    void skips_agents_with_a_null_or_blank_client_id() {
        stubPage(0, page(0));
        stubAgentPage(
            0,
            new AmAgentPage(
                List.of(agent("app-1", null, "no client", "AUTONOMOUS"), agent("app-2", "  ", "blank client", "AUTONOMOUS"), agent("app-3", "ok", "good", "AUTONOMOUS")),
                3L
            )
        );

        SyncAmUsersUseCase.Output result = run();

        // Only the agent with a usable client_id is fetched and upserted; the two others are skipped.
        assertThat(result).isEqualTo(new SyncAmUsersUseCase.Output(0, 1, 1));
        List<CreateOrReplaceAuthzEntityCommand> commands = captureSingleBulkUpsert();
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).attributes()).containsEntry("clientId", "ok");
    }

    @Test
    void pages_through_all_agents_until_total_count_is_reached() {
        stubPage(0, page(0));
        stubAgentPage(0, new AmAgentPage(List.of(agent("app-1", "c-1", null, null), agent("app-2", "c-2", null, null)), 3L));
        stubAgentPage(1, new AmAgentPage(List.of(agent("app-3", "c-3", null, null)), 3L));

        SyncAmUsersUseCase.Output result = run();

        assertThat(result.agentsFetched()).isEqualTo(3);
        verify(session).fetchAgents(eq(0), eq(50));
        verify(session).fetchAgents(eq(1), eq(50));
    }

    @Test
    void stops_syncing_agents_once_the_configured_max_is_reached() {
        stubPage(0, page(0));
        List<AmAgent> firstPage = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            firstPage.add(agent("app-" + i, "c-" + i, null, null));
        }
        stubAgentPage(0, new AmAgentPage(firstPage, 500L));

        SyncAmUsersUseCase.Output result = run(new SyncAmUsersUseCase.SyncConfig(20, PAGE_SIZE, BATCH_SIZE));

        assertThat(result.agentsFetched()).isEqualTo(20);
        verify(session).fetchAgents(eq(0), eq(50));
        verify(session, never()).fetchAgents(eq(1), eq(50));
    }
}
